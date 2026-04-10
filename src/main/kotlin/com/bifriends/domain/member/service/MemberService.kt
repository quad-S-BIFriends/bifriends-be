package com.bifriends.domain.member.service

import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository
) {

    /**
     * [회원가입 / 로그인 핵심 로직]
     * - providerId(구글 고유 ID)로 기존 회원 조회
     * - 있으면 → 로그인 처리 (lastLoginAt 갱신)
     * - 없으면 → 회원가입 처리 (새 Member 엔티티 저장)
     */
    @Transactional
    fun findOrCreateMember(email: String, name: String, profileImageUrl: String?, providerId: String): Member {
        return memberRepository.findByProviderId(providerId)
            .map { existingMember ->
                // 기존 회원 → 로그인: 마지막 로그인 시간 갱신
                existingMember.updateLastLogin()
                existingMember
            }
            .orElseGet {
                // 신규 회원 → 회원가입: DB에 저장
                memberRepository.save(
                    Member(
                        email = email,
                        name = name,
                        profileImageUrl = profileImageUrl,
                        providerId = providerId
                    )
                )
            }
    }

    fun findById(id: Long): Member {
        return memberRepository.findById(id)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$id") }
    }
}
