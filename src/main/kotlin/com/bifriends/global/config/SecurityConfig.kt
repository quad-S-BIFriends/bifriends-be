package com.bifriends.global.config

import com.bifriends.infrastructure.security.InternalServiceAuthenticationFilter
import com.bifriends.infrastructure.security.InternalServicePaths
import com.bifriends.infrastructure.security.JwtAuthenticationFilter
import com.bifriends.infrastructure.security.JwtProvider
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val internalServiceAuthenticationFilter: InternalServiceAuthenticationFilter,
    private val objectMapper: ObjectMapper
) {

    /**
     * Spring Security 필터 체인 설정
     * - CSRF 비활성화 (REST API이므로)
     * - 세션 사용 안 함 (JWT 기반 Stateless)
     * - OAuth2 로그인 설정 및 성공 핸들러 등록
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("{\"error\":\"Unauthorized\"}")
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        AntPathRequestMatcher("/health"),
                        AntPathRequestMatcher("/error"),
                        AntPathRequestMatcher("/actuator/health"),
                        AntPathRequestMatcher("/actuator/health/**"),
                        AntPathRequestMatcher("/api/v1/members/auth/**"),
                        AntPathRequestMatcher("/oauth2/**"),
                        AntPathRequestMatcher("/login/**"),
                    ).permitAll()
                    .requestMatchers(
                        AntPathRequestMatcher("/swagger-ui/**"),
                        AntPathRequestMatcher("/v3/api-docs/**"),
                        AntPathRequestMatcher("/swagger-ui.html"),
                    ).permitAll()
                    // JWT API는 anyRequest 한 곳에서만 authenticated() — 경로별 MVC matcher는 JWT가 401로 막히는 경우가 있음
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(internalServiceAuthenticationFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtFilterRegistration(filter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> {
        return FilterRegistrationBean(filter).apply { isEnabled = false }
    }

    @Bean
    fun internalServiceFilterRegistration(
        filter: InternalServiceAuthenticationFilter,
    ): FilterRegistrationBean<InternalServiceAuthenticationFilter> {
        return FilterRegistrationBean(filter).apply { isEnabled = false }
    }

}
