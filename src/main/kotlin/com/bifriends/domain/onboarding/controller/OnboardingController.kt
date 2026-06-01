package com.bifriends.domain.onboarding.controller

import com.bifriends.domain.onboarding.dto.*
import com.bifriends.domain.onboarding.service.OnboardingService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController(
    private val onboardingService: OnboardingService,
    private val jwtProvider: JwtProvider,
) {

    /** ONB-02 — 약관 동의 */
    @PostMapping("/terms")
    fun agreeToTerms(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: TermsConsentRequest,
    ): ResponseEntity<TermsConsentResponse> {
        return ResponseEntity.ok(onboardingService.agreeToTerms(extractMemberId(token), request))
    }

    /** ONB-02-01 — 부모 비밀번호 설정 */
    @PostMapping("/parent-password")
    fun setParentPassword(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: SetParentPasswordRequest,
    ): ResponseEntity<SetParentPasswordResponse> {
        return ResponseEntity.ok(onboardingService.setParentPassword(extractMemberId(token), request))
    }

    /** ONB-04/06 — 이름·학년 입력 */
    @PatchMapping("/profile")
    fun updateProfile(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: ProfileRequest,
    ): ResponseEntity<ProfileResponse> {
        return ResponseEntity.ok(onboardingService.updateProfile(extractMemberId(token), request))
    }

    /** ONB-07 — 관심사 선택 */
    @PutMapping("/interests")
    fun saveInterests(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: InterestsRequest,
    ): ResponseEntity<InterestsResponse> {
        return ResponseEntity.ok(onboardingService.saveInterests(extractMemberId(token), request))
    }

    /** ONB-08 — 선물 아이템 선택 */
    @PostMapping("/gift")
    fun saveGift(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: GiftRequest,
    ): ResponseEntity<GiftResponse> {
        return ResponseEntity.ok(onboardingService.saveGift(extractMemberId(token), request))
    }

    /** ONB-10 — 권한(알림·마이크) 설정 */
    @PatchMapping("/permissions")
    fun updatePermissions(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: PermissionsRequest,
    ): ResponseEntity<PermissionsResponse> {
        return ResponseEntity.ok(onboardingService.updatePermissions(extractMemberId(token), request))
    }

    /** ONB-11 — 온보딩 완료 */
    @PostMapping("/complete")
    fun completeOnboarding(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<OnboardingCompleteResponse> {
        return ResponseEntity.ok(onboardingService.completeOnboarding(extractMemberId(token)))
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
