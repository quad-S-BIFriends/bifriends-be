package com.bifriends.domain.parent.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.parent.dto.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ParentService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * RPT-01 — 부모 모드 PIN 확인.
     * 일치하면 true, 불일치하면 false 반환.
     * 보안상 틀린 이유는 노출하지 않는다.
     */
    fun verifyParentPassword(memberId: Long, request: VerifyParentPasswordRequest): VerifyParentPasswordResponse {
        val member = findMember(memberId)

        val storedHash = member.parentPassword
            ?: return VerifyParentPasswordResponse(verified = false)

        val verified = passwordEncoder.matches(request.password, storedHash)
        return VerifyParentPasswordResponse(verified = verified)
    }

    /**
     * RPT-12 — 부모 비밀번호 변경.
     * 현재 비밀번호 확인 → 새 비밀번호 저장.
     */
    @Transactional
    fun changeParentPassword(memberId: Long, request: ChangeParentPasswordRequest): ChangeParentPasswordResponse {
        require(request.newPassword == request.newPasswordConfirm) {
            "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다."
        }

        val member = findMember(memberId)

        val storedHash = member.parentPassword
            ?: throw IllegalStateException("부모 비밀번호가 설정되지 않았습니다.")

        require(passwordEncoder.matches(request.currentPassword, storedHash)) {
            "현재 비밀번호가 올바르지 않습니다."
        }

        member.parentPassword = passwordEncoder.encode(request.newPassword)
        return ChangeParentPasswordResponse(changed = true)
    }

    /**
     * RPT-01-01 — 비밀번호 찾기 (MVP: 별도 인증 없이 새 비밀번호로 재설정)
     */
    @Transactional
    fun resetParentPassword(memberId: Long, request: ResetParentPasswordRequest): ResetParentPasswordResponse {
        require(request.newPassword == request.newPasswordConfirm) {
            "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다."
        }

        val member = findMember(memberId)
        member.parentPassword = passwordEncoder.encode(request.newPassword)
        return ResetParentPasswordResponse(reset = true)
    }

    private fun findMember(memberId: Long) =
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
}
