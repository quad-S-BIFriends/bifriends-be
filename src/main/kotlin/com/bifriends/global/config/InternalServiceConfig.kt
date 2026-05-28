package com.bifriends.global.config

import com.bifriends.infrastructure.security.InternalServiceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(InternalServiceProperties::class)
class InternalServiceConfig
