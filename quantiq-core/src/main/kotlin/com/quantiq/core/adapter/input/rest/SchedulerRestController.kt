package com.quantiq.core.adapter.input.rest

import com.quantiq.core.domain.model.ScheduleInfo
import com.quantiq.core.domain.model.SchedulerStatus
import com.quantiq.core.domain.scheduler.port.input.SchedulerUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 스케줄러 관리 REST Controller (Input Adapter)
 * HTTP 요청을 UseCase로 전달하는 Adapter 역할을 합니다.
 */
@Tag(name = "Scheduler", description = "스케줄러 관리 API - Quartz Scheduler 제어 및 모니터링")
@RestController
@RequestMapping("/api/scheduler")
class SchedulerRestController(
    private val schedulerUseCase: SchedulerUseCase
) {

    @Operation(
        summary = "스케줄러 상태 조회",
        description = """
            Quartz Scheduler의 전체 상태를 조회합니다.

            **조회 정보:**
            - 스케줄러 실행 상태 (running/stopped/paused)
            - 전체 Job 개수
            - 실행 중인 Job 개수
            - 스케줄러 인스턴스 이름
            - 시작 시간 및 실행 시간
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상태 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = SchedulerStatus::class),
                    examples = [ExampleObject(
                        value = """{"isRunning":true,"totalJobs":8,"runningJobs":0,"schedulerName":"quartzScheduler","startTime":"2026-02-01T00:00:00Z"}"""
                    )]
                )]
            ),
            ApiResponse(responseCode = "500", description = "상태 조회 실패")
        ]
    )
    @GetMapping("/status")
    fun getSchedulerStatus(): ResponseEntity<SchedulerStatus> {
        return ResponseEntity.ok(schedulerUseCase.getSchedulerStatus())
    }

    @Operation(
        summary = "전체 스케줄 목록 조회",
        description = """
            등록된 모든 스케줄의 상태와 설정 정보를 조회합니다.

            **등록된 스케줄:**
            1. **economicDataUpdateTrigger** (06:05 KST): 경제 데이터 업데이트
            2. **economicDataUpdate2Trigger** (23:00 KST): 경제 데이터 재수집 + Vertex AI 예측
            3. **parallelAnalysisTrigger** (23:05 KST): 병렬 분석 (기술적 + 감정)
            4. **combinedAnalysisTrigger** (23:45 KST): 통합 분석
            5. **autoBuyTrigger** (23:50 KST): 자동 매수
            6. **cleanupOrdersTrigger** (06:30 KST): 주문 정리
            7. **portfolioProfitReportTrigger** (07:00 KST): 포트폴리오 수익 보고
            8. **autoSellTrigger** (매 1분): 자동 매도 체크 (미국 시장 시간만)

            **조회 정보 (각 스케줄):**
            - Trigger 이름 및 설명
            - Cron 표현식 또는 반복 주기
            - 다음 실행 시간
            - 이전 실행 시간
            - 현재 상태 (NORMAL, PAUSED, BLOCKED)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스케줄 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{
  "economicDataUpdateTrigger": {
    "triggerName": "economicDataUpdateTrigger",
    "description": "경제 데이터 업데이트",
    "cronExpression": "0 5 6 * * ?",
    "nextFireTime": "2026-02-02T06:05:00Z",
    "previousFireTime": "2026-02-01T06:05:00Z",
    "state": "NORMAL"
  },
  "autoSellTrigger": {
    "triggerName": "autoSellTrigger",
    "description": "자동 매도 체크",
    "repeatInterval": "60000",
    "nextFireTime": "2026-02-01T10:31:00Z",
    "previousFireTime": "2026-02-01T10:30:00Z",
    "state": "NORMAL"
  }
}"""
                    )]
                )]
            ),
            ApiResponse(responseCode = "500", description = "스케줄 목록 조회 실패")
        ]
    )
    @GetMapping("/schedules")
    fun getAllSchedules(): ResponseEntity<Map<String, ScheduleInfo>> {
        return ResponseEntity.ok(schedulerUseCase.getAllSchedules())
    }

    @Operation(
        summary = "특정 스케줄 일시 중지",
        description = """
            지정된 Trigger의 스케줄을 일시 중지합니다.

            **동작:**
            - 스케줄이 PAUSED 상태로 변경됩니다
            - 다음 실행 시간이 되어도 Job이 실행되지 않습니다
            - 스케줄러 자체는 계속 실행 중입니다
            - resume 요청으로 재개할 수 있습니다

            **사용 예시:**
            - 특정 스케줄을 임시로 비활성화
            - 유지보수 중 특정 Job 중지
            - 테스트 환경에서 일부 스케줄만 실행
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스케줄 일시 중지 성공",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":true,"message":"스케줄이 일시 중지되었습니다."}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "200",
                description = "스케줄 일시 중지 실패 (존재하지 않는 Trigger)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":false,"message":"스케줄 일시 중지 실패"}"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/schedules/{triggerName}/pause")
    fun pauseSchedule(
        @Parameter(
            description = "일시 중지할 Trigger 이름 (예: economicDataUpdateTrigger, autoSellTrigger)",
            required = true,
            example = "economicDataUpdateTrigger"
        )
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

    @Operation(
        summary = "특정 스케줄 재개",
        description = """
            일시 중지된 Trigger의 스케줄을 재개합니다.

            **동작:**
            - PAUSED 상태의 스케줄이 NORMAL 상태로 변경됩니다
            - 다음 실행 시간부터 Job이 정상적으로 실행됩니다
            - 일시 중지 기간 동안 놓친 실행은 보충되지 않습니다

            **사용 예시:**
            - pause로 중지한 스케줄 재활성화
            - 유지보수 완료 후 스케줄 재개
            - 테스트 완료 후 정상 운영 복귀
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스케줄 재개 성공",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":true,"message":"스케줄이 재개되었습니다."}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "200",
                description = "스케줄 재개 실패 (존재하지 않는 Trigger)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":false,"message":"스케줄 재개 실패"}"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/schedules/{triggerName}/resume")
    fun resumeSchedule(
        @Parameter(
            description = "재개할 Trigger 이름 (예: economicDataUpdateTrigger, autoSellTrigger)",
            required = true,
            example = "economicDataUpdateTrigger"
        )
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

    @Operation(
        summary = "스케줄러 시작",
        description = """
            전체 Quartz Scheduler를 시작합니다.

            **동작:**
            - 중지된 스케줄러를 시작합니다
            - 등록된 모든 스케줄(NORMAL 상태)이 활성화됩니다
            - 각 Trigger의 다음 실행 시간부터 Job이 실행됩니다

            **주의사항:**
            - 스케줄러가 이미 실행 중이면 실패합니다
            - 애플리케이션 시작 시 자동으로 실행됩니다
            - stop으로 중지한 경우에만 사용하세요

            **사용 예시:**
            - 수동으로 중지한 스케줄러 재시작
            - 전체 스케줄러 점검 후 재가동
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스케줄러 시작 성공",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":true,"message":"스케줄러가 시작되었습니다."}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "200",
                description = "스케줄러 시작 실패 (이미 실행 중)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":false,"message":"스케줄러 시작 실패"}"""
                    )]
                )]
            )
        ]
    )
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

    @Operation(
        summary = "스케줄러 중지",
        description = """
            전체 Quartz Scheduler를 중지합니다.

            **동작:**
            - 실행 중인 스케줄러를 중지합니다
            - 모든 스케줄의 실행이 중단됩니다
            - 현재 실행 중인 Job은 완료될 때까지 대기합니다

            **주의사항:**
            - ⚠️ **전체 시스템 중지**: 모든 자동화 작업이 중단됩니다
            - 경제 데이터 수집, 분석, 자동 매매가 모두 중지됩니다
            - start 요청으로 재시작할 수 있습니다

            **사용 예시:**
            - 긴급 유지보수 시 전체 스케줄 중지
            - 시스템 점검 전 안전한 종료
            - 특정 스케줄만 중지할 경우 pause 사용 권장
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스케줄러 중지 성공",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":true,"message":"스케줄러가 중지되었습니다."}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "200",
                description = "스케줄러 중지 실패 (이미 중지됨)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"success":false,"message":"스케줄러 중지 실패"}"""
                    )]
                )]
            )
        ]
    )
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
