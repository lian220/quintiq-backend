package com.quantiq.core.controller

import com.quantiq.core.domain.User
import com.quantiq.core.domain.UserStockEmbedded
import com.quantiq.core.repository.StockRepository
import com.quantiq.core.repository.UserRepository
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
        private val userRepository: UserRepository,
        private val stockRepository: StockRepository
) {

        @GetMapping
        fun getUsers(
                @RequestParam(required = false) userId: String?,
                @RequestParam(required = false) email: String?
        ): ResponseEntity<List<User>> {
                val users =
                        userRepository.findAll().filter { user ->
                                val userIdMatch =
                                        userId == null ||
                                                user.userId.contains(userId, ignoreCase = true)
                                val emailMatch =
                                        email == null ||
                                                (user.email?.contains(email, ignoreCase = true)
                                                        ?: false)
                                userIdMatch && emailMatch
                        }
                return ResponseEntity.ok(users)
        }

        @GetMapping("/{userId}")
        fun getUser(@PathVariable userId: String): ResponseEntity<Map<String, Any>> {
                val user =
                        userRepository.findByUserId(userId)
                                ?: return ResponseEntity.notFound().build()

                // Enrichment logic: Join users.stocks with stocks collection
                val enrichedStocks =
                        user.stocks.map { userStock ->
                                val stockInfo = stockRepository.findByTicker(userStock.ticker)
                                mapOf(
                                        "ticker" to userStock.ticker,
                                        "use_leverage" to userStock.useLeverage,
                                        "notes" to userStock.notes,
                                        "tags" to userStock.tags,
                                        "is_active" to userStock.isActive,
                                        "added_at" to userStock.addedAt,
                                        "stock_name" to (stockInfo?.stockName ?: ""),
                                        "stock_name_en" to (stockInfo?.stockNameEn ?: ""),
                                        "is_etf" to (stockInfo?.isEtf ?: false),
                                        "leverage_ticker" to (stockInfo?.leverageTicker ?: ""),
                                        "exchange" to (stockInfo?.exchange ?: ""),
                                        "sector" to (stockInfo?.sector ?: ""),
                                        "industry" to (stockInfo?.industry ?: "")
                                )
                        }

                val userMap =
                        mutableMapOf<String, Any>(
                                "id" to (user.id ?: ""),
                                "user_id" to user.userId,
                                "email" to (user.email ?: ""),
                                "display_name" to (user.displayName ?: ""),
                                "preferences" to (user.preferences ?: emptyMap<String, Any>()),
                                "stocks" to enrichedStocks,
                                "account_balance" to
                                        (user.accountBalance ?: emptyMap<String, Any>()),
                                "trading_config" to (user.tradingConfig ?: emptyMap<String, Any>()),
                                "created_at" to user.createdAt,
                                "updated_at" to user.updatedAt
                        )

                return ResponseEntity.ok(userMap)
        }

        @PostMapping
        fun createUser(@RequestBody user: User): ResponseEntity<Map<String, Any>> {
                if (userRepository.findByUserId(user.userId) != null) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(mapOf("error" to "User already exists"))
                }
                val saved =
                        userRepository.save(
                                user.copy(
                                        createdAt = LocalDateTime.now(),
                                        updatedAt = LocalDateTime.now()
                                )
                        )
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(
                                mapOf(
                                        "success" to true,
                                        "user_id" to saved.userId,
                                        "id" to (saved.id ?: "")
                                )
                        )
        }

        @PutMapping("/{userId}")
        fun updateUser(
                @PathVariable userId: String,
                @RequestBody updates: User
        ): ResponseEntity<Map<String, Any>> {
                val existing =
                        userRepository.findByUserId(userId)
                                ?: return ResponseEntity.notFound().build()
                val updated =
                        existing.copy(
                                email = updates.email ?: existing.email,
                                displayName = updates.displayName ?: existing.displayName,
                                preferences = updates.preferences ?: existing.preferences,
                                tradingConfig = updates.tradingConfig ?: existing.tradingConfig,
                                updatedAt = LocalDateTime.now()
                        )
                userRepository.save(updated)
                return ResponseEntity.ok(mapOf("success" to true))
        }

        @DeleteMapping("/{userId}")
        fun deleteUser(@PathVariable userId: String): ResponseEntity<Map<String, Any>> {
                val existing =
                        userRepository.findByUserId(userId)
                                ?: return ResponseEntity.notFound().build()
                userRepository.delete(existing)
                return ResponseEntity.ok(mapOf("success" to true))
        }

        // --- User Stocks (Holdings/Watchlist) ---

        @PostMapping("/{userId}/stocks")
        fun addUserStock(
                @PathVariable userId: String,
                @RequestBody stockAdd: UserStockEmbedded
        ): ResponseEntity<Map<String, Any>> {
                val user =
                        userRepository.findByUserId(userId)
                                ?: return ResponseEntity.notFound().build()

                // Verify stock exists in global stocks collection
                if (stockRepository.findByTicker(stockAdd.ticker.uppercase()) == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(mapOf("error" to "Stock not found in master list"))
                }

                if (user.stocks.any { it.ticker == stockAdd.ticker.uppercase() }) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(mapOf("error" to "Stock already in user list"))
                }

                val newStocks =
                        user.stocks +
                                stockAdd.copy(
                                        ticker = stockAdd.ticker.uppercase(),
                                        addedAt = LocalDateTime.now()
                                )
                userRepository.save(user.copy(stocks = newStocks, updatedAt = LocalDateTime.now()))

                return ResponseEntity.ok(mapOf("success" to true))
        }

        @DeleteMapping("/{userId}/stocks/{ticker}")
        fun removeUserStock(
                @PathVariable userId: String,
                @PathVariable ticker: String
        ): ResponseEntity<Map<String, Any>> {
                val user =
                        userRepository.findByUserId(userId)
                                ?: return ResponseEntity.notFound().build()
                val tickerUpper = ticker.uppercase()

                if (user.stocks.none { it.ticker == tickerUpper }) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(mapOf("error" to "Stock not found in user list"))
                }

                val newStocks = user.stocks.filter { it.ticker != tickerUpper }
                userRepository.save(user.copy(stocks = newStocks, updatedAt = LocalDateTime.now()))

                return ResponseEntity.ok(mapOf("success" to true))
        }
}
