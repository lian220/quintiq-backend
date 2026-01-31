package com.quantiq.core.adapter.output.persistence.mongodb

import com.quantiq.core.domain.StockAnalysis
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockAnalysisRepository : MongoRepository<StockAnalysis, String> {
    fun findByTicker(ticker: String): List<StockAnalysis>
}
