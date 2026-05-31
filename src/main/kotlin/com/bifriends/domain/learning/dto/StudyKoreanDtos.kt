package com.bifriends.domain.learning.dto

import com.bifriends.domain.learning.model.StepStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

// ───────────────────────────────────────────────────────────────
// 4-1. 로드맵 조회
// ───────────────────────────────────────────────────────────────

data class KoreanRoadmapResponse(
    val grade: Int,
    val lastStepId: Long?,
    val steps: List<KoreanStepSummaryResponse>,
)

data class KoreanStepSummaryResponse(
    val stepId: Long,
    val stepNumber: Int,
    val stepTitle: String,
    val concept: String,
    val status: StepStatus,
    val completedCycles: List<Int>,
)

// ───────────────────────────────────────────────────────────────
// 4-2. 스텝 콘텐츠 조회
// ───────────────────────────────────────────────────────────────

data class KoreanStepContentResponse(
    val stepId: Long,
    val stepTitle: String,
    val concept: String,
    val grade: Int,
    val passage: JsonNode,      // { title, text, image } — 전체 응답에 포함
    val cycles: List<JsonNode>, // answer 제거된 사이클 목록
)

// ───────────────────────────────────────────────────────────────
// 4-3. 진도 조회
// ───────────────────────────────────────────────────────────────

data class KoreanProgressResponse(
    val lastStepId: Long?,
    val totalSteps: Int,
    val completedSteps: Int,
    val progress: List<KoreanStepProgressItem>,
)

data class KoreanStepProgressItem(
    val stepId: Long,
    val isStepCompleted: Boolean,
    val completedCycles: List<Int>,
    val lastAccessedAt: LocalDateTime?,
)

// ───────────────────────────────────────────────────────────────
// 4-4. 답안 검증 (Cycle 2~5 — word_card인 Cycle 1은 불필요)
// ───────────────────────────────────────────────────────────────

data class KoreanValidateAnswerRequest(
    val answer: String,   // 국어는 텍스트 답안만 존재
)

data class KoreanValidateAnswerResponse(
    val correct: Boolean,
    val explanation: String? = null,
)

// ───────────────────────────────────────────────────────────────
// 4-5. 사이클 완료 처리
// ───────────────────────────────────────────────────────────────

data class KoreanCompleteCycleResponse(
    val stepId: Long,
    val cycleNumber: Int,
    val completedCycles: List<Int>,
    val isStepCompleted: Boolean,
)

// ───────────────────────────────────────────────────────────────
// 4-6. 스텝 완료 처리
// ───────────────────────────────────────────────────────────────

data class KoreanCompleteStepResponse(
    val stepId: Long,
    val isStepCompleted: Boolean,
    val nextStepId: Long?,
    val nextStepStatus: StepStatus?,
)

// ───────────────────────────────────────────────────────────────
// Leo 연동 — 현재 진입 가능한 국어 lesson (LRN_33)
// ───────────────────────────────────────────────────────────────

/**
 * Leo가 "국어 공부 도움" 의도를 분류한 뒤 BE에서 조회.
 * 우선순위: IN_PROGRESS → AVAILABLE → 없으면 첫 스텝
 */
data class KoreanCurrentLessonResponse(
    val stepId: Long,
    val stepTitle: String,
    val concept: String,
    val lessonStatus: StepStatus,
)

// ───────────────────────────────────────────────────────────────
// Leo 연동 — 국어 스텝 전체 목록 + 상태 (InternalServicePaths)
// ───────────────────────────────────────────────────────────────

data class KoreanStepsResponse(
    val grade: Int,
    val totalSteps: Int,
    val completedSteps: Int,
    val steps: List<KoreanStepStatusItem>,
)

data class KoreanStepStatusItem(
    val stepId: Long,
    val stepNumber: Int,
    val stepTitle: String,
    val concept: String,
    val status: StepStatus,
    val completedCycles: List<Int>,
)
