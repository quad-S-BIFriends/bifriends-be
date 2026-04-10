package com.bifriends.domain.member.controller

import com.bifriends.infrastructure.security.JwtProvider
import com.bifriends.infrastructure.security.PrincipalDetails
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class OAuthController(
    private val jwtProvider: JwtProvider
) {

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
                profileImageUrl = member.profileImageUrl
            )
        )
    }

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val email: String,
        val name: String,
        val profileImageUrl: String?
    )
}
