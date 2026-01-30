package com.quantiq.core.controller

import com.quantiq.core.repository.EconomicDataRepository
import com.quantiq.core.service.AnalysisRequestProducer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/economic")
class EconomicController(
        private val economicDataRepository: EconomicDataRepository,
        private val producer: AnalysisRequestProducer
) {

        @GetMapping("/last-updated")
        fun getLastUpdated(): ResponseEntity<Map<String, Any?>> {
                val lastData = economicDataRepository.findFirstByOrderByDateDesc()
                return if (lastData != null) {
                        ResponseEntity.ok(
                                mapOf(
                                        "success" to true,
                                        "last_updated_date" to lastData.date,
                                        "message" to "Last updated: ${lastData.date}"
                                )
                        )
                } else {
                        ResponseEntity.ok(
                                mapOf<String, Any?>(
                                        "success" to true,
                                        "last_updated_date" to null,
                                        "message" to "No data found"
                                )
                        )
                }
        }

        @PostMapping("/update")
        fun updateEconomicData(): ResponseEntity<Map<String, Any>> {
                producer.sendRequest("ECONOMIC")
                return ResponseEntity.ok(
                        mapOf("success" to true, "message" to "Economic data update triggered")
                )
        }
}
