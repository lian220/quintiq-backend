package com.quantiq.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class KisConfig {
    @Value("\${kis.app-key}") lateinit var appKey: String

    @Value("\${kis.app-secret}") lateinit var appSecret: String

    @Value("\${kis.base-url}") var baseUrl: String = "https://openapi.koreainvestment.com:9443"

    @Value("\${kis.account-no}") lateinit var accountNo: String
}
