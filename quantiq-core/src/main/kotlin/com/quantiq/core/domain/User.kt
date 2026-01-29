package com.quantiq.core.domain

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "users")
data class User(
        @Id val id: String? = null,
        @Field("user_id") val userId: String,
        val name: String? = null,
        @Field("trading_config") val tradingConfig: TradingConfig? = null,
        @Field("updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class TradingConfig(
        val enabled: Boolean = false,
        @Field("auto_trading_enabled") val autoTradingEnabled: Boolean = false,
        @Field("min_composite_score") val minCompositeScore: Double = 2.0,
        @Field("max_stocks_to_buy") val maxStocksToBuy: Int = 5,
        @Field("max_amount_per_stock") val maxAmountPerStock: Double = 10000.0,
        @Field("stop_loss_percent") val stopLossPercent: Double = -7.0,
        @Field("take_profit_percent") val takeProfitPercent: Double = 5.0
)
