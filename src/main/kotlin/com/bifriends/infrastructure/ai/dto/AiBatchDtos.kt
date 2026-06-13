package com.bifriends.infrastructure.ai.dto

import com.bifriends.domain.report.dto.LearningSummaryResponse
import com.fasterxml.jackson.annotation.JsonFormat
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val weekStart: LocalDate,

    /** 분석 주간 종료일 (일요일, KST — 직전 완료 주) */
    @JsonProperty("week_end")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val weekEnd: LocalDate,
)

/**
 * BE 스케줄러 → AI 주간 성장 리포트(부모용) 배치 트리거 요청 (1인당 1건)
 *
 * AI는 [memberId]의 [weekStart] ~ [weekEnd] 데이터를 집계·생성 후
 * POST /api/v1/weekly-report 로 결과를 콜백한다.
 */
data class AiBatchWeeklyReportRequest(
    @JsonProperty("member_id")
    val memberId: Long,

    @JsonProperty("week_start")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val weekStart: LocalDate,

    @JsonProperty("week_end")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val weekEnd: LocalDate,

    @JsonProperty("grade")
    val grade: Int?,

    @JsonProperty("learning_summary")
    val learningSummary: LearningSummaryResponse,
)

/** AI 배치 트리거 응답 */
data class AiBatchWeeklySafetyResponse(
    val accepted: Boolean = true,
    val message: String? = null,
)

/** 주간 성장 리포트 배치 트리거 응답 (안전 배치와 동일 형식) */
typealias AiBatchWeeklyReportResponse = AiBatchWeeklySafetyResponse
