package com.bifriends.domain.member.controller

import com.bifriends.domain.member.dto.MemberProfileResponse
import com.bifriends.domain.member.dto.RepresentativeItemRequest
import com.bifriends.domain.member.dto.RepresentativeItemResponse
import com.bifriends.domain.member.service.MemberService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val jwtProvider: JwtProvider
) {

    @GetMapping("/me")
    fun getMyProfile(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<MemberProfileResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(memberService.getProfile(memberId))
    }

    @PatchMapping("/me/representative-item")
    fun updateRepresentativeItem(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: RepresentativeItemRequest
    ): ResponseEntity<RepresentativeItemResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(memberService.updateRepresentativeItem(memberId, request.itemType!!))
    }

    private fun extractMemberId(token: String): Long {
        return jwtProvider.getMemberId(token.removePrefix("Bearer "))
    }
}