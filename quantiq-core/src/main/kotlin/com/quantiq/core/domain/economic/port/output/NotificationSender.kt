package com.quantiq.core.domain.economic.port.output

/**
 * 알림 전송 인터페이스 (Output Port)
 * Slack과 같은 알림 서비스에 메시지를 전송하는 인터페이스입니다.
 */
interface NotificationSender {
    /**
     * 경제 데이터 업데이트 요청 알림
     *
     * @return Slack 스레드 타임스탬프 (답글용)
     */
    fun notifyEconomicDataUpdateRequest(requestId: String): String?

    /**
     * 경제 데이터 수집 오류 알림
     */
    fun notifyEconomicDataCollectionError(requestId: String, error: String)
}
