package com.bifriends.domain.parent.controller

import com.bifriends.domain.parent.dto.*
import com.bifriends.domain.parent.service.ParentService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 부모 모드 API.
 * RPT-01: PIN 확인으로 부모 모드 진입
 * RPT-12: 부모 비밀번호 변경
 */
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

    /** RPT-12 — 부모 비밀번호 변경 */
    @PatchMapping("/password")
    fun changeParentPassword(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: ChangeParentPasswordRequest,
    ): ResponseEntity<ChangeParentPasswordResponse> {
        return ResponseEntity.ok(parentService.changeParentPassword(extractMemberId(token), request))
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
