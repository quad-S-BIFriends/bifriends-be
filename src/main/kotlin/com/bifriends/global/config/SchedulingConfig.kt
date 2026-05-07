package com.bifriends.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Spring Scheduling 활성화
 * @Scheduled 어노테이션을 사용하려면 반드시 필요하다.
 */
@Configuration
@EnableScheduling
class SchedulingConfig
