package com.bifriends.domain.study.dto

import com.bifriends.domain.study.model.StepStatus
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
