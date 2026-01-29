package com.quantiq.core.domain

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "stock_recommendations")
data class StockRecommendation(
        @Id val id: String? = null,
        val ticker: String,
        val date: String, // YYYY-MM-DD
        @Field("stock_name") val stockName: String,
        @Field("technical_indicators") val technicalIndicators: TechnicalIndicators,
        @Field("is_recommended") val isRecommended: Boolean = false,
        @Field("updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class TechnicalIndicators(
        val sma20: Double? = null,
        val sma50: Double? = null,
        val rsi: Double? = null,
        val macd: Double? = null,
        val signal: Double? = null
)
