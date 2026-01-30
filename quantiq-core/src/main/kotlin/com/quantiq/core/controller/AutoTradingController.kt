package com.quantiq.core.controller

import com.quantiq.core.domain.TradingConfig
import com.quantiq.core.repository.UserRepository
import com.quantiq.core.service.AutoTradingService
import java.time.LocalDateTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auto-trading")
class AutoTradingController(
        private val userRepository: UserRepository,
        private val autoTradingService: AutoTradingService
) {

    @GetMapping("/config")
    fun getConfig(@RequestParam userId: String): ResponseEntity<TradingConfig> {
        val user = userRepository.findByUserId(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.tradingConfig ?: TradingConfig())
    }

    @PutMapping("/config")
    fun updateConfig(
            @RequestParam userId: String,
            @RequestBody config: TradingConfig
    ): ResponseEntity<Map<String, Any>> {
        val user = userRepository.findByUserId(userId) ?: return ResponseEntity.notFound().build()
        val updatedUser = user.copy(tradingConfig = config, updatedAt = LocalDateTime.now())
        userRepository.save(updatedUser)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/execute/buy")
    fun executeBuy(
            @RequestParam(defaultValue = "true") dryRun: Boolean
    ): ResponseEntity<Map<String, Any>> {
        // Normally this would trigger the service logic
        autoTradingService.executeAutoTrading()
        return ResponseEntity.ok(mapOf("success" to true, "message" to "Buy execution triggered"))
    }

    @PostMapping("/execute/sell")
    fun executeSell(
            @RequestParam(defaultValue = "true") dryRun: Boolean
    ): ResponseEntity<Map<String, Any>> {
        // Trigger sell logic
        return ResponseEntity.ok(mapOf("success" to true, "message" to "Sell execution triggered"))
    }

    @GetMapping("/status")
    fun getStatus(@RequestParam userId: String): ResponseEntity<Map<String, Any>> {
        val user = userRepository.findByUserId(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
                mapOf(
                        "success" to true,
                        "config" to (user.tradingConfig ?: emptyMap<String, Any>()),
                        "status" to "ACTIVE"
                )
        )
    }
}
