package com.quantiq.core.controller

import com.quantiq.core.service.AnalysisRequestProducer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stocks/analysis")
class AnalysisController(private val producer: AnalysisRequestProducer) {

    @PostMapping("/sentiment")
    fun triggerSentimentAnalysis(): ResponseEntity<Map<String, String>> {
        producer.sendRequest("SENTIMENT")
        return ResponseEntity.ok(mapOf("message" to "Sentiment analysis triggered successfully"))
    }

    @PostMapping("/technical")
    fun triggerTechnicalAnalysis(): ResponseEntity<Map<String, String>> {
        producer.sendRequest("TECHNICAL")
        return ResponseEntity.ok(mapOf("message" to "Technical analysis triggered successfully"))
    }
}
