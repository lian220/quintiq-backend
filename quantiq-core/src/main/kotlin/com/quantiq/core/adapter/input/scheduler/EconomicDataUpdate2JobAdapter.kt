package com.quantiq.core.adapter.input.scheduler

import com.quantiq.core.domain.economic.port.input.EconomicDataUseCase
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 경제 데이터 재수집 + Vertex AI 예측 Job (Input Adapter)
 * 매일 23:00에 실행됩니다.
 *
 * 역할:
 * - 경제 데이터 재수집 (경제지표 업데이트)
 * - Vertex AI 예측 모델 실행 (병렬 처리)
 */
@Component
class EconomicDataUpdate2JobAdapter(
    private val economicDataUseCase: EconomicDataUseCase
) : Job {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(context: JobExecutionContext?) {
        try {
            val triggerName = context?.trigger?.key?.name ?: "unknown"
            logger.info("=".repeat(80))
            logger.info("경제 데이터 재수집 + Vertex AI 예측 시작 (23:00) [Trigger: $triggerName]")
            logger.info("=".repeat(80))

            // UseCase 호출 (경제 데이터 업데이트)
            economicDataUseCase.triggerEconomicDataUpdate()
                .thenAccept { result ->
                    logger.info("✅ 경제 데이터 재수집 완료: $result")
                }
                .exceptionally { e ->
                    logger.error("❌ 경제 데이터 재수집 실패", e)
                    null
                }
                .get()

            // TODO: Vertex AI 예측 로직 추가
            // vertexAIUseCase.runPrediction()
            logger.info("⚠️ Vertex AI 예측은 아직 미구현 상태입니다")

            logger.info("=".repeat(80))
        } catch (e: Exception) {
            logger.error("❌ 경제 데이터 재수집 + Vertex AI 예측 Job 실행 중 오류", e)
            throw JobExecutionException(e)
        }
    }
}
