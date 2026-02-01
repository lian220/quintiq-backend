package com.quantiq.core.adapter.output.external

import com.quantiq.core.adapter.output.persistence.jpa.KisAccountType
import com.quantiq.core.adapter.output.persistence.jpa.UserKisAccountEntity
import com.quantiq.core.adapter.output.persistence.jpa.UserKisAccountJpaRepository
import com.quantiq.core.adapter.output.persistence.mongodb.KisTokenRepository
import com.quantiq.core.infrastructure.security.EncryptionService
import com.quantiq.core.config.KisConfig
import com.quantiq.core.domain.KisToken
import com.quantiq.core.domain.trading.port.output.TradingApiPort
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * KIS API Adapter (Output Adapter)
 * TradingApiPort를 구현하여 한국투자증권 API와 연동합니다.
 *
 * ⚠️ 사용자별 KIS 계정 정보를 DB에서 조회하여 사용합니다
 */
@Component
class KisApiAdapter(
    private val userKisAccountRepository: UserKisAccountJpaRepository,
    private val encryptionService: EncryptionService,
    private val tokenRepository: KisTokenRepository
) : TradingApiPort {
    private val logger = LoggerFactory.getLogger(KisApiAdapter::class.java)

    // 사용자별 WebClient 캐시
    private val webClientCache = ConcurrentHashMap<String, WebClient>()

    private val lastApiCallTime = AtomicLong(0)
    private val minApiIntervalMs = 500L
    private val apiLock = ReentrantLock()

    // 사용자별 액세스 토큰 캐시
    private val accessTokenCache = ConcurrentHashMap<String, Pair<String, LocalDateTime>>()

    /**
     * 사용자별 활성화된 KIS 계정 조회
     */
    private fun getActiveKisAccount(userId: String): UserKisAccountEntity {
        return userKisAccountRepository.findActiveByUserUserId(userId)
            .orElseThrow { IllegalStateException("KIS account not found for user: $userId") }
    }

    /**
     * 사용자 ID로 WebClient 생성 또는 캐시에서 반환
     */
    private fun getWebClientForUser(userId: String): WebClient {
        return webClientCache.computeIfAbsent(userId) {
            val kisAccount = getActiveKisAccount(userId)
            val baseUrl = KisConfig.getBaseUrlForAccountType(kisAccount.accountType)
            logger.info("Creating WebClient for user $userId with ${kisAccount.accountType} account: $baseUrl")

            WebClient.builder().baseUrl(baseUrl).build()
        }
    }

    override fun getAccessToken(userId: String): String {
        val now = LocalDateTime.now()

        // 1. Memory cache
        val cached = accessTokenCache[userId]
        if (cached != null && now.isBefore(cached.second)) {
            return cached.first
        }

        // 2. DB cache
        val kisAccount = getActiveKisAccount(userId)
        val accountType = kisAccount.accountType.name.lowercase()
        val tokenDoc = tokenRepository.findTopByUserIdAndAccountTypeOrderByCreatedAtDesc(userId, accountType)

        if (tokenDoc != null && now.isBefore(tokenDoc.expirationTime)) {
            accessTokenCache[userId] = Pair(tokenDoc.accessToken, tokenDoc.expirationTime)
            return tokenDoc.accessToken
        }

        // 3. New token from KIS
        return refreshToken(userId, kisAccount, accountType)
    }

    private fun refreshToken(userId: String, kisAccount: UserKisAccountEntity, accountType: String): String {
        logger.info("Refreshing KIS access token for user: $userId, type: $accountType")

        val appSecret = encryptionService.decrypt(kisAccount.appSecretEncrypted)
        val webClient = getWebClientForUser(userId)

        val response = webClient
            .post()
            .uri("/oauth2/tokenP")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "grant_type" to "client_credentials",
                    "appkey" to kisAccount.appKey,
                    "appsecret" to appSecret
                )
            )
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: throw RuntimeException("Failed to get access token from KIS for user: $userId")

        val token = response["access_token"] as String
        val expiresIn = (response["expires_in"] as Int).toLong()
        val expirationTime = LocalDateTime.now().plusSeconds(expiresIn)

        val kisToken = KisToken(
            userId = userId,
            accountType = accountType,
            accessToken = token,
            expirationTime = expirationTime
        )
        tokenRepository.save(kisToken)

        accessTokenCache[userId] = Pair(token, expirationTime)

        return token
    }

    private fun waitForRateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastApiCallTime.get()
        if (elapsed < minApiIntervalMs) {
            Thread.sleep(minApiIntervalMs - elapsed)
        }
        lastApiCallTime.set(System.currentTimeMillis())
    }

    override fun getOverseasBalance(exchange: String): Map<String, Any> {
        // TODO: userId를 파라미터로 받도록 인터페이스 수정 필요
        // 임시로 하드코딩된 사용자 사용
        val userId = "admin"
        return getOverseasBalance(userId, exchange)
    }

    @Suppress("UNCHECKED_CAST")
    fun getOverseasBalance(userId: String, exchange: String): Map<String, Any> {
        waitForRateLimit()

        val kisAccount = getActiveKisAccount(userId)
        val token = getAccessToken(userId)
        val webClient = getWebClientForUser(userId)

        // TR_ID: 모의투자(VTTS3012R), 실전투자(TTTS3012R)
        val trId = when (kisAccount.accountType) {
            KisAccountType.MOCK -> "VTTS3012R"
            KisAccountType.REAL -> "TTTS3012R"
        }

        val appSecret = encryptionService.decrypt(kisAccount.appSecretEncrypted)

        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/uapi/overseas-stock/v1/trading/inquire-balance")
                    .queryParam("CANO", kisAccount.accountNumber)
                    .queryParam("ACNT_PRDT_CD", kisAccount.accountProductCode)
                    .queryParam("OVRS_EXCG_CD", exchange)
                    .queryParam("TR_CRCY_CD", "USD")
                    .queryParam("CTX_AREA_FK200", "")
                    .queryParam("CTX_AREA_NK200", "")
                    .build()
            }
            .header("authorization", "Bearer $token")
            .header("appkey", kisAccount.appKey)
            .header("appsecret", appSecret)
            .header("tr_id", trId)
            .header("Content-Type", "application/json; charset=utf-8")
            .retrieve()
            .bodyToMono(Map::class.java)
            .block() as Map<String, Any>? ?: emptyMap()
    }
}
