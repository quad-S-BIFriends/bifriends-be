package com.bifriends.domain.onboarding

import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.dto.*
import com.bifriends.domain.onboarding.model.Interest
import com.bifriends.domain.onboarding.model.ItemType
import com.bifriends.domain.onboarding.model.MemberInterest
import com.bifriends.domain.onboarding.model.MemberItem
import com.bifriends.domain.onboarding.repository.MemberInterestRepository
import com.bifriends.domain.onboarding.repository.MemberItemRepository
import com.bifriends.domain.onboarding.service.OnboardingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("OnboardingService 단위 테스트")
class OnboardingServiceTest {

    @Mock lateinit var memberRepository: MemberRepository
    @Mock lateinit var memberInterestRepository: MemberInterestRepository
    @Mock lateinit var memberItemRepository: MemberItemRepository

    // 실제 BCrypt 사용 (Mock X — 암호화 로직을 실제로 검증)
    private val passwordEncoder = BCryptPasswordEncoder()

    private lateinit var onboardingService: OnboardingService

    private lateinit var testMember: Member

    @BeforeEach
    fun setUp() {
        onboardingService = OnboardingService(
            memberRepository,
            memberInterestRepository,
            memberItemRepository,
            passwordEncoder,
        )
        testMember = Member(
            id = 1L,
            email = "parent@test.com",
            providerId = "google_123",
        )
    }

    private fun givenMemberExists() {
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(testMember))
    }

    // ── ONB-02. 약관 동의 ─────────────────────────────────────────────

    @Nested
    @DisplayName("약관 동의 (ONB-02)")
    inner class AgreeToTerms {

        @Test
        @DisplayName("필수 항목 동의 시 저장 성공")
        fun `약관 동의 성공`() {
            givenMemberExists()
            val request = TermsConsentRequest(
                termsAgreed = true,
                privacyAgreed = true,
                marketingAgreed = false,
            )

            val result = onboardingService.agreeToTerms(1L, request)

            assertThat(result.termsAgreed).isTrue()
            assertThat(result.privacyAgreed).isTrue()
            assertThat(result.marketingAgreed).isFalse()
            assertThat(result.agreedAt).isNotBlank()
            assertThat(testMember.termsAgreed).isTrue()
            assertThat(testMember.termsAgreedAt).isNotNull()
        }

        @Test
        @DisplayName("마케팅 동의 포함 시 저장")
        fun `마케팅 동의 포함`() {
            givenMemberExists()
            val request = TermsConsentRequest(
                termsAgreed = true,
                privacyAgreed = true,
                marketingAgreed = true,
            )

            val result = onboardingService.agreeToTerms(1L, request)

            assertThat(result.marketingAgreed).isTrue()
            assertThat(testMember.marketingAgreed).isTrue()
        }
    }

    // ── ONB-02-01. 부모 비밀번호 설정 ─────────────────────────────────

    @Nested
    @DisplayName("부모 비밀번호 설정 (ONB-02-01)")
    inner class SetParentPassword {

        @Test
        @DisplayName("PIN 일치 시 BCrypt 해시로 저장")
        fun `부모 PIN 설정 성공`() {
            givenMemberExists()
            val request = SetParentPasswordRequest(
                password = "1234",
                passwordConfirm = "1234",
            )

            val result = onboardingService.setParentPassword(1L, request)

            assertThat(result.configured).isTrue()
            assertThat(testMember.parentPassword).isNotNull()
            assertThat(testMember.parentPassword).isNotEqualTo("1234") // 평문 저장 X
            assertThat(passwordEncoder.matches("1234", testMember.parentPassword)).isTrue()
        }

        @Test
        @DisplayName("PIN 불일치 시 예외 발생")
        fun `부모 PIN 불일치 예외`() {
            givenMemberExists()
            val request = SetParentPasswordRequest(
                password = "1234",
                passwordConfirm = "5678",
            )

            assertThatThrownBy { onboardingService.setParentPassword(1L, request) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("일치하지 않습니다")
        }
    }

    // ── ONB-04/06. 프로필 ─────────────────────────────────────────────

    @Nested
    @DisplayName("프로필 입력 (ONB-04/06)")
    inner class UpdateProfile {

        @Test
        @DisplayName("이름만 업데이트")
        fun `이름 업데이트`() {
            givenMemberExists()
            val result = onboardingService.updateProfile(1L, ProfileRequest(nickname = "민준이"))

            assertThat(result.nickname).isEqualTo("민준이")
            assertThat(testMember.nickname).isEqualTo("민준이")
            assertThat(testMember.grade).isNull() // 학년은 변경 없음
        }

        @Test
        @DisplayName("학년만 업데이트")
        fun `학년 업데이트`() {
            givenMemberExists()
            val result = onboardingService.updateProfile(1L, ProfileRequest(grade = 4))

            assertThat(result.grade).isEqualTo(4)
            assertThat(testMember.grade).isEqualTo(4)
        }

        @Test
        @DisplayName("이름·학년 동시 업데이트")
        fun `이름 학년 동시 업데이트`() {
            givenMemberExists()
            val result = onboardingService.updateProfile(1L, ProfileRequest(nickname = "민준이", grade = 4))

            assertThat(result.nickname).isEqualTo("민준이")
            assertThat(result.grade).isEqualTo(4)
        }
    }

    // ── ONB-11. 온보딩 완료 ───────────────────────────────────────────

    @Nested
    @DisplayName("온보딩 완료 (ONB-11)")
    inner class CompleteOnboarding {

        @BeforeEach
        fun setUpComplete() {
            // 완료 조건을 모두 충족한 회원 준비
            testMember.termsAgreed = true
            testMember.privacyAgreed = true
            testMember.parentPassword = passwordEncoder.encode("1234")
            testMember.nickname = "민준이"
            testMember.grade = 4
        }

        @Test
        @DisplayName("모든 조건 충족 시 완료 처리")
        fun `온보딩 완료 성공`() {
            givenMemberExists()

            val result = onboardingService.completeOnboarding(1L)

            assertThat(result.completed).isTrue()
            assertThat(testMember.onboardingCompleted).isTrue()
        }

        @Test
        @DisplayName("약관 미동의 시 예외")
        fun `약관 미동의 예외`() {
            testMember.termsAgreed = false
            givenMemberExists()

            assertThatThrownBy { onboardingService.completeOnboarding(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("약관")
        }

        @Test
        @DisplayName("부모 비밀번호 미설정 시 예외")
        fun `부모 PIN 미설정 예외`() {
            testMember.parentPassword = null
            givenMemberExists()

            assertThatThrownBy { onboardingService.completeOnboarding(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("비밀번호")
        }

        @Test
        @DisplayName("닉네임 미입력 시 예외")
        fun `닉네임 미입력 예외`() {
            testMember.nickname = null
            givenMemberExists()

            assertThatThrownBy { onboardingService.completeOnboarding(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("닉네임")
        }

        @Test
        @DisplayName("학년 미선택 시 예외")
        fun `학년 미선택 예외`() {
            testMember.grade = null
            givenMemberExists()

            assertThatThrownBy { onboardingService.completeOnboarding(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("학년")
        }
    }
}
