package com.quantiq.core.domain.analysis.port.input

import java.util.concurrent.CompletableFuture

/**
 * 분석 UseCase (Input Port)
 * 기술적 분석, 감정 분석, 통합 분석을 트리거하는 비즈니스 로직
 */
interface AnalysisUseCase {

    /**
     * 기술적 분석 트리거
     * SMA, RSI, MACD 등 기술적 지표 분석 요청
     */
    fun triggerTechnicalAnalysis(): CompletableFuture<String>

    /**
     * 뉴스 감정 분석 트리거
     * Alpha Vantage NEWS_SENTIMENT API를 통한 감정 분석 요청
     */
    fun triggerSentimentAnalysis(): CompletableFuture<String>

    /**
     * 통합 분석 트리거
     * 기술적 분석 + 감정 분석 + 통합 점수 계산
     */
    fun triggerCombinedAnalysis(): CompletableFuture<String>
}
