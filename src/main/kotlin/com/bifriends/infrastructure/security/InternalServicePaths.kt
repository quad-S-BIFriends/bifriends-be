package com.bifriends.infrastructure.security

import org.springframework.http.HttpMethod

/**
 * AI 서비스(bifriends-ai)가 Docker 내부망에서 호출하는 BE API 경로.
 * 명세 확정 후 컨트롤러와 함께 맞춘다.
 */
object InternalServicePaths {

    const val ROLE = "INTERNAL_SERVICE"

    fun securityRules(): List<Pair<Array<HttpMethod>, String>> = listOf(
        arrayOf(HttpMethod.GET) to "/api/v1/members/*/profile",
        arrayOf(HttpMethod.GET) to "/api/v1/learning/math/steps",
        arrayOf(HttpMethod.GET) to "/api/v1/learning/korean/steps",
        arrayOf(HttpMethod.GET) to "/api/v1/members/*/learning-progress",
        arrayOf(HttpMethod.GET) to "/api/v1/chat/sessions/*/messages",
        arrayOf(HttpMethod.GET) to "/api/v1/chat/messages",
        arrayOf(HttpMethod.GET) to "/api/v1/report/learning-summary",
        // Leo 연동 — 수학 concept 조회 (LRN_13 / LRN_14·15·16)
        arrayOf(HttpMethod.GET) to "/api/v1/learning/math/concepts",
        arrayOf(HttpMethod.GET) to "/api/v1/learning/math/concepts/lesson-status",
        // Leo 연동 — 국어 현재 lesson 조회 (LRN_32·33)
        arrayOf(HttpMethod.GET) to "/api/v1/learning/korean/lessons/current",
        // Leo 연동 — Agent 할 일 CRUD
        arrayOf(HttpMethod.POST) to "/api/v1/todos",
        arrayOf(HttpMethod.PATCH) to "/api/v1/todos/*",
        arrayOf(HttpMethod.DELETE) to "/api/v1/todos/*",
        // AI → BE 주간 안전 신호 콜백
        arrayOf(HttpMethod.POST) to "/api/v1/weekly-safety-report",
        // AI → BE 주간 성장 리포트 콜백
        arrayOf(HttpMethod.POST) to "/api/v1/weekly-report",
        arrayOf(HttpMethod.PATCH) to "/api/v1/chat/sessions/*",
    )

    fun matches(method: String, path: String): Boolean =
        securityRules().any { (methods, pattern) ->
            methods.any { it.matches(method) } && antMatch(pattern, path)
        }

    private fun HttpMethod.matches(requestMethod: String): Boolean =
        name().equals(requestMethod, ignoreCase = true)

    private fun antMatch(pattern: String, path: String): Boolean {
        val regex = pattern
            .replace("**", "§§")
            .replace("*", "[^/]+")
            .replace("§§", ".*")
            .let { "^$it$" }
        return path.matches(Regex(regex))
    }
}
