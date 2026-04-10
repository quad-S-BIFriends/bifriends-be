package com.bifriends.infrastructure.security

import com.bifriends.domain.member.model.Member
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * Spring Security 인증 객체.
 * OAuth2 로그인 후 SecurityContext에 저장되며,
 * Controller에서 @AuthenticationPrincipal로 접근 가능.
 */
class PrincipalDetails(
    private val member: Member,
    private val attributes: Map<String, Any>
) : OAuth2User {

    override fun getName(): String = member.providerId

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority(member.role.name))

    fun getMember(): Member = member
}
