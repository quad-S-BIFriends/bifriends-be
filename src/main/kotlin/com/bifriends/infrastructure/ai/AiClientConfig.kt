package com.bifriends.infrastructure.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AiServiceProperties::class)
class AiClientConfig {

    @Bean
    fun aiRestClient(properties: AiServiceProperties): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .build()
}
