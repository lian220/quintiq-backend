package com.quantiq.core.repository

import com.quantiq.core.domain.Stock
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockRepository : MongoRepository<Stock, String> {
    fun findByTicker(ticker: String): Stock?
    fun findByIsActive(isActive: Boolean): List<Stock>
}
