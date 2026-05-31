package com.bifriends.domain.learning.dto

import com.bifriends.domain.learning.model.StepStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

// ───────────────────────────────────────────────────────────────
// 4-1. 로드맵 조회
// ───────────────────────────────────────────────────────────────

data class RoadmapResponse(
    val grade: Int,
    val lastStepId: Long?,
    val steps: List<StepSummaryResponse>,
)

data class StepSummaryResponse(
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

data class StepContentResponse(
    val stepId: Long,
    val stepTitle: String,
    val concept: String,
    val grade: Int,
    val cycles: List<JsonNode>,   // answer/explanation 제거된 사이클 목록
)

// ───────────────────────────────────────────────────────────────
// 4-3. 진도 조회
// ───────────────────────────────────────────────────────────────

data class ProgressResponse(
    val lastStepId: Long?,
    val totalSteps: Int,
    val completedSteps: Int,
    val progress: List<StepProgressItem>,
)

data class StepProgressItem(
    val stepId: Long,
    val isStepCompleted: Boolean,
    val completedCycles: List<Int>,
    val lastAccessedAt: LocalDateTime?,
)

// ───────────────────────────────────────────────────────────────
// 4-4. 답안 검증
// ───────────────────────────────────────────────────────────────

data class ValidateAnswerRequest(
    val answer: JsonNode,   // String 또는 {numerator, denominator} 객체 모두 JsonNode로 수신
)

data class ValidateAnswerResponse(
    val correct: Boolean,
    val explanation: JsonNode? = null,
)

// ───────────────────────────────────────────────────────────────
// 4-5. 사이클 완료 처리
// ───────────────────────────────────────────────────────────────

data class CompleteCycleResponse(
    val stepId: Long,
    val cycleNumber: Int,
    val completedCycles: List<Int>,
    val isStepCompleted: Boolean,
)

// ───────────────────────────────────────────────────────────────
// 4-6. 스텝 완료 처리
// ───────────────────────────────────────────────────────────────

data class CompleteStepResponse(
    val stepId: Long,
    val isStepCompleted: Boolean,
    val nextStepId: Long?,
    val nextStepStatus: StepStatus?,
)

// ───────────────────────────────────────────────────────────────
// Leo 연동 — 수학 concept 목록 (LRN_13)
// ───────────────────────────────────────────────────────────────

data class MathConceptListResponse(
    val grade: Int,
    val concepts: List<MathConceptItem>,
)

data class MathConceptItem(
    val concept: String,
    val stepId: Long,
    val stepNumber: Int,
    val stepTitle: String,
)

// ───────────────────────────────────────────────────────────────
// Leo 연동 — concept별 lesson 상태 (LRN_14/15/16)
// ───────────────────────────────────────────────────────────────

/**
 * [lessonStatus]
 * - AVAILABLE / IN_PROGRESS / COMPLETED : 해당 lesson으로 이동 가능 (LRN_15)
 * - LOCKED : currentAvailableStepId로 이동 (LRN_16)
 * - NOT_FOUND : concept이 콘텐츠 DB에 없음
 */
data class MathLessonStatusResponse(
    val concept: String,
    val lessonStatus: LessonStatus,
    val stepId: Long?,           // 매칭된 스텝 id (NOT_FOUND면 null)
    val stepTitle: String?,
    // LOCKED일 때 현재 진입 가능한 스텝 정보 (LRN_16)
    val currentAvailableStepId: Long?,
    val currentAvailableStepTitle: String?,
)

enum class LessonStatus {
    AVAILABLE,      // 진입 가능 (progress 행 없음, 잠금 아님)
    IN_PROGRESS,    // 진행 중
    COMPLETED,      // 완료
    LOCKED,         // 잠김 (이전 스텝 미완료)
    NOT_FOUND,      // concept이 현재 학년 커리큘럼에 없음
}

// ───────────────────────────────────────────────────────────────
// Leo 연동 — 수학 스텝 전체 목록 + 상태 (InternalServicePaths)
// ───────────────────────────────────────────────────────────────

data class MathStepsResponse(
    val grade: Int,
    val totalSteps: Int,
    val completedSteps: Int,
    val steps: List<MathStepStatusItem>,
)

data class MathStepStatusItem(
    val stepId: Long,
    val stepNumber: Int,
    val stepTitle: String,
    val concept: String,
    val status: StepStatus,
    val completedCycles: List<Int>,
)
