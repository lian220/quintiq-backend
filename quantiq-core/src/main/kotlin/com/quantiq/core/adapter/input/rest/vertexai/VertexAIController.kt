package com.quantiq.core.adapter.input.rest.vertexai

import com.google.cloud.aiplatform.v1.JobState
import com.quantiq.core.adapter.input.api.VertexAIApi
import com.quantiq.core.service.VertexAIService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Vertex AI 제어 Controller
 */
@RestController
@RequestMapping("/api/v1/vertex-ai")
class VertexAIController(
    private val vertexAIService: VertexAIService
) : VertexAIApi {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun runPrediction(): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("🚀 Vertex AI 예측 수동 실행 요청")

            val jobId = vertexAIService.createAndRunCustomJob()

            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Vertex AI 예측 실행 완료",
                "jobId" to jobId,
                "estimatedTime" to "3-5분"
            ))
        } catch (e: Exception) {
            logger.error("❌ Vertex AI 예측 실행 실패", e)
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "message" to "Vertex AI 예측 실행 실패: ${e.message}"
            ))
        }
    }

    override fun getJobStatus(jobId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val state = vertexAIService.getJobState(jobId)

            ResponseEntity.ok(mapOf(
                "success" to true,
                "jobId" to jobId,
                "state" to state.name,
                "stateDescription" to getStateDescription(state)
            ))
        } catch (e: Exception) {
            logger.error("❌ Job 상태 조회 실패", e)
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "message" to "Job 상태 조회 실패: ${e.message}"
            ))
        }
    }

    override fun cancelJob(jobId: String): ResponseEntity<Map<String, Any>> {
        return try {
            vertexAIService.cancelJob(jobId)

            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Job 취소 요청 완료",
                "jobId" to jobId
            ))
        } catch (e: Exception) {
            logger.error("❌ Job 취소 실패", e)
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "message" to "Job 취소 실패: ${e.message}"
            ))
        }
    }

    /**
     * Job 상태 설명
     */
    private fun getStateDescription(state: JobState): String {
        return when (state) {
            JobState.JOB_STATE_QUEUED -> "대기 중"
            JobState.JOB_STATE_PENDING -> "준비 중"
            JobState.JOB_STATE_RUNNING -> "실행 중"
            JobState.JOB_STATE_SUCCEEDED -> "완료"
            JobState.JOB_STATE_FAILED -> "실패"
            JobState.JOB_STATE_CANCELLING -> "취소 중"
            JobState.JOB_STATE_CANCELLED -> "취소됨"
            JobState.JOB_STATE_PAUSED -> "일시정지"
            JobState.JOB_STATE_EXPIRED -> "만료됨"
            else -> "알 수 없음"
        }
    }
}
