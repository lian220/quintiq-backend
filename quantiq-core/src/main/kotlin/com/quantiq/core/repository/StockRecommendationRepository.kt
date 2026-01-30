package com.quantiq.core.repository

import com.quantiq.core.domain.StockRecommendation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockRecommendationRepository : MongoRepository<StockRecommendation, String> {
    fun findByDateAndIsRecommendedTrue(date: String): List<StockRecommendation>
    fun findByTickerAndDate(ticker: String, date: String): StockRecommendation?
}
