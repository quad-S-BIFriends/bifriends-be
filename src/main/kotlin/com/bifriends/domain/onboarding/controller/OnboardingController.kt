package com.bifriends.domain.onboarding.controller

import com.bifriends.domain.onboarding.dto.*
import com.bifriends.domain.onboarding.service.OnboardingService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController(
    private val onboardingService: OnboardingService,
) {

    /** ONB-02 — 약관 동의 */
    @PostMapping("/terms")
    fun agreeToTerms(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: TermsConsentRequest,
    ): ResponseEntity<TermsConsentResponse> {
        return ResponseEntity.ok(onboardingService.agreeToTerms(memberId, request))
    }

    /** ONB-02-01 — 부모 비밀번호 설정 */
    @PostMapping("/parent-password")
    fun setParentPassword(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: SetParentPasswordRequest,
    ): ResponseEntity<SetParentPasswordResponse> {
        return ResponseEntity.ok(onboardingService.setParentPassword(memberId, request))
    }

    /** ONB-04/06 — 이름·학년 입력 */
    @PatchMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: ProfileRequest,
    ): ResponseEntity<ProfileResponse> {
        return ResponseEntity.ok(onboardingService.updateProfile(memberId, request))
    }

    /** ONB-07 — 관심사 선택 */
    @PutMapping("/interests")
    fun saveInterests(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: InterestsRequest,
    ): ResponseEntity<InterestsResponse> {
        return ResponseEntity.ok(onboardingService.saveInterests(memberId, request))
    }

    /** ONB-08 — 선물 아이템 선택 */
    @PostMapping("/gift")
    fun saveGift(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: GiftRequest,
    ): ResponseEntity<GiftResponse> {
        return ResponseEntity.ok(onboardingService.saveGift(memberId, request))
    }

    /** ONB-10 — 권한(알림·마이크) 설정 */
    @PatchMapping("/permissions")
    fun updatePermissions(
        @AuthenticationPrincipal memberId: Long,
        @Valid @RequestBody request: PermissionsRequest,
    ): ResponseEntity<PermissionsResponse> {
        return ResponseEntity.ok(onboardingService.updatePermissions(memberId, request))
    }

    /** ONB-11 — 온보딩 완료 */
    @PostMapping("/complete")
    fun completeOnboarding(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<OnboardingCompleteResponse> {
        return ResponseEntity.ok(onboardingService.completeOnboarding(memberId))
    }

}
