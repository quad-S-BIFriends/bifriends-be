package com.bifriends.infrastructure.ai.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * BE 스케줄러 → AI 주간 안전 보고서 배치 트리거 요청 (1인당 1건)
 *
 * AI는 [memberId]의 [weekStart] ~ [weekEnd] 채팅을 분석해
 * POST /api/v1/weekly-safety-report 로 결과를 콜백한다.
 */
data class AiBatchWeeklySafetyRequest(
    /** 분석 대상 회원 ID */
    @JsonProperty("member_id")
    val memberId: Long,

    /** 분석 주간 시작일 (월요일, KST) */
    @JsonProperty("week_start")
    val weekStart: LocalDate,

    /** 분석 주간 종료일 (금요일, KST) */
    @JsonProperty("week_end")
    val weekEnd: LocalDate,
)

/** AI 배치 트리거 응답 */
data class AiBatchWeeklySafetyResponse(
    /** 배치 접수 여부 */
    val accepted: Boolean = true,
    /** AI 측 처리 메시지 (선택) */
    val message: String? = null,
)
