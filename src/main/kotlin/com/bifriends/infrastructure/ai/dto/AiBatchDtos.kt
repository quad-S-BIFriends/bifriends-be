package com.bifriends.infrastructure.ai.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/** BE 스케줄러 → AI 주간 안전 보고서 배치 트리거 요청 */
data class AiBatchWeeklySafetyRequest(
    /** 기준 날짜 (트리거된 금요일 날짜, KST) */
    @JsonProperty("target_date")
    val targetDate: LocalDate,
)

/** AI 배치 트리거 응답 */
data class AiBatchWeeklySafetyResponse(
    /** 배치 접수 여부 */
    val accepted: Boolean = true,
    /** AI 측 처리 메시지 (선택) */
    val message: String? = null,
)
