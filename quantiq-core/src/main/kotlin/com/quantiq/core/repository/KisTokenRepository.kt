package com.quantiq.core.repository

import com.quantiq.core.domain.KisToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface KisTokenRepository : MongoRepository<KisToken, String> {
    fun findTopByUserIdAndAccountTypeOrderByCreatedAtDesc(
            userId: String,
            accountType: String
    ): KisToken?
}
