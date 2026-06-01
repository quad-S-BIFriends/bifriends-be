package com.bifriends.domain.parent

import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.parent.dto.ChangeParentPasswordRequest
import com.bifriends.domain.parent.dto.VerifyParentPasswordRequest
import com.bifriends.domain.parent.service.ParentService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("ParentService 단위 테스트")
class ParentServiceTest {

    @Mock lateinit var memberRepository: MemberRepository

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var parentService: ParentService
    private lateinit var testMember: Member

    @BeforeEach
    fun setUp() {
        parentService = ParentService(memberRepository, passwordEncoder)
        testMember = Member(
            id = 1L,
            email = "parent@test.com",
            providerId = "google_123",
        ).apply {
            parentPassword = passwordEncoder.encode("1234")
        }
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(testMember))
    }

    // ── RPT-01. 부모 모드 PIN 확인 ─────────────────────────────────────

    @Nested
    @DisplayName("부모 모드 PIN 확인 (RPT-01)")
    inner class VerifyParentPassword {

        @Test
        @DisplayName("올바른 PIN 입력 시 verified=true")
        fun `PIN 일치`() {
            val result = parentService.verifyParentPassword(1L, VerifyParentPasswordRequest("1234"))
            assertThat(result.verified).isTrue()
        }

        @Test
        @DisplayName("틀린 PIN 입력 시 verified=false (400 아님)")
        fun `PIN 불일치`() {
            val result = parentService.verifyParentPassword(1L, VerifyParentPasswordRequest("9999"))
            assertThat(result.verified).isFalse()
        }

        @Test
        @DisplayName("PIN 미설정 회원은 verified=false")
        fun `PIN 미설정`() {
            testMember.parentPassword = null
            val result = parentService.verifyParentPassword(1L, VerifyParentPasswordRequest("1234"))
            assertThat(result.verified).isFalse()
        }
    }

    // ── RPT-12. 부모 비밀번호 변경 ─────────────────────────────────────

    @Nested
    @DisplayName("부모 비밀번호 변경 (RPT-12)")
    inner class ChangeParentPassword {

        @Test
        @DisplayName("현재 PIN 일치 + 새 PIN 일치 시 변경 성공")
        fun `비밀번호 변경 성공`() {
            val request = ChangeParentPasswordRequest(
                currentPassword = "1234",
                newPassword = "5678",
                newPasswordConfirm = "5678",
            )

            val result = parentService.changeParentPassword(1L, request)

            assertThat(result.changed).isTrue()
            assertThat(passwordEncoder.matches("5678", testMember.parentPassword)).isTrue()
            assertThat(passwordEncoder.matches("1234", testMember.parentPassword)).isFalse()
        }

        @Test
        @DisplayName("현재 PIN 불일치 시 예외")
        fun `현재 PIN 불일치 예외`() {
            val request = ChangeParentPasswordRequest(
                currentPassword = "wrong",
                newPassword = "5678",
                newPasswordConfirm = "5678",
            )

            assertThatThrownBy { parentService.changeParentPassword(1L, request) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 비밀번호")
        }

        @Test
        @DisplayName("새 PIN 불일치 시 예외")
        fun `새 PIN 불일치 예외`() {
            val request = ChangeParentPasswordRequest(
                currentPassword = "1234",
                newPassword = "5678",
                newPasswordConfirm = "0000",
            )

            assertThatThrownBy { parentService.changeParentPassword(1L, request) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("새 비밀번호")
        }
    }
}
