package com.bifriends.domain.member.controller

import com.bifriends.domain.member.dto.MemberProfileSummaryResponse
import com.bifriends.domain.member.service.MemberService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * AI 서비스 전용 회원 조회 API.
 * [com.bifriends.infrastructure.security.InternalServiceAuthenticationFilter] + ROLE_INTERNAL_SERVICE 필요.
 * JWT 없음 — X-Internal-Service 헤더만 사용.
 */
@RestController
@RequestMapping("/api/v1/members")
class InternalMemberController(
    private val memberService: MemberService,
) {

    @GetMapping("/{memberId}/profile")
    fun getMemberProfileSummary(
        @PathVariable memberId: Long,
    ): ResponseEntity<MemberProfileSummaryResponse> {
        val profile = memberService.getProfile(memberId)
        return ResponseEntity.ok(
            MemberProfileSummaryResponse(
                memberId = profile.id,
                nickname = profile.nickname,
                grade = profile.grade,
                interests = profile.interests,
            )
        )
    }
}
