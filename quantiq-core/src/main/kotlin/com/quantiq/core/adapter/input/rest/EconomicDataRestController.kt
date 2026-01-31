package com.quantiq.core.adapter.input.rest

import com.quantiq.core.domain.economic.port.input.EconomicDataUseCase
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 경제 데이터 업데이트 REST Controller (Input Adapter)
 * HTTP 요청을 UseCase로 전달하는 Adapter 역할을 합니다.
 */
@RestController
@RequestMapping("/api/economic")
class EconomicDataRestController(
    private val economicDataUseCase: EconomicDataUseCase
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/trigger-update")
    fun triggerEconomicDataUpdate(): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("경제 데이터 업데이트 수동 트리거 요청 받음")

            economicDataUseCase.triggerEconomicDataUpdate()
                .thenApply { result ->
                    ResponseEntity.ok(
                        mapOf<String, Any>(
                            "success" to true,
                            "message" to result,
                            "timestamp" to Instant.now().toString()
                        )
                    )
                }
                .get()
        } catch (e: Exception) {
            logger.error("경제 데이터 업데이트 트리거 실패", e)
            ResponseEntity.status(500).body(
                mapOf<String, Any>(
                    "success" to false,
                    "message" to "경제 데이터 업데이트 요청 실패: ${e.message}",
                    "timestamp" to Instant.now().toString()
                )
            )
        }
    }

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            ResponseEntity.ok(
                mapOf(
                    "status" to "running",
                    "service" to "economic-data-scheduler",
                    "timestamp" to Instant.now().toString(),
                    "schedules" to listOf(
                        mapOf(
                            "name" to "economicDataUpdate1",
                            "time" to "06:05 (KST)",
                            "description" to "경제 데이터 업데이트"
                        ),
                        mapOf(
                            "name" to "economicDataUpdate2",
                            "time" to "23:00 (KST)",
                            "description" to "경제 데이터 재수집 및 Vertex AI 예측 병렬 실행"
                        )
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("상태 조회 중 오류", e)
            ResponseEntity.status(500).body(
                mapOf<String, Any>(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }
}
