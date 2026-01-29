package com.quantiq.core.repository

import com.quantiq.core.domain.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, String> {
    fun findByUserId(userId: String): User?
}
