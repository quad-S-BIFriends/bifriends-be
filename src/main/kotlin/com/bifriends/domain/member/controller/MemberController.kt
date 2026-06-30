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
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val withdrawalService: WithdrawalService,
    private val parentService: ParentService,
) {

    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<MemberProfileResponse> {
        return ResponseEntity.ok(memberService.getProfile(memberId))
    }

    /** HOM-10-01~04 — 설정 화면 저장 (이름·학년·관심사 한 번에) */
    @PatchMapping("/me/settings")
    fun updateSettings(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: MemberSettingsRequest,
    ): ResponseEntity<MemberSettingsResponse> {
        return ResponseEntity.ok(memberService.updateSettings(memberId, request))
    }

    @PatchMapping("/me/representative-item")
    fun updateRepresentativeItem(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: RepresentativeItemRequest,
    ): ResponseEntity<RepresentativeItemResponse> {
        return ResponseEntity.ok(memberService.updateRepresentativeItem(memberId, request.itemType!!))
    }

    /** RPT-11 — 회원 탈퇴 (부모 모드에서만 가능 — 부모 PIN 확인 필요) */
    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: WithdrawRequest,
    ): ResponseEntity<Void> {
        val verified = parentService.verifyParentPassword(memberId, VerifyParentPasswordRequest(request.parentPassword))
        if (!verified.verified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        withdrawalService.withdraw(memberId)
        return ResponseEntity.noContent().build()
    }

}
