package com.bifriends.infrastructure.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(AiServiceProperties::class)
class AiClientConfig {

    /**
     * JDK [SimpleClientHttpRequestFactory] = HTTP/1.1.
     * 일부 환경에서 기본 클라이언트가 POST body를 AI(FastAPI)에 전달하지 못해
     * `loc: ["body"], input: null` 422가 나는 경우가 있어 이 팩토리를 사용한다.
     */
    @Bean
    fun aiRestClient(properties: AiServiceProperties): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(15))
            setReadTimeout(Duration.ofMinutes(3))
        }
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(factory)
            .build()
    }
}
