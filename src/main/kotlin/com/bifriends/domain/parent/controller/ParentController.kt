package com.bifriends.domain.parent.controller

import com.bifriends.domain.parent.dto.*
import com.bifriends.domain.parent.service.ParentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/parent")
class ParentController(
    private val parentService: ParentService,
) {

    /** RPT-01 — 부모 모드 PIN 확인 */
    @PostMapping("/verify")
    fun verifyParentPassword(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: VerifyParentPasswordRequest,
    ): ResponseEntity<VerifyParentPasswordResponse> {
        return ResponseEntity.ok(parentService.verifyParentPassword(memberId, request))
    }

    /** RPT-12 — 부모 비밀번호 변경 (현재 비밀번호 확인 필요) */
    @PatchMapping("/password")
    fun changeParentPassword(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: ChangeParentPasswordRequest,
    ): ResponseEntity<ChangeParentPasswordResponse> {
        return ResponseEntity.ok(parentService.changeParentPassword(memberId, request))
    }

    /** RPT-01-01 — 비밀번호 찾기 (MVP: 별도 인증 없이 재설정) */
    @PostMapping("/reset-password")
    fun resetParentPassword(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: ResetParentPasswordRequest,
    ): ResponseEntity<ResetParentPasswordResponse> {
        return ResponseEntity.ok(parentService.resetParentPassword(memberId, request))
    }

}
