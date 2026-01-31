package com.quantiq.core.domain.model

/**
 * 분석 요청 Domain Model
 */
data class AnalysisRequest(
    val timestamp: String,
    val source: String,
    val requestId: String,
    val threadTs: String? = null,
    val analysisType: String // "TECHNICAL", "SENTIMENT", "COMBINED"
)
