package com.quantiq.core.adapter.input.rest.ml

import com.quantiq.core.adapter.input.api.MlPackageApi
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * ML 패키지 관리 Controller
 *
 * Python Data Engine의 ML 패키지 업로드 기능을 트리거합니다.
 */
@RestController
@RequestMapping("/api/v1/ml-packages")
class MlPackageController(
    private val webClient: WebClient,
    @Value("\${quantiq.data-engine.url:http://quantiq-data-engine:8000}")
    private val dataEngineUrl: String
) : MlPackageApi {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun uploadPackage(): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("=".repeat(60))
            logger.info("📦 ML 패키지 업로드 요청")
            logger.info("Data Engine URL: $dataEngineUrl")
            logger.info("=".repeat(60))

            // Python Data Engine API 호출
            val response = webClient.post()
                .uri("$dataEngineUrl/ml/upload-package")
                .retrieve()
                .bodyToMono(Map::class.java)
                .timeout(Duration.ofMinutes(5))
                .onErrorResume { e ->
                    logger.error("❌ Data Engine API 호출 실패", e)
                    Mono.just(
                        mapOf(
                            "success" to false,
                            "message" to "Data Engine API 호출 실패: ${e.message}"
                        )
                    )
                }
                .block()

            @Suppress("UNCHECKED_CAST")
            val result = response as? Map<String, Any> ?: mapOf(
                "success" to false,
                "message" to "잘못된 응답 형식"
            )

            val success = result["success"] as? Boolean ?: false

            if (success) {
                logger.info("✅ 패키지 업로드 완료")
                logger.info("GCS URI: ${result["gcs_uri"]}")
                logger.info("버전: v${result["version"]}")
                ResponseEntity.ok(result)
            } else {
                logger.error("❌ 패키지 업로드 실패: ${result["message"]}")
                ResponseEntity.status(500).body(result)
            }

        } catch (e: Exception) {
            logger.error("❌ 패키지 업로드 요청 실패", e)
            ResponseEntity.status(500).body(
                mapOf(
                    "success" to false,
                    "message" to "패키지 업로드 요청 실패: ${e.message}"
                )
            )
        }
    }

    override fun getPackageStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val response = webClient.get()
                .uri("$dataEngineUrl/ml/package-status")
                .retrieve()
                .bodyToMono(Map::class.java)
                .timeout(Duration.ofSeconds(30))
                .block()

            @Suppress("UNCHECKED_CAST")
            val result = response as? Map<String, Any> ?: mapOf(
                "message" to "데이터 없음"
            )

            ResponseEntity.ok(result)

        } catch (e: Exception) {
            logger.error("❌ 패키지 상태 조회 실패", e)
            ResponseEntity.status(500).body(
                mapOf(
                    "success" to false,
                    "message" to "패키지 상태 조회 실패: ${e.message}"
                )
            )
        }
    }
}
