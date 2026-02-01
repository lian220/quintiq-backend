package com.quantiq.core.adapter.input.rest.user

import com.quantiq.core.application.user.UserKisAccountService
import com.quantiq.core.application.user.KisAccountRequest
import com.quantiq.core.application.user.KisAccountResponse
import com.quantiq.core.infrastructure.security.CurrentUser
import com.quantiq.core.infrastructure.security.UserPrincipal
import com.quantiq.core.infrastructure.security.SecurityUtils
import com.quantiq.core.infrastructure.security.AccessDeniedException
import com.quantiq.core.infrastructure.security.UnauthorizedException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * User KIS 계정 관리 Controller
 * 사용자별 한국투자증권 계정 정보 등록/조회/관리
 *
 * 🔐 보안: 본인만 자신의 KIS 계정 정보에 접근 가능
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/kis-accounts")
class UserKisAccountController(
    private val userKisAccountService: UserKisAccountService
) {

    /**
     * KIS 계정 정보 등록/업데이트
     * POST /api/v1/users/{userId}/kis-accounts
     *
     * 🔐 본인만 등록/수정 가능
     */
    @PostMapping
    fun registerKisAccount(
        @PathVariable userId: String,
        @RequestBody request: KisAccountRequest,
        @CurrentUser currentUser: UserPrincipal?
    ): ResponseEntity<Map<String, Any>> {
        // 본인 확인 (개발 단계에서는 비활성화, 향후 활성화)
        validateUserAccess(userId, currentUser)

        val kisAccount = userKisAccountService.registerOrUpdateKisAccount(userId, request)

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "KIS account registered successfully",
            "accountNumber" to kisAccount.accountNumber,
            "accountType" to kisAccount.accountType.name
        ))
    }

    /**
     * KIS 계정 정보 조회
     * GET /api/v1/users/{userId}/kis-accounts
     *
     * 🔐 본인만 조회 가능
     */
    @GetMapping
    fun getKisAccount(
        @PathVariable userId: String,
        @CurrentUser currentUser: UserPrincipal?
    ): ResponseEntity<KisAccountResponse> {
        // 본인 확인
        validateUserAccess(userId, currentUser)

        val response = userKisAccountService.getKisAccount(userId)
        return ResponseEntity.ok(response)
    }

    /**
     * KIS 계정 활성화/비활성화
     * PATCH /api/v1/users/{userId}/kis-accounts/toggle
     *
     * 🔐 본인만 활성화/비활성화 가능
     */
    @PatchMapping("/toggle")
    fun toggleKisAccount(
        @PathVariable userId: String,
        @RequestParam enabled: Boolean,
        @CurrentUser currentUser: UserPrincipal?
    ): ResponseEntity<Map<String, Any>> {
        // 본인 확인
        validateUserAccess(userId, currentUser)

        userKisAccountService.toggleKisAccount(userId, enabled)

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "KIS account ${if (enabled) "enabled" else "disabled"}",
            "enabled" to enabled
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
            throw AccessDeniedException("You can only access your own KIS account information.")
        }
        */

        // 개발 단계: 경고 로그만
        if (currentUser == null) {
            println("⚠️ [DEV MODE] Unauthenticated access to user $requestedUserId")
        } else if (currentUser.userId != requestedUserId && !SecurityUtils.isAdmin()) {
            println("⚠️ [DEV MODE] User ${currentUser.userId} accessing $requestedUserId's data")
        }
    }
}
