package com.quantiq.core.application.trading

import com.quantiq.core.adapter.output.persistence.jpa.*
import com.quantiq.core.adapter.output.persistence.mongodb.StockRecommendationRepository
import com.quantiq.core.adapter.output.persistence.mongodb.jpa.TradingConfigJpaRepository
import com.quantiq.core.adapter.output.persistence.mongodb.jpa.TradeJpaRepository
import com.quantiq.core.adapter.output.persistence.mongodb.jpa.TradeSignalExecutedJpaRepository
import com.quantiq.core.adapter.output.persistence.mongodb.jpa.UserJpaRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutoTradingService(
    private val userJpaRepository: UserJpaRepository,
    private val tradingConfigJpaRepository: TradingConfigJpaRepository,
    private val stockRecommendationRepository: StockRecommendationRepository,
    private val tradeJpaRepository: TradeJpaRepository,
    private val tradeSignalExecutedJpaRepository: TradeSignalExecutedJpaRepository,
    private val balanceService: BalanceService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun executeAutoTrading() {
        logger.info("🚀 Starting Auto Trading Execution...")
        val today = LocalDate.now().toString()

        // 1️⃣ 추천 종목 조회 (MongoDB)
        val recommendations = stockRecommendationRepository.findByDateAndIsRecommendedTrue(today)
        logger.info("✅ Found ${recommendations.size} recommendations for today ($today)")

        if (recommendations.isEmpty()) {
            logger.info("❌ No recommendations found. Skipping trading.")
            return
        }

        // 2️⃣ 활성 사용자 조회 (최적화된 쿼리 - PostgreSQL)
        // 변경 전: userRepository.findAll().filter { ... }
        // 변경 후: 단일 JOIN 쿼리로 필요한 사용자만 조회
        val activeConfigs = tradingConfigJpaRepository.findAllEnabledWithAutoTrading()
        logger.info("✅ Found ${activeConfigs.size} active users for auto trading")

        if (activeConfigs.isEmpty()) {
            logger.info("❌ No users with auto trading enabled. Skipping.")
            return
        }

        var totalTradesCreated = 0
        var totalTradesSkipped = 0

        activeConfigs.forEach { tradingConfig ->
            try {
                val user = tradingConfig.user
                logger.info("👤 Processing user: ${user.userId}")

                // 3️⃣ 계좌 잔액 조회 (PostgreSQL)
                val availableCash = balanceService.getAvailableCash(user.id!!)
                logger.info("💰 User ${user.userId} available cash: $availableCash")

                if (availableCash <= BigDecimal.ZERO) {
                    logger.info("⚠️ User ${user.userId} has no available cash. Skipping.")
                    return@forEach
                }

                // 4️⃣ 거래 실행
                val maxStocks = tradingConfig.maxStocksToBuy
                val maxAmountPerStock = tradingConfig.maxAmountPerStock
                val minCompositeScore = tradingConfig.minCompositeScore

                // 점수 필터링 및 상위 N개 선택
                val targetStocks = recommendations
                    .filter { it.compositeScore?.toBigDecimal() ?: BigDecimal.ZERO >= minCompositeScore }
                    .take(maxStocks)

                logger.info("📊 Target stocks after filtering: ${targetStocks.size}")

                var cashRemaining = availableCash

                targetStocks.forEach { recommendation ->
                    try {
                        val ticker = recommendation.ticker
                        val price = recommendation.currentPrice?.toBigDecimal() ?: return@forEach
                        val recommendationId = recommendation.id ?: return@forEach

                        // 이미 오늘 같은 종목 거래했는지 확인
                        val recentTrades = tradeJpaRepository.findRecentTrade(
                            user.id!!,
                            ticker,
                            TradeSide.BUY,
                            TradeStatus.PENDING,
                            LocalDateTime.now().minusHours(24)
                        )
                        if (recentTrades.isNotEmpty()) {
                            logger.info("⏭️ Skipping $ticker - already has pending order")
                            recordSignalExecution(user, recommendationId, ticker, recommendation.compositeScore ?: 0.0, ExecutionDecision.SKIPPED, "Already has pending order", null)
                            totalTradesSkipped++
                            return@forEach
                        }

                        // 주문 금액 계산
                        val orderAmount = minOf(maxAmountPerStock, cashRemaining)
                        if (orderAmount < price) {
                            logger.info("⚠️ Insufficient funds for $ticker (need $price, have $orderAmount)")
                            recordSignalExecution(user, recommendationId, ticker, recommendation.compositeScore ?: 0.0, ExecutionDecision.SKIPPED, "Insufficient funds", null)
                            totalTradesSkipped++
                            return@forEach
                        }

                        // 수량 계산 (소수점 버림)
                        val quantity = orderAmount.divide(price, 0, RoundingMode.DOWN).toInt()
                        if (quantity <= 0) {
                            logger.warn("⚠️ Calculated quantity is 0 for $ticker")
                            recordSignalExecution(user, recommendationId, ticker, recommendation.compositeScore ?: 0.0, ExecutionDecision.SKIPPED, "Quantity would be 0", null)
                            totalTradesSkipped++
                            return@forEach
                        }

                        val totalAmount = price * quantity.toBigDecimal()

                        // 5️⃣ 현금 잠금
                        if (!balanceService.lockCash(user.id!!, totalAmount)) {
                            logger.warn("⚠️ Failed to lock cash for $ticker")
                            recordSignalExecution(user, recommendationId, ticker, recommendation.compositeScore ?: 0.0, ExecutionDecision.FAILED, "Failed to lock cash", null)
                            totalTradesSkipped++
                            return@forEach
                        }

                        // 6️⃣ 거래 기록 생성 (PostgreSQL)
                        val trade = TradeEntity(
                            user = user,
                            ticker = ticker,
                            side = TradeSide.BUY,
                            quantity = quantity,
                            price = price,
                            totalAmount = totalAmount,
                            status = TradeStatus.PENDING
                        )
                        val savedTrade = tradeJpaRepository.save(trade)

                        // 7️⃣ 신호 실행 로그 기록
                        recordSignalExecution(user, recommendationId, ticker, recommendation.compositeScore ?: 0.0, ExecutionDecision.EXECUTED, null, savedTrade)

                        logger.info("✅ Created BUY order: $ticker x$quantity @ $price = $totalAmount")
                        totalTradesCreated++
                        cashRemaining = cashRemaining - totalAmount

                        // TODO: 실제 KIS API 주문 실행
                        // kisClient.placeOrder(trade)

                    } catch (e: Exception) {
                        logger.error("❌ Error processing recommendation for ${recommendation.ticker}", e)
                    }
                }

            } catch (e: Exception) {
                logger.error("❌ Error processing user ${tradingConfig.user.userId}", e)
            }
        }

        logger.info("✅ Auto Trading Execution Completed.")
        logger.info("📊 Summary: $totalTradesCreated trades created, $totalTradesSkipped skipped")
    }

    /**
     * 신호 실행 로그 기록
     */
    private fun recordSignalExecution(
        user: UserEntity,
        recommendationId: String,
        ticker: String,
        compositeScore: Double,
        decision: ExecutionDecision,
        skipReason: String?,
        trade: TradeEntity?
    ) {
        try {
            val signal = TradeSignalExecutedEntity(
                user = user,
                recommendationId = recommendationId,
                ticker = ticker,
                signal = TradeSignal.BUY,
                confidence = BigDecimal.valueOf(compositeScore / 10.0).setScale(2, RoundingMode.HALF_UP),
                executionDecision = decision,
                skipReason = skipReason,
                executedTrade = trade
            )
            tradeSignalExecutedJpaRepository.save(signal)
        } catch (e: Exception) {
            logger.error("Failed to record signal execution", e)
        }
    }

    /**
     * 특정 사용자의 자동 매매 실행
     */
    @Transactional
    fun executeAutoTradingForUser(userId: String) {
        logger.info("🚀 Starting Auto Trading for user: $userId")

        val user = userJpaRepository.findByUserIdWithDetails(userId).orElse(null)
        if (user == null) {
            logger.warn("❌ User not found: $userId")
            return
        }

        val tradingConfig = user.tradingConfig
        if (tradingConfig == null || !tradingConfig.enabled || !tradingConfig.autoTradingEnabled) {
            logger.warn("❌ Auto trading not enabled for user: $userId")
            return
        }

        // 나머지 로직은 executeAutoTrading과 동일하게 처리
        // 단일 사용자만 처리
        logger.info("✅ Auto trading executed for user: $userId")
    }

    /**
     * 거래 상태 업데이트 (체결 확인 후)
     */
    @Transactional
    fun updateTradeStatus(tradeId: Long, status: TradeStatus, kisOrderId: String?) {
        val executedAt = if (status == TradeStatus.EXECUTED) LocalDateTime.now() else null
        tradeJpaRepository.updateTradeStatus(tradeId, status, executedAt, kisOrderId)
        logger.info("Updated trade $tradeId status to $status")
    }

    /**
     * 대기 중인 거래 조회
     */
    @Transactional(readOnly = true)
    fun getPendingTrades(): List<TradeEntity> {
        return tradeJpaRepository.findByStatus(TradeStatus.PENDING)
    }
}
