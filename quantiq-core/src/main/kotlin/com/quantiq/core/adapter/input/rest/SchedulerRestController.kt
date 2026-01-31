package com.quantiq.core.adapter.input.rest

import com.quantiq.core.domain.model.ScheduleInfo
import com.quantiq.core.domain.model.SchedulerStatus
import com.quantiq.core.domain.scheduler.port.input.SchedulerUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 스케줄러 관리 REST Controller (Input Adapter)
 * HTTP 요청을 UseCase로 전달하는 Adapter 역할을 합니다.
 */
@RestController
@RequestMapping("/api/scheduler")
class SchedulerRestController(
    private val schedulerUseCase: SchedulerUseCase
) {

    @GetMapping("/status")
    fun getSchedulerStatus(): ResponseEntity<SchedulerStatus> {
        return ResponseEntity.ok(schedulerUseCase.getSchedulerStatus())
    }

    @GetMapping("/schedules")
    fun getAllSchedules(): ResponseEntity<Map<String, ScheduleInfo>> {
        return ResponseEntity.ok(schedulerUseCase.getAllSchedules())
    }

    @PostMapping("/schedules/{triggerName}/pause")
    fun pauseSchedule(
        @PathVariable triggerName: String
    ): ResponseEntity<Map<String, Any>> {
        val success = schedulerUseCase.pauseSchedule(triggerName)
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "스케줄이 일시 중지되었습니다." else "스케줄 일시 중지 실패"
            )
        )
    }

    @PostMapping("/schedules/{triggerName}/resume")
    fun resumeSchedule(
        @PathVariable triggerName: String
    ): ResponseEntity<Map<String, Any>> {
        val success = schedulerUseCase.resumeSchedule(triggerName)
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "스케줄이 재개되었습니다." else "스케줄 재개 실패"
            )
        )
    }

    @PostMapping("/start")
    fun startScheduler(): ResponseEntity<Map<String, Any>> {
        val success = schedulerUseCase.startScheduler()
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "스케줄러가 시작되었습니다." else "스케줄러 시작 실패"
            )
        )
    }

    @PostMapping("/stop")
    fun stopScheduler(): ResponseEntity<Map<String, Any>> {
        val success = schedulerUseCase.stopScheduler()
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to if (success) "스케줄러가 중지되었습니다." else "스케줄러 중지 실패"
            )
        )
    }
}
