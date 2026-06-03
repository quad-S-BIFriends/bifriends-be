package com.bifriends.domain.learning.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * 학습 콘텐츠 조회 API 응답용 사이클 JSON 정규화.
 * DB 시드는 과목별로 키가 다를 수 있으나, API는 항상 [cycle_number], [cycle_type]을 사용한다.
 */
object LearningCycleContentSanitizer {

    fun sanitizeMathCycle(cycle: JsonNode): JsonNode {
        val copy = cycle.deepCopy<ObjectNode>()
        normalizeCycleKeysForResponse(copy)
        sanitizeMathQuestions(copy)
        return copy
    }

    fun sanitizeKoreanCycle(cycle: JsonNode): JsonNode {
        val copy = cycle.deepCopy<ObjectNode>()
        normalizeCycleKeysForResponse(copy)
        sanitizeKoreanQuestions(copy)
        return copy
    }

    /**
     * 국어 시드의 `cycle` / `type`을 API canonical 키 `cycle_number` / `cycle_type`으로 통일한다.
     * 이미 canonical 키가 있으면 legacy 키만 제거한다.
     */
    fun normalizeCycleKeysForResponse(cycle: ObjectNode) {
        when {
            cycle.has("cycle_number") && cycle.has("cycle") -> cycle.remove("cycle")
            !cycle.has("cycle_number") && cycle.has("cycle") -> {
                cycle.set<ObjectNode>("cycle_number", cycle.get("cycle"))
                cycle.remove("cycle")
            }
        }

        when {
            cycle.has("cycle_type") && cycle.has("type") -> cycle.remove("type")
            !cycle.has("cycle_type") && cycle.has("type") -> {
                cycle.set<ObjectNode>("cycle_type", cycle.get("type"))
                cycle.remove("type")
            }
        }
    }

    private fun sanitizeMathQuestions(cycle: ObjectNode) {
        val questions = cycle.get("questions") as? ArrayNode ?: return
        questions.forEachIndexed { index, q ->
            if (q is ObjectNode) {
                q.put("questionIndex", index)
                q.remove("answer")
                q.remove("explanation")
            }
        }
    }

    private fun sanitizeKoreanQuestions(cycle: ObjectNode) {
        val questions = cycle.get("questions") as? ArrayNode ?: return
        questions.forEachIndexed { index, q ->
            if (q is ObjectNode) {
                q.put("questionIndex", index)
                q.remove("answer")
            }
        }
    }
}
