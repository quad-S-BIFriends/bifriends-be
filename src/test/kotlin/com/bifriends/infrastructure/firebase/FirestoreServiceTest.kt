package com.bifriends.infrastructure.firebase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FirestoreService")
class FirestoreServiceTest {

    private val service = FirestoreService("(default)")

    @Test
    fun `인덱스 오류 메시지를 감지한다`() {
        val error = RuntimeException("FAILED_PRECONDITION: The query requires an index")
        assertThat(invokeIsIndexOrQueryError(error)).isTrue()
    }

    @Test
    fun `일반 오류는 인덱스 오류로 보지 않는다`() {
        assertThat(invokeIsIndexOrQueryError(IllegalStateException("permission denied"))).isFalse()
    }

    @Test
    fun `learnedExpressions 필드를 문자열 리스트로 파싱한다`() {
        assertThat(service.parseLearnedExpressionsField(listOf("기뻐", "속상해")))
            .containsExactly("기뻐", "속상해")
        assertThat(service.parseLearnedExpressionsField(emptyList<String>())).isEmpty()
        assertThat(service.parseLearnedExpressionsField(null)).isEmpty()
    }

    private fun invokeIsIndexOrQueryError(e: Exception): Boolean {
        val method = FirestoreService::class.java.getDeclaredMethod(
            "isIndexOrQueryError",
            Exception::class.java,
        )
        method.isAccessible = true
        return method.invoke(service, e) as Boolean
    }
}
