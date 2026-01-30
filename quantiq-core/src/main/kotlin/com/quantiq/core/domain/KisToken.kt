package com.quantiq.core.domain

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "access_tokens")
data class KisToken(
        @Id val id: String? = null,
        @Field("user_id") val userId: String = "lian",
        @Field("account_type") val accountType: String, // "mock" or "real"
        @Field("access_token") val accessToken: String,
        @Field("expiration_time") val expirationTime: LocalDateTime,
        @Field("is_active") val isActive: Boolean = true,
        @Field("created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
        @Field("updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
