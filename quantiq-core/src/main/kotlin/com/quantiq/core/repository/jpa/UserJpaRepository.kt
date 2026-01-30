package com.quantiq.core.repository.jpa

import com.quantiq.core.entity.UserEntity
import com.quantiq.core.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, Long> {

    fun findByUserId(userId: String): Optional<UserEntity>

    fun findByEmail(email: String): Optional<UserEntity>

    fun findByStatus(status: UserStatus): List<UserEntity>

    @Query("""
        SELECT u FROM UserEntity u
        JOIN FETCH u.tradingConfig tc
        WHERE tc.enabled = true AND tc.autoTradingEnabled = true
    """)
    fun findUsersWithAutoTradingEnabled(): List<UserEntity>

    @Query("""
        SELECT u FROM UserEntity u
        LEFT JOIN FETCH u.tradingConfig
        LEFT JOIN FETCH u.accountBalance
        WHERE u.userId = :userId
    """)
    fun findByUserIdWithDetails(userId: String): Optional<UserEntity>

    fun existsByUserId(userId: String): Boolean

    fun existsByEmail(email: String): Boolean
}
