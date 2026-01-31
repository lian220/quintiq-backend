package com.quantiq.core.adapter.output.external

import com.quantiq.core.domain.economic.port.output.RestApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CompletableFuture

/**
 * WebClient REST API Adapter (Output Adapter)
 * RestApiClient 인터페이스를 구현하여 외부 REST API와 연동합니다.
 */
@Component
class WebClientRestApiAdapter(
    private val webClient: WebClient
) : RestApiClient {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun callEconomicDataCollectionApi(url: String): CompletableFuture<String> {
        return try {
            logger.info("REST API 호출: $url")

            webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(String::class.java)
                .toFuture()
        } catch (e: Exception) {
            logger.error("REST API 호출 실패: $url", e)
            CompletableFuture.failedFuture(e)
        }
    }
}
