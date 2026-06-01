package com.bifriends.domain.parent.controller

import com.bifriends.domain.parent.dto.*
import com.bifriends.domain.parent.service.ParentService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/parent")
class ParentController(
    private val parentService: ParentService,
    private val jwtProvider: JwtProvider,
) {

    /** RPT-01 — 부모 모드 PIN 확인 */
    @PostMapping("/verify")
    fun verifyParentPassword(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: VerifyParentPasswordRequest,
    ): ResponseEntity<VerifyParentPasswordResponse> {
        return ResponseEntity.ok(parentService.verifyParentPassword(extractMemberId(token), request))
    }

    /** RPT-12 — 부모 비밀번호 변경 (현재 비밀번호 확인 필요) */
    @PatchMapping("/password")
    fun changeParentPassword(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: ChangeParentPasswordRequest,
    ): ResponseEntity<ChangeParentPasswordResponse> {
        return ResponseEntity.ok(parentService.changeParentPassword(extractMemberId(token), request))
    }

    /** RPT-01-01 — 비밀번호 찾기 (MVP: 별도 인증 없이 재설정) */
    @PostMapping("/reset-password")
    fun resetParentPassword(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: ResetParentPasswordRequest,
    ): ResponseEntity<ResetParentPasswordResponse> {
        return ResponseEntity.ok(parentService.resetParentPassword(extractMemberId(token), request))
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
