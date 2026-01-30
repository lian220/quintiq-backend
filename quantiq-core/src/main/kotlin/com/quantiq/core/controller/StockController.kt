package com.quantiq.core.controller

import com.quantiq.core.domain.Stock
import com.quantiq.core.repository.StockRepository
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stocks")
class StockController(private val stockRepository: StockRepository) {

        @GetMapping
        fun getStocks(
                @RequestParam(required = false) isActive: Boolean?,
                @RequestParam(required = false) ticker: String?
        ): ResponseEntity<List<Stock>> {
                val stocks =
                        stockRepository
                                .findAll()
                                .filter { stock ->
                                        val activeMatch =
                                                isActive == null || stock.isActive == isActive
                                        val tickerMatch =
                                                ticker == null ||
                                                        stock.ticker.contains(
                                                                ticker,
                                                                ignoreCase = true
                                                        )
                                        activeMatch && tickerMatch
                                }
                                .sortedBy { it.ticker }
                return ResponseEntity.ok(stocks)
        }

        @GetMapping("/{ticker}")
        fun getStockByTicker(@PathVariable ticker: String): ResponseEntity<Stock> {
                val stock = stockRepository.findByTicker(ticker.uppercase())
                return stock?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
        }

        @PostMapping
        fun createOrUpdateStock(@RequestBody stockInput: Stock): ResponseEntity<Map<String, Any>> {
                val tickerUpper = stockInput.ticker.uppercase()
                val existing = stockRepository.findByTicker(tickerUpper)

                return if (existing != null) {
                        val updated =
                                existing.copy(
                                        stockName = stockInput.stockName,
                                        stockNameEn = stockInput.stockNameEn
                                                        ?: existing.stockNameEn,
                                        isEtf = stockInput.isEtf,
                                        leverageTicker = stockInput.leverageTicker
                                                        ?: existing.leverageTicker,
                                        exchange = stockInput.exchange ?: existing.exchange,
                                        sector = stockInput.sector ?: existing.sector,
                                        industry = stockInput.industry ?: existing.industry,
                                        isActive = stockInput.isActive,
                                        updatedAt = LocalDateTime.now()
                                )
                        stockRepository.save(updated)
                        ResponseEntity.ok(
                                mapOf(
                                        "success" to true,
                                        "message" to "Stock updated: $tickerUpper",
                                        "action" to "updated"
                                )
                        )
                } else {
                        val newStock =
                                stockInput.copy(
                                        ticker = tickerUpper,
                                        createdAt = LocalDateTime.now(),
                                        updatedAt = LocalDateTime.now()
                                )
                        val saved = stockRepository.save(newStock)
                        ResponseEntity.status(HttpStatus.CREATED)
                                .body(
                                        mapOf(
                                                "success" to true,
                                                "message" to "Stock created: $tickerUpper",
                                                "id" to (saved.id ?: ""),
                                                "action" to "created"
                                        )
                                )
                }
        }

        @PutMapping("/{ticker}")
        fun updateStock(
                @PathVariable ticker: String,
                @RequestBody updates: Stock
        ): ResponseEntity<Map<String, Any>> {
                val existing =
                        stockRepository.findByTicker(ticker.uppercase())
                                ?: return ResponseEntity.notFound().build()

                val updated =
                        existing.copy(
                                stockName = updates.stockName,
                                stockNameEn = updates.stockNameEn ?: existing.stockNameEn,
                                isEtf = updates.isEtf,
                                leverageTicker = updates.leverageTicker ?: existing.leverageTicker,
                                exchange = updates.exchange ?: existing.exchange,
                                sector = updates.sector ?: existing.sector,
                                industry = updates.industry ?: existing.industry,
                                isActive = updates.isActive,
                                updatedAt = LocalDateTime.now()
                        )
                stockRepository.save(updated)
                return ResponseEntity.ok(
                        mapOf(
                                "success" to true,
                                "message" to "Stock updated: ${ticker.uppercase()}"
                        )
                )
        }

        @DeleteMapping("/{ticker}")
        fun deleteStock(@PathVariable ticker: String): ResponseEntity<Map<String, Any>> {
                val existing =
                        stockRepository.findByTicker(ticker.uppercase())
                                ?: return ResponseEntity.notFound().build()

                // Soft delete
                val deactivated = existing.copy(isActive = false, updatedAt = LocalDateTime.now())
                stockRepository.save(deactivated)
                return ResponseEntity.ok(
                        mapOf(
                                "success" to true,
                                "message" to "Stock deactivated: ${ticker.uppercase()}",
                                "action" to "deactivated"
                        )
                )
        }
}
