package com.bifriends.domain.mind.dto

// ─── POST /api/v1/mind/scenario ──────────────────────────────────────────────

data class MindScenarioRequest(
    val emotion: String? = null,
)

// 응답은 EmotionScenarioResponse 재사용 (동일 구조)

// ─── POST /api/v1/mind/sessions ──────────────────────────────────────────────

data class MindSessionSaveRequest(
    val setId: String,
    val emotion: String,
    val situation: String,
    val learnedExpression: String,
    val isFallback: Boolean = false,
    val steps: MindStepsData,
)

data class MindStepsData(
    val step1: MindStep1Data,
    val step2: MindStep2Data,
    val step3: MindStep3Data,
    val step4: MindStep4Data,
)

data class MindStep1Data(
    val title: String,
    val expression: String,
    val emotion: String,
    val bodySensation: String,
    val situationExample: String,
    val imageUrl: String,
    val nextButtonText: String,
)

data class MindStep2Data(
    val title: String,
    val visualClue: String,
    val question: String,
    val choices: List<MindChoiceData>,
    val imageUrl: String,
    val retryMessage: String,
    val nextButtonText: String,
)

data class MindStep3Data(
    val title: String,
    val comic: List<MindComicCutData>,
    val question: String,
    val choices: List<MindChoiceData>,
    val retryMessage: String,
    val nextButtonText: String,
)

data class MindComicCutData(
    val cut: Int,
    val text: String,
    val imageUrl: String,
)

data class MindStep4Data(
    val title: String,
    val leoIntro: String,
    val question: String,
    val choices: List<MindStep4ChoiceData>,
    val retryMessage: String,
    val successMessage: String,
    val reward: MindRewardData,
    val completeButtonText: String,
)

data class MindChoiceData(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val feedback: String,
)

data class MindStep4ChoiceData(
    val id: String,
    val text: String,
    val type: String,
    val isCorrect: Boolean,
    val feedback: String,
)

data class MindRewardData(val type: String, val amount: Int)

data class MindSessionSaveResponse(val setId: String, val rewardAmount: Int)

// ─── GET /api/v1/mind/sessions ───────────────────────────────────────────────

data class MindSessionSummary(
    val setId: String,
    val emotion: String,
    val learnedExpression: String,
    val completedAt: String,
    val isFallback: Boolean = false,
)

data class MindSessionListResponse(
    val sessions: List<MindSessionSummary>,
    val totalCount: Int,
)

// ─── GET /api/v1/mind/sessions/{sessionId} ───────────────────────────────────

data class MindSessionDetailResponse(
    val setId: String,
    val emotion: String,
    val situation: String,
    val learnedExpression: String,
    val isFallback: Boolean,
    val completedAt: String,
    val steps: MindStepsData,
)
