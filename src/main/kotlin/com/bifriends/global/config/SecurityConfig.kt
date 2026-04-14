package com.bifriends.global.config

import com.bifriends.domain.member.service.CustomOAuth2UserService
import com.bifriends.infrastructure.security.JwtProvider
import com.bifriends.infrastructure.security.PrincipalDetails
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.core.Authentication
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val jwtProvider: JwtProvider,
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
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/health", "/api/v1/members/auth/**", "/oauth2/**", "/login/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/v1/onboarding/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
                    .successHandler(oAuth2AuthenticationSuccessHandler())
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:8080", "http://localhost:3000", "http://localhost:5000")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun oAuth2AuthenticationSuccessHandler(): AuthenticationSuccessHandler {
        return AuthenticationSuccessHandler { _: HttpServletRequest, response: HttpServletResponse, authentication: Authentication ->
            val principal = authentication.principal as PrincipalDetails
            val member = principal.getMember()

            val accessToken = jwtProvider.generateAccessToken(
                memberId = member.id,
                email = member.email,
                role = member.role.name
            )
            val refreshToken = jwtProvider.generateRefreshToken(
                memberId = member.id,
                email = member.email,
                role = member.role.name
            )

            val tokenResponse = mapOf(
                "accessToken" to accessToken,
                "refreshToken" to refreshToken,
                "email" to member.email,
                "name" to member.name,
                "profileImageUrl" to member.profileImageUrl,
                "onboardingCompleted" to member.onboardingCompleted
            )

            response.contentType = "application/json;charset=UTF-8"
            response.status = HttpServletResponse.SC_OK
            response.writer.write(objectMapper.writeValueAsString(tokenResponse))
        }
    }
}
