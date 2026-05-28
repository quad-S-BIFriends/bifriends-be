package com.bifriends.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Docker 내부망에서 AI 서비스가 BE API를 호출할 때 JWT 대신
 * [InternalServiceProperties.headerName] 헤더로 인증한다.
 */
@Component
class InternalServiceAuthenticationFilter(
    private val properties: InternalServiceProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!properties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val path = request.servletPath.takeIf { it.isNotEmpty() }
            ?: request.requestURI.removePrefix(request.contextPath)
        if (!InternalServicePaths.matches(request.method, path)) {
            filterChain.doFilter(request, response)
            return
        }

        val provided = request.getHeader(properties.headerName)
        if (provided == null) {
            filterChain.doFilter(request, response)
            return
        }

        if (provided != properties.expectedValue) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("{\"error\":\"Invalid internal service credentials\"}")
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(
            properties.expectedValue,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${InternalServicePaths.ROLE}")),
        )
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }
}
