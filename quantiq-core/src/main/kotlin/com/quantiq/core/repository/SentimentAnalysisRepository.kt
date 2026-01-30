package com.quantiq.core.repository

import com.quantiq.core.domain.SentimentAnalysis
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface SentimentAnalysisRepository : MongoRepository<SentimentAnalysis, String> {
    fun findByTicker(ticker: String): List<SentimentAnalysis>
    fun findByTickerAndDate(ticker: String, date: String): SentimentAnalysis?
}
