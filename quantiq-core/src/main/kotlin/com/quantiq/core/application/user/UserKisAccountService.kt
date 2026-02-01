package com.quantiq.core.application.user

import com.quantiq.core.adapter.output.persistence.jpa.*
import com.quantiq.core.infrastructure.security.EncryptionService
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserKisAccountService(
    private val userKisAccountJpaRepository: UserKisAccountJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * KIS 계정 정보 등록/업데이트
     * @param userId 사용자 ID
     * @param request KIS 계정 정보
     */
    @Transactional
    fun registerOrUpdateKisAccount(userId: String, request: KisAccountRequest): UserKisAccountEntity {
        logger.info("🔐 Registering/Updating KIS account for user: $userId")

        // 1. 사용자 조회
        val user = userJpaRepository.findByUserIdWithDetails(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        // 2. AppSecret 암호화
        val encryptedSecret = encryptionService.encrypt(request.appSecret)

        // 3. 기존 계정 확인
        val existingAccount = userKisAccountJpaRepository.findByUserId(user.id!!)

        return if (existingAccount.isPresent) {
            // 업데이트
            val account = existingAccount.get()
            val updated = account.copy(
                appKey = request.appKey,
                appSecretEncrypted = encryptedSecret,
                accountNumber = request.accountNumber,
                accountProductCode = request.accountProductCode,
                accountType = request.accountType,
                enabled = request.enabled,
                updatedAt = LocalDateTime.now()
            )
            userKisAccountJpaRepository.save(updated)
            logger.info("✅ KIS account updated for user: $userId")
            updated
        } else {
            // 신규 등록
            val newAccount = UserKisAccountEntity(
                user = user,
                appKey = request.appKey,
                appSecretEncrypted = encryptedSecret,
                accountNumber = request.accountNumber,
                accountProductCode = request.accountProductCode,
                accountType = request.accountType,
                enabled = request.enabled
            )
            userKisAccountJpaRepository.save(newAccount)
            logger.info("✅ KIS account registered for user: $userId")
            newAccount
        }
    }

    /**
     * KIS 계정 정보 조회
     * @param userId 사용자 ID
     * @return KIS 계정 정보 (복호화된 Secret 제외)
     */
    @Transactional(readOnly = true)
    fun getKisAccount(userId: String): KisAccountResponse {
        val kisAccount = userKisAccountJpaRepository.findActiveByUserUserId(userId)
            .orElseThrow { IllegalArgumentException("KIS account not found or not active: $userId") }

        return KisAccountResponse(
            appKey = kisAccount.appKey,
            accountNumber = kisAccount.accountNumber,
            accountProductCode = kisAccount.accountProductCode,
            accountType = kisAccount.accountType,
            enabled = kisAccount.enabled,
            lastUsedAt = kisAccount.lastUsedAt,
            createdAt = kisAccount.createdAt
        )
    }

    /**
     * KIS 계정 활성화/비활성화
     */
    @Transactional
    fun toggleKisAccount(userId: String, enabled: Boolean) {
        val user = userJpaRepository.findByUserIdWithDetails(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val kisAccount = userKisAccountJpaRepository.findByUserId(user.id!!)
            .orElseThrow { IllegalArgumentException("KIS account not found: $userId") }

        val updated = kisAccount.copy(enabled = enabled, updatedAt = LocalDateTime.now())
        userKisAccountJpaRepository.save(updated)

        logger.info("✅ KIS account ${if (enabled) "enabled" else "disabled"} for user: $userId")
    }

    /**
     * 복호화된 AppSecret 조회 (내부 사용 전용)
     * @param userId 사용자 ID
     * @return 복호화된 AppSecret
     */
    @Transactional(readOnly = true)
    fun getDecryptedAppSecret(userId: String): String {
        val kisAccount = userKisAccountJpaRepository.findActiveByUserUserId(userId)
            .orElseThrow { IllegalArgumentException("KIS account not found: $userId") }

        return encryptionService.decrypt(kisAccount.appSecretEncrypted)
    }
}

/**
 * KIS 계정 등록 요청
 */
data class KisAccountRequest(
    val appKey: String,
    val appSecret: String,  // 평문 (암호화되어 저장됨)
    val accountNumber: String,
    val accountProductCode: String = "01",
    val accountType: KisAccountType = KisAccountType.MOCK,
    val enabled: Boolean = true
)

/**
 * KIS 계정 조회 응답
 */
data class KisAccountResponse(
    val appKey: String,
    val accountNumber: String,
    val accountProductCode: String,
    val accountType: KisAccountType,
    val enabled: Boolean,
    val lastUsedAt: LocalDateTime?,
    val createdAt: LocalDateTime
)
