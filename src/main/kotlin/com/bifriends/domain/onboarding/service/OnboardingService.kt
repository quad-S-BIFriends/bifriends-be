package com.bifriends.domain.onboarding.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.dto.*
import com.bifriends.domain.onboarding.model.MemberInterest
import com.bifriends.domain.onboarding.model.MemberItem
import com.bifriends.domain.onboarding.repository.MemberInterestRepository
import com.bifriends.domain.onboarding.repository.MemberItemRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class OnboardingService(
    private val memberRepository: MemberRepository,
    private val memberInterestRepository: MemberInterestRepository,
    private val memberItemRepository: MemberItemRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val KST = ZoneId.of("Asia/Seoul")

    // ── ONB-02. 약관 동의 ─────────────────────────────────────────────

    @Transactional
    fun agreeToTerms(memberId: Long, request: TermsConsentRequest): TermsConsentResponse {
        val member = findMember(memberId)
        val agreedAt = LocalDateTime.now(KST)

        member.termsAgreed = request.termsAgreed
        member.privacyAgreed = request.privacyAgreed
        member.marketingAgreed = request.marketingAgreed
        member.termsAgreedAt = agreedAt

        return TermsConsentResponse(
            termsAgreed = member.termsAgreed,
            privacyAgreed = member.privacyAgreed,
            marketingAgreed = member.marketingAgreed,
            agreedAt = agreedAt.toString(),
        )
    }

    // ── ONB-02-01. 부모 비밀번호 설정 ─────────────────────────────────

    @Transactional
    fun setParentPassword(memberId: Long, request: SetParentPasswordRequest): SetParentPasswordResponse {
        require(request.password == request.passwordConfirm) {
            "비밀번호와 비밀번호 확인이 일치하지 않습니다."
        }

        val member = findMember(memberId)
        member.parentPassword = passwordEncoder.encode(request.password)

        return SetParentPasswordResponse(configured = true)
    }

    // ── ONB-04/06. 프로필 (이름·학년) ─────────────────────────────────

    @Transactional
    fun updateProfile(memberId: Long, request: ProfileRequest): ProfileResponse {
        val member = findMember(memberId)
        request.nickname?.let { member.nickname = it }
        request.grade?.let { member.grade = it }
        return ProfileResponse(nickname = member.nickname, grade = member.grade)
    }

    // ── ONB-07. 관심사 ────────────────────────────────────────────────

    @Transactional
    fun saveInterests(memberId: Long, request: InterestsRequest): InterestsResponse {
        val member = findMember(memberId)
        memberInterestRepository.deleteAllByMemberId(memberId)
        memberInterestRepository.flush()

        val interests = request.interests.distinct().map { interest ->
            MemberInterest(member = member, interest = interest)
        }
        memberInterestRepository.saveAll(interests)

        return InterestsResponse(interests = request.interests.distinct())
    }

    // ── ONB-08. 선물 아이템 ───────────────────────────────────────────

    @Transactional
    fun saveGift(memberId: Long, request: GiftRequest): GiftResponse {
        val member = findMember(memberId)
        val item = memberItemRepository.save(
            MemberItem(member = member, itemType = request.itemType)
        )
        return GiftResponse(itemType = item.itemType, acquiredAt = item.acquiredAt.toString())
    }

    // ── ONB-10. 권한 설정 ─────────────────────────────────────────────

    @Transactional
    fun updatePermissions(memberId: Long, request: PermissionsRequest): PermissionsResponse {
        val member = findMember(memberId)
        member.notificationEnabled = request.notificationEnabled
        member.microphoneEnabled = request.microphoneEnabled
        return PermissionsResponse(
            notificationEnabled = member.notificationEnabled,
            microphoneEnabled = member.microphoneEnabled,
        )
    }

    // ── ONB-11. 온보딩 완료 ───────────────────────────────────────────

    @Transactional
    fun completeOnboarding(memberId: Long): OnboardingCompleteResponse {
        val member = findMember(memberId)

        require(member.termsAgreed && member.privacyAgreed) {
            "약관 동의가 완료되지 않았습니다."
        }
        require(member.parentPassword != null) {
            "부모 비밀번호 설정이 완료되지 않았습니다."
        }
        require(!member.nickname.isNullOrBlank()) {
            "닉네임(이름)을 입력해야 합니다."
        }
        require(member.grade != null) {
            "학년을 선택해야 합니다."
        }

        member.onboardingCompleted = true
        return OnboardingCompleteResponse(completed = true)
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────

    private fun findMember(memberId: Long) =
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
}
