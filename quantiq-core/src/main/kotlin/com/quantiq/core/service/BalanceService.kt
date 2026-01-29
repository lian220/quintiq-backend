package com.quantiq.core.service

import java.math.BigDecimal

interface BalanceService {
    fun getAvailableCash(): BigDecimal
    fun getStockHoldings(): Map<String, Int> // Ticker -> Quantity
}
