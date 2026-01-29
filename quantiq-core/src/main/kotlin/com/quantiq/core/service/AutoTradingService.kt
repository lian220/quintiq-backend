package com.quantiq.core.service

import com.quantiq.core.repository.StockRecommendationRepository
import com.quantiq.core.repository.UserRepository
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutoTradingService(
        private val userRepository: UserRepository,
        private val stockRecommendationRepository: StockRecommendationRepository,
        private val balanceService: BalanceService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun executeAutoTrading() {
        logger.info("Starting Auto Trading Execution...")
        val today = LocalDate.now().toString()

        // 1. Get Recommendations
        val recommendations = stockRecommendationRepository.findByDateAndIsRecommendedTrue(today)
        logger.info("Found ${recommendations.size} recommendations for today ($today)")

        if (recommendations.isEmpty()) {
            logger.info("No recommendations found. Skipping trading.")
            return
        }

        // 2. Get Users with Auto Trading Enabled
        // Simple scan for now (in prod, use optimized query)
        val users =
                userRepository.findAll().filter {
                    it.tradingConfig?.enabled == true && it.tradingConfig.autoTradingEnabled
                }
        logger.info("Found ${users.size} active users for auto trading.")

        users.forEach { user ->
            try {
                logger.info("Processing user: ${user.userId}")

                // 3. Check Balance
                val cash = balanceService.getAvailableCash()
                logger.info("User ${user.userId} Cash: $cash")

                // 4. Execute Logic (Simplified)
                // Logic: Buy top recommended stocks if not already held
                val maxStocks = user.tradingConfig?.maxStocksToBuy ?: 5
                val targetStocks = recommendations.take(maxStocks)

                targetStocks.forEach { stock ->
                    logger.info("Placing BUY order for ${stock.ticker} (User: ${user.userId})")
                    // Real implementation would call OrderService/BrokerAPI here
                }
            } catch (e: Exception) {
                logger.error("Error processing user ${user.userId}", e)
            }
        }

        logger.info("Auto Trading Execution Completed.")
    }
}
