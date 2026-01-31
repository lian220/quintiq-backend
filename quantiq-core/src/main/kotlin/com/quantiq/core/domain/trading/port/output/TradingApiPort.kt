package com.quantiq.core.domain.trading.port.output

/**
 * Trading API Port (Output Port)
 * 외부 증권사 API와의 연동을 추상화합니다.
 */
interface TradingApiPort {

    /**
     * Access Token 조회
     * @param userId 사용자 ID
     * @return Access Token
     */
    fun getAccessToken(userId: String = "lian"): String

    /**
     * 해외 주식 잔고 조회
     * @param exchange 거래소 코드 (기본값: NASD)
     * @return 잔고 정보 Map
     */
    fun getOverseasBalance(exchange: String = "NASD"): Map<String, Any>
}
