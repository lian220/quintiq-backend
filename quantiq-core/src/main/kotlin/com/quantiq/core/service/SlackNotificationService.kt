package com.quantiq.core.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.ZonedDateTime
import java.time.ZoneId

/**
 * Slack 알림 서비스 (Thread 지원 - Slack API 기반)
 * 경제 데이터 스케줄러 이벤트를 Slack으로 통지합니다.
 */
@Service
class SlackNotificationService(
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val kst = ZoneId.of("Asia/Seoul")

    @Value("\${slack.bot-token:}")
    private lateinit var slackBotToken: String

    @Value("\${slack.channel:#trading-alerts}")
    private lateinit var slackChannel: String

    @Value("\${slack.webhook-url:}")
    private lateinit var slackWebhookUrl: String

    private fun getCurrentTimeKST(): String = ZonedDateTime.now(kst).toString()

    /**
     * 경제 데이터 업데이트 요청 알림 (스레드 루트 메시지)
     *
     * @return Slack 스레드 타임스탬프 (답글용)
     */
    fun notifyEconomicDataUpdateRequest(requestId: String): String? {
        if (slackBotToken.isBlank()) {
            logger.warn("⚠️ Slack Bot Token 없음 - Webhook으로 fallback")
            notifyViaWebhook(requestId)
            return null
        }

        try {
            val message = SlackApiMessage(
                channel = slackChannel,
                text = "📊 경제 데이터 업데이트 요청",
                attachments = listOf(
                    SlackAttachment(
                        color = "36a64f",
                        title = "경제 데이터 수집 요청",
                        text = "경제 데이터 업데이트 요청이 발행되었습니다.",
                        fields = listOf(
                            SlackField("Request ID", requestId, true),
                            SlackField("Timestamp", getCurrentTimeKST(), true),
                            SlackField("Source", "Quartz Scheduler", true),
                            SlackField("Status", "🔄 Processing", true)
                        )
                    )
                )
            )

            val response = sendToSlackApi(message)
            val threadTs = response?.ts

            if (threadTs != null) {
                logger.info("✅ Slack 스레드 루트 생성: requestId=$requestId, threadTs=$threadTs")
            } else {
                logger.warn("⚠️ Slack 메시지 발송 성공하지만 threadTs 없음")
            }

            return threadTs
        } catch (e: Exception) {
            logger.error("❌ Slack API 알림 발송 실패", e)
            return null
        }
    }

    /**
     * Webhook으로 알림 전송 (Thread 미지원 - Fallback)
     */
    private fun notifyViaWebhook(requestId: String) {
        if (slackWebhookUrl.isBlank()) {
            logger.debug("Slack webhook URL not configured, skipping notification")
            return
        }

        try {
            val message = SlackMessage(
                text = "📊 경제 데이터 업데이트 요청",
                attachments = listOf(
                    SlackAttachment(
                        color = "36a64f",
                        title = "경제 데이터 수집 요청",
                        text = "경제 데이터 업데이트 요청이 발행되었습니다.",
                        fields = listOf(
                            SlackField("Request ID", requestId, true),
                            SlackField("Timestamp", getCurrentTimeKST(), true),
                            SlackField("Source", "Quartz Scheduler", true),
                            SlackField("Status", "🔄 Processing", true)
                        )
                    )
                )
            )

            sendToSlackWebhook(message)
            logger.info("✅ Slack 알림 발송 완료 (Webhook): $requestId")
        } catch (e: Exception) {
            logger.error("❌ Slack Webhook 알림 발송 실패", e)
        }
    }

    /**
     * 경제 데이터 수집 오류 알림
     */
    fun notifyEconomicDataCollectionError(requestId: String, error: String) {
        if (slackWebhookUrl.isBlank()) return

        try {
            val message = SlackMessage(
                text = "⚠️ 경제 데이터 수집 오류",
                attachments = listOf(
                    SlackAttachment(
                        color = "dc3545",
                        title = "경제 데이터 수집 실패",
                        text = "경제 데이터 수집 중 오류가 발생했습니다.",
                        fields = listOf(
                            SlackField("Request ID", requestId, true),
                            SlackField("Error", error, false),
                            SlackField("Timestamp", getCurrentTimeKST(), true),
                            SlackField("Action", "수동으로 재시도해주세요", true)
                        )
                    )
                )
            )

            sendToSlackWebhook(message)
            logger.info("⚠️ 오류 알림 발송 완료")
        } catch (e: Exception) {
            logger.error("❌ 오류 알림 발송 실패", e)
        }
    }

    /**
     * 스케줄러 상태 알림
     */
    fun notifySchedulerStatus(status: String, details: Map<String, Any>) {
        if (slackWebhookUrl.isBlank()) return

        try {
            val color = when (status) {
                "started" -> "0099cc"
                "stopped" -> "999999"
                else -> "666666"
            }

            val message = SlackMessage(
                text = "🔄 스케줄러 상태 업데이트",
                attachments = listOf(
                    SlackAttachment(
                        color = color,
                        title = "Quartz 스케줄러 - $status",
                        text = "스케줄러 상태가 변경되었습니다.",
                        fields = listOf(
                            SlackField("Status", status.uppercase(), true),
                            SlackField("Jobs", details["scheduledJobCount"]?.toString() ?: "0", true),
                            SlackField("Triggers", details["activeTriggerCount"]?.toString() ?: "0", true),
                            SlackField("Timestamp", getCurrentTimeKST(), true)
                        )
                    )
                )
            )

            sendToSlackWebhook(message)
        } catch (e: Exception) {
            logger.error("❌ 상태 알림 발송 실패", e)
        }
    }

    /**
     * Slack API (chat.postMessage)로 메시지 전송
     */
    private fun sendToSlackApi(message: SlackApiMessage): SlackApiResponse? {
        return try {
            webClient.post()
                .uri("https://slack.com/api/chat.postMessage")
                .header("Authorization", "Bearer $slackBotToken")
                .header("Content-Type", "application/json")
                .bodyValue(message)
                .retrieve()
                .bodyToMono(SlackApiResponse::class.java)
                .block()
        } catch (e: Exception) {
            logger.error("❌ Slack API 호출 실패", e)
            null
        }
    }

    /**
     * Slack Webhook으로 메시지 전송
     */
    private fun sendToSlackWebhook(message: SlackMessage) {
        webClient.post()
            .uri(slackWebhookUrl)
            .bodyValue(message)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }
}

/**
 * Slack API 메시지 데이터 클래스 (chat.postMessage)
 */
data class SlackApiMessage(
    val channel: String,
    val text: String,
    val attachments: List<SlackAttachment>,
    @JsonProperty("thread_ts")
    val threadTs: String? = null
)

/**
 * Slack API 응답 데이터 클래스
 */
data class SlackApiResponse(
    val ok: Boolean,
    val ts: String?,
    val error: String?
)

/**
 * Slack Webhook 메시지 데이터 클래스
 */
data class SlackMessage(
    val text: String,
    val attachments: List<SlackAttachment>
)

/**
 * Slack 첨부 파일 데이터 클래스
 */
data class SlackAttachment(
    val color: String,
    val title: String,
    val text: String,
    val fields: List<SlackField>,
    val footer: String = "Quantiq Economic Data Scheduler",
    val ts: Long = System.currentTimeMillis() / 1000
)

/**
 * Slack 필드 데이터 클래스
 */
data class SlackField(
    val title: String,
    val value: String,
    val short: Boolean
)
