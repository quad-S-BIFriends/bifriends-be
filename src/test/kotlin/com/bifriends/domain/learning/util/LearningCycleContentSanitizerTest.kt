package com.bifriends.domain.learning.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LearningCycleContentSanitizer")
class LearningCycleContentSanitizerTest {

    private val mapper = ObjectMapper()

    @Test
    fun `국어 legacy 키를 cycle_number cycle_type으로 정규화한다`() {
        val cycle = mapper.readTree(
            """{"cycle":2,"type":"fact_check","questions":[{"answer":"O"}]}"""
        ) as ObjectNode

        val sanitized = LearningCycleContentSanitizer.sanitizeKoreanCycle(cycle) as ObjectNode

        assertThat(sanitized.has("cycle")).isFalse()
        assertThat(sanitized.has("type")).isFalse()
        assertThat(sanitized.get("cycle_number").asInt()).isEqualTo(2)
        assertThat(sanitized.get("cycle_type").asText()).isEqualTo("fact_check")
        assertThat(sanitized.get("questions")[0].has("answer")).isFalse()
        assertThat(sanitized.get("questions")[0].get("questionIndex").asInt()).isZero()
    }

    @Test
    fun `수학 canonical 키는 유지하고 answer explanation을 제거한다`() {
        val cycle = mapper.readTree(
            """{"cycle_number":1,"cycle_type":"choice","questions":[{"answer":"3","explanation":"ok"}]}"""
        ) as ObjectNode

        val sanitized = LearningCycleContentSanitizer.sanitizeMathCycle(cycle) as ObjectNode

        assertThat(sanitized.get("cycle_number").asInt()).isEqualTo(1)
        assertThat(sanitized.get("cycle_type").asText()).isEqualTo("choice")
        assertThat(sanitized.get("questions")[0].has("answer")).isFalse()
        assertThat(sanitized.get("questions")[0].has("explanation")).isFalse()
    }
}
