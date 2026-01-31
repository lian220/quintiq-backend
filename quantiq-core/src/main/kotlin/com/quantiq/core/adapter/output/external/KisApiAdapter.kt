package com.quantiq.core.adapter.output.external

import com.quantiq.core.config.KisConfig
import com.quantiq.core.domain.KisToken
import com.quantiq.core.domain.trading.port.output.TradingApiPort
import com.quantiq.core.repository.KisTokenRepository
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * KIS API Adapter (Output Adapter)
 * TradingApiPort를 구현하여 한국투자증권 API와 연동합니다.
 */
@Component
class KisApiAdapter(
    private val kisConfig: KisConfig,
    private val tokenRepository: KisTokenRepository
) : TradingApiPort {
    private val logger = LoggerFactory.getLogger(KisApiAdapter::class.java)
    private val webClient = WebClient.builder().baseUrl(kisConfig.baseUrl).build()

    private val lastApiCallTime = AtomicLong(0)
    private val minApiIntervalMs = 500L
    private val apiLock = ReentrantLock()

    private var accessToken: String? = null
    private var expiresAt: LocalDateTime? = null

    override fun getAccessToken(userId: String): String {
        val now = LocalDateTime.now()

        // 1. Memory cache
        if (accessToken != null && expiresAt != null && now.isBefore(expiresAt)) {
            return accessToken!!
        }

        // 2. DB cache
        val accountType = "mock" // Defaulting to mock for now, can be parameterized
        val tokenDoc =
                tokenRepository.findTopByUserIdAndAccountTypeOrderByCreatedAtDesc(
                        userId,
                        accountType
                )
        if (tokenDoc != null && now.isBefore(tokenDoc.expirationTime)) {
            this.accessToken = tokenDoc.accessToken
            this.expiresAt = tokenDoc.expirationTime
            return tokenDoc.accessToken
        }

        // 3. New token from KIS
        return refreshToken(userId, accountType)
    }

    private fun refreshToken(userId: String, accountType: String): String {
        logger.info("Refreshing KIS access token for user: $userId, type: $accountType")

        val response =
                webClient
                        .post()
                        .uri("/oauth2/tokenP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(
                                mapOf(
                                        "grant_type" to "client_credentials",
                                        "appkey" to kisConfig.appKey,
                                        "appsecret" to kisConfig.appSecret
                                )
                        )
                        .retrieve()
                        .bodyToMono(Map::class.java)
                        .block()
                        ?: throw RuntimeException("Failed to get access token from KIS")

        val token = response["access_token"] as String
        val expiresIn = (response["expires_in"] as Int).toLong()
        val expirationTime = LocalDateTime.now().plusSeconds(expiresIn)

        val kisToken =
                KisToken(
                        userId = userId,
                        accountType = accountType,
                        accessToken = token,
                        expirationTime = expirationTime
                )
        tokenRepository.save(kisToken)

        this.accessToken = token
        this.expiresAt = expirationTime

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
        waitForRateLimit()
        val token = getAccessToken()

        // TR_ID for overseas balance: VTTS3012R (Mock), TTTS3012R (Real)
        val trId = "VTTS3012R" // Assuming mock for now

        return webClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                            .path("/uapi/overseas-stock/v1/trading/inquire-balance")
                            .queryParam("CANO", kisConfig.accountNo.substring(0, 8))
                            .queryParam("ACNT_PRDT_CD", kisConfig.accountNo.substring(8))
                            .queryParam("OVRS_EXCG_CD", exchange)
                            .queryParam("TR_CRCY_CD", "USD")
                            .queryParam("CTX_AREA_FK200", "")
                            .queryParam("CTX_AREA_NK200", "")
                            .build()
                }
                .header("authorization", "Bearer $token")
                .header("appkey", kisConfig.appKey)
                .header("appsecret", kisConfig.appSecret)
                .header("tr_id", trId)
                .header("Content-Type", "application/json; charset=utf-8")
                .retrieve()
                .bodyToMono(Map::class.java)
                .block() as
                Map<String, Any>?
                ?: emptyMap()
    }
}
