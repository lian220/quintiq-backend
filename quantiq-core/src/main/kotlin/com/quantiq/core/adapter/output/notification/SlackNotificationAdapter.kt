package com.quantiq.core.adapter.output.notification

import com.quantiq.core.domain.economic.port.output.NotificationSender
import com.quantiq.core.service.SlackNotificationService
import org.springframework.stereotype.Component

/**
 * Slack Notification Adapter (Output Adapter)
 * NotificationSender 인터페이스를 구현하여 Slack과 연동합니다.
 */
@Component
class SlackNotificationAdapter(
    private val slackNotificationService: SlackNotificationService
) : NotificationSender {

    override fun notifyEconomicDataUpdateRequest(requestId: String): String? {
        return slackNotificationService.notifyEconomicDataUpdateRequest(requestId)
    }

    override fun notifyEconomicDataCollectionError(requestId: String, error: String) {
        slackNotificationService.notifyEconomicDataCollectionError(requestId, error)
    }
}
