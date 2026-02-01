package com.quantiq.core.adapter.input.rest.user

import com.quantiq.core.application.balance.BalanceService
import com.quantiq.core.domain.model.BalanceWithProfitResponse
import com.quantiq.core.infrastructure.security.CurrentUser
import com.quantiq.core.infrastructure.security.UserPrincipal
import com.quantiq.core.infrastructure.security.SecurityUtils
import com.quantiq.core.infrastructure.security.AccessDeniedException
import com.quantiq.core.infrastructure.security.UnauthorizedException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * User 잔고 및 수익률 조회 Controller
 * 사용자별 보유 종목, 수익률, 계좌 요약 정보 제공
 *
 * 🔐 보안: 본인만 자신의 수익률 정보에 접근 가능
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/balances")
class UserBalanceController(
    private val balanceService: BalanceService
) {

    /**
     * User 기준 잔고 및 수익률 조회
     * GET /api/v1/users/{userId}/balances/profit
     *
     * 🔐 본인만 조회 가능
     *
     * @param userId 사용자 ID
     * @param currentUser 현재 로그인한 사용자
     * @return 보유 종목, 수익률, 계좌 요약 정보
     */
    @GetMapping("/profit")
    fun getBalanceWithProfit(
        @PathVariable userId: String,
        @CurrentUser currentUser: UserPrincipal?
    ): ResponseEntity<BalanceWithProfitResponse> {
        // 본인 확인
        validateUserAccess(userId, currentUser)

        val response = balanceService.getBalanceWithProfit(userId)
        return ResponseEntity.ok(response)
    }

    /**
     * 간단한 수익률만 조회
     * GET /api/v1/users/{userId}/balances/profit-summary
     *
     * 🔐 본인만 조회 가능
     */
    @GetMapping("/profit-summary")
    fun getProfitSummary(
        @PathVariable userId: String,
        @CurrentUser currentUser: UserPrincipal?
    ): ResponseEntity<Map<String, Any>> {
        // 본인 확인
        validateUserAccess(userId, currentUser)

        val balance = balanceService.getBalanceWithProfit(userId)

        return ResponseEntity.ok(mapOf(
            "userId" to balance.userId,
            "totalProfitRate" to balance.summary.totalProfitRate,
            "totalProfit" to balance.summary.totalProfit,
            "realizedProfit" to balance.summary.realizedProfit,
            "unrealizedProfit" to balance.summary.unrealizedProfit,
            "totalAssets" to balance.totalAssets,
            "currency" to balance.summary.currency,
            "timestamp" to balance.timestamp
        ))
    }

    /**
     * 본인 확인 검증
     *
     * 현재: 개발 단계이므로 경고만 로깅
     * 향후: 인증 활성화 시 예외 발생
     *
     * @param requestedUserId URL의 userId
     * @param currentUser 현재 로그인한 사용자
     * @throws UnauthorizedException 인증되지 않은 경우
     * @throws AccessDeniedException 본인이 아닌 경우
     */
    private fun validateUserAccess(requestedUserId: String, currentUser: UserPrincipal?) {
        // TODO: 인증 활성화 시 아래 주석 해제
        /*
        // 1. 인증 확인
        if (currentUser == null) {
            throw UnauthorizedException("Authentication required. Please login first.")
        }

        // 2. 본인 확인 (관리자는 모든 사용자 접근 가능)
        if (!SecurityUtils.isAdmin() && currentUser.userId != requestedUserId) {
            throw AccessDeniedException("You can only access your own balance and profit information.")
        }
        */

        // 개발 단계: 경고 로그만
        if (currentUser == null) {
            println("⚠️ [DEV MODE] Unauthenticated access to user $requestedUserId balance")
        } else if (currentUser.userId != requestedUserId && !SecurityUtils.isAdmin()) {
            println("⚠️ [DEV MODE] User ${currentUser.userId} accessing $requestedUserId's balance")
        }
    }
}
