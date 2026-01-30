package com.quantiq.core.controller

import com.quantiq.core.service.StockRecommendationService
import java.time.LocalDateTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommendations")
class StockRecommendationController(private val recommendationService: StockRecommendationService) {

    @GetMapping("/ai")
    fun getAiRecommendations(): ResponseEntity<Map<String, Any>> {
        val results = recommendationService.getAiRecommendations()
        return ResponseEntity.ok(
                mapOf(
                        "success" to true,
                        "data" to results,
                        "metadata" to
                                mapOf("timestamp" to LocalDateTime.now(), "count" to results.size)
                )
        )
    }

    @GetMapping("/with-sentiment")
    fun getRecommendationsWithSentiment(): ResponseEntity<Map<String, Any>> {
        val results = recommendationService.getRecommendationsWithSentiment()
        return ResponseEntity.ok(
                mapOf(
                        "success" to true,
                        "data" to results,
                        "metadata" to
                                mapOf("timestamp" to LocalDateTime.now(), "count" to results.size)
                )
        )
    }

    @GetMapping("/combined")
    fun getCombinedRecommendations(): ResponseEntity<Map<String, Any>> {
        val results = recommendationService.getCombinedRecommendations()
        return ResponseEntity.ok(
                mapOf(
                        "success" to true,
                        "data" to results,
                        "metadata" to
                                mapOf("timestamp" to LocalDateTime.now(), "count" to results.size)
                )
        )
    }
}
