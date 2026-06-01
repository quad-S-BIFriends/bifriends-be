package com.bifriends.domain.parent.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

// ── RPT-01. 부모 모드 PIN 확인 ─────────────────────────────────────────────────

data class VerifyParentPasswordRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String,
)

data class VerifyParentPasswordResponse(
    val verified: Boolean,
)

// ── RPT-12. 부모 비밀번호 변경 ─────────────────────────────────────────────────

data class ChangeParentPasswordRequest(
    @field:NotBlank(message = "현재 비밀번호를 입력해주세요.")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Pattern(regexp = "\\d{4,6}", message = "비밀번호는 4~6자리 숫자여야 합니다.")
    val newPassword: String,

    @field:NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    val newPasswordConfirm: String,
)

data class ChangeParentPasswordResponse(
    val changed: Boolean,
)

// ── RPT-01-01. 비밀번호 찾기 (인증 없이 재설정) ────────────────────────────────

data class ResetParentPasswordRequest(
    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Pattern(regexp = "\\d{4,6}", message = "비밀번호는 4~6자리 숫자여야 합니다.")
    val newPassword: String,

    @field:NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    val newPasswordConfirm: String,
)

data class ResetParentPasswordResponse(
    val reset: Boolean,
)
