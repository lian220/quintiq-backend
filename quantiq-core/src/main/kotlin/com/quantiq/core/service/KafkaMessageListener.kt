package com.quantiq.core.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaMessageListener(
        private val objectMapper: ObjectMapper,
        private val autoTradingService: AutoTradingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["quantiq.analysis.completed"], groupId = "quantiq-core-group")
    fun listenAnalysisCompleted(message: String) {
        logger.info("Received Analysis Completed Event: $message")
        try {
            val event = objectMapper.readTree(message)
            // Optional: validate event type or date

            logger.info("Triggering Auto Trading Logic...")
            autoTradingService.executeAutoTrading()
        } catch (e: Exception) {
            logger.error("Failed to process message: $message", e)
        }
    }
}
