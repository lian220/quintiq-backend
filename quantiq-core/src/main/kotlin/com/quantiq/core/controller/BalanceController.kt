package com.quantiq.core.controller

import com.quantiq.core.service.BalanceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/balance")
class BalanceController(private val balanceService: BalanceService) {

    @GetMapping("/overseas")
    fun getOverseasBalance(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(balanceService.getOverseasBalance())
    }

    @GetMapping("/profit")
    fun getTotalProfit(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(balanceService.getTotalProfit())
    }
}
