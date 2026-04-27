package com.bifriends.domain.member.controller

import com.bifriends.domain.member.service.MemberService
import com.bifriends.infrastructure.security.FirebaseTokenVerifier
import com.bifriends.infrastructure.security.JwtProvider
import com.bifriends.infrastructure.security.PrincipalDetails
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class OAuthController(
    private val jwtProvider: JwtProvider,
    private val firebaseTokenVerifier: FirebaseTokenVerifier,
    private val memberService: MemberService
) {

    /**
     * [모바일(Flutter) 구글 로그인]
     * 클라이언트에서 Google Sign-In으로 받은 ID 토큰을 전달하면,
     * 토큰 검증 → 회원가입/로그인 처리 → JWT 발급.
     */
    @PostMapping("/auth/google")
    fun googleLogin(@Valid @RequestBody request: GoogleLoginRequest): ResponseEntity<LoginResponse> {
        val firebaseToken = firebaseTokenVerifier.verifyIdToken(request.idToken)

        val providerId = firebaseToken.uid
        val email = firebaseToken.email ?: throw IllegalArgumentException("이메일 정보가 없습니다.")
        val name = firebaseToken.name ?: email
        val profileImageUrl = firebaseToken.picture

        val member = memberService.findOrCreateMember(
            email = email,
            name = name,
            profileImageUrl = profileImageUrl,
            providerId = providerId
        )

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

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                email = member.email,
                name = member.name,
                profileImageUrl = member.profileImageUrl,
                onboardingCompleted = member.onboardingCompleted
            )
        )
    }

    @GetMapping("/auth/login/success")
    fun loginSuccess(@AuthenticationPrincipal principalDetails: PrincipalDetails): ResponseEntity<LoginResponse> {
        val member = principalDetails.getMember()

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

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                email = member.email,
                name = member.name,
                profileImageUrl = member.profileImageUrl,
                onboardingCompleted = member.onboardingCompleted
            )
        )
    }

    data class GoogleLoginRequest(
        @field:NotBlank(message = "ID 토큰은 필수입니다.")
        val idToken: String
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val email: String,
        val name: String,
        val profileImageUrl: String?,
        val onboardingCompleted: Boolean
    )
}
