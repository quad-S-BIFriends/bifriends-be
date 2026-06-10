package com.bifriends.domain.member.controller

import com.bifriends.domain.member.dto.MemberProfileResponse
import com.bifriends.domain.member.dto.MemberSettingsRequest
import com.bifriends.domain.member.dto.MemberSettingsResponse
import com.bifriends.domain.member.dto.RepresentativeItemRequest
import com.bifriends.domain.member.dto.RepresentativeItemResponse
import com.bifriends.domain.member.dto.WithdrawRequest
import com.bifriends.domain.member.service.MemberService
import com.bifriends.domain.member.service.WithdrawalService
import com.bifriends.domain.parent.dto.VerifyParentPasswordRequest
import com.bifriends.domain.parent.service.ParentService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val withdrawalService: WithdrawalService,
    private val parentService: ParentService,
    private val jwtProvider: JwtProvider,
) {

    @GetMapping("/me")
    fun getMyProfile(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<MemberProfileResponse> {
        return ResponseEntity.ok(memberService.getProfile(extractMemberId(token)))
    }

    /** HOM-10-01~04 — 설정 화면 저장 (이름·학년·관심사 한 번에) */
    @PatchMapping("/me/settings")
    fun updateSettings(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: MemberSettingsRequest,
    ): ResponseEntity<MemberSettingsResponse> {
        return ResponseEntity.ok(memberService.updateSettings(extractMemberId(token), request))
    }

    @PatchMapping("/me/representative-item")
    fun updateRepresentativeItem(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: RepresentativeItemRequest,
    ): ResponseEntity<RepresentativeItemResponse> {
        return ResponseEntity.ok(memberService.updateRepresentativeItem(extractMemberId(token), request.itemType!!))
    }

    /** RPT-11 — 회원 탈퇴 (부모 모드에서만 가능 — 부모 PIN 확인 필요) */
    @DeleteMapping("/me")
    fun withdraw(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: WithdrawRequest,
    ): ResponseEntity<Void> {
        val memberId = extractMemberId(token)
        val verified = parentService.verifyParentPassword(memberId, VerifyParentPasswordRequest(request.parentPassword))
        if (!verified.verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        withdrawalService.withdraw(memberId)
        return ResponseEntity.noContent().build()
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
