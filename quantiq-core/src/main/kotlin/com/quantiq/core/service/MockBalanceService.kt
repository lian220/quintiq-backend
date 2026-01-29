package com.quantiq.core.service

import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MockBalanceService : BalanceService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAvailableCash(): BigDecimal {
        // Mock implementation
        logger.info("Mock: Fetching available cash")
        return BigDecimal("100000.00") // $100,000
    }

    override fun getStockHoldings(): Map<String, Int> {
        // Mock implementation
        logger.info("Mock: Fetching stock holdings")
        return mapOf("AAPL" to 10, "TSLA" to 5)
    }
}
