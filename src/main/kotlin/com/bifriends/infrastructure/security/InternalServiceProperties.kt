package com.bifriends.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "internal.service")
data class InternalServiceProperties(
    /** AI 등 내부 서비스 호출 시 헤더 인증 사용 여부 */
    val enabled: Boolean = true,
    val headerName: String = "X-Internal-Service",
    /** 헤더 값과 일치해야 통과 (환경 변수로 교체 권장) */
    val expectedValue: String = "bifriends-ai",
)
