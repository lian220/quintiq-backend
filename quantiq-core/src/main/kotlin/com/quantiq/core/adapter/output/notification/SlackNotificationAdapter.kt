package com.quantiq.core.adapter.output.notification

import com.quantiq.core.domain.economic.port.output.NotificationSender
import com.quantiq.core.adapter.output.notification.slack.SlackApiClient
import org.springframework.stereotype.Component

/**
 * Slack Notification Adapter (Output Adapter)
 * NotificationSender 인터페이스를 구현하여 Slack과 연동합니다.
 */
@Component
class SlackNotificationAdapter(
    private val slackApiClient: SlackApiClient
) : NotificationSender {

    override fun notifyEconomicDataUpdateRequest(requestId: String): String? {
        return slackApiClient.notifyEconomicDataUpdateRequest(requestId)
    }

    override fun notifyEconomicDataCollectionError(requestId: String, error: String) {
        slackApiClient.notifyEconomicDataCollectionError(requestId, error)
    }

    override fun notifyTechnicalAnalysisRequest(requestId: String): String? {
        return slackApiClient.notifyTechnicalAnalysisRequest(requestId)
    }

    override fun notifySentimentAnalysisRequest(requestId: String): String? {
        return slackApiClient.notifySentimentAnalysisRequest(requestId)
    }

    override fun notifyCombinedAnalysisRequest(requestId: String): String? {
        return slackApiClient.notifyCombinedAnalysisRequest(requestId)
    }

    override fun notifyAnalysisError(requestId: String, analysisType: String, error: String) {
        slackApiClient.notifyAnalysisError(requestId, analysisType, error)
    }
}
