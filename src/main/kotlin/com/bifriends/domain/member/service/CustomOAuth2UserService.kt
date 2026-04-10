package com.bifriends.domain.member.service

import com.bifriends.infrastructure.security.PrincipalDetails
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * [로그인/회원가입 진입점]
 * Google OAuth2 로그인 성공 시 Spring Security가 자동 호출하는 서비스.
 *
 * 흐름:
 * 1. 사용자가 /oauth2/authorization/google 접근 → 구글 로그인 화면
 * 2. 구글 인증 완료 → Spring이 이 클래스의 loadUser() 호출
 * 3. 구글에서 받은 유저 정보(email, name, picture, sub)를 추출
 * 4. MemberService.findOrCreateMember()로 회원가입 or 로그인 처리
 * 5. PrincipalDetails(인증 객체)를 반환 → SecurityConfig의 successHandler로 전달
 */
@Service
class CustomOAuth2UserService(
    private val memberService: MemberService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        // 구글 API에서 유저 정보 조회
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.attributes

        // 구글 유저 정보 추출
        val providerId = attributes["sub"] as String       // 구글 고유 ID
        val email = attributes["email"] as String
        val name = attributes["name"] as String
        val profileImageUrl = attributes["picture"] as? String

        // 기존 회원이면 로그인(lastLogin 갱신), 신규면 회원가입(DB 저장)
        val member = memberService.findOrCreateMember(
            email = email,
            name = name,
            profileImageUrl = profileImageUrl,
            providerId = providerId
        )

        // Spring Security 인증 객체로 감싸서 반환
        return PrincipalDetails(member, attributes)
    }
}
