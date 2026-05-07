package com.bifriends.domain.home.dto

import com.bifriends.domain.home.model.GreetingType

// UserStatsResponse, AttendanceResult 는 같은 패키지(dto) 내 UserStatsDtos.kt 에 정의되어 있어 import 불필요.
// TodoResponse 는 같은 패키지(dto) 내 TodoDtos.kt 에 정의되어 있어 import 불필요.

// ── 홈 화면 전체 응답 ──────────────────────────────────────────────────────

/**
 * GET /api/v1/home 응답
 *
 * 클라이언트가 홈 화면을 한 번의 요청으로 렌더링할 수 있도록
 * 멤버 정보 + 인사 메시지 + 레벨/풀 통계 + 출석 결과 + 오늘의 할 일을 묶어 반환한다.
 */
data class HomeResponse(
    /** 회원 요약 (닉네임) */
    val member: MemberSummary,

    /** 인사 메시지 */
    val greeting: GreetingResponse,

    /** 레벨 · 풀 · streak 통계 (출석 처리 후 최신값) */
    val stats: UserStatsResponse,

    /** 오늘 출석 처리 결과 */
    val attendance: AttendanceResult,

    /** 오늘의 할 일 목록 */
    val todos: List<TodoResponse>,
)

// ── 회원 요약 ──────────────────────────────────────────────────────────────

data class MemberSummary(
    /** 닉네임 (미설정 시 이름으로 대체) */
    val nickname: String,
)

// ── 인사 메시지 ────────────────────────────────────────────────────────────

data class GreetingResponse(
    /**
     * 인사 유형
     * FIRST_LOGIN / COMEBACK_SHORT / COMEBACK_LONG / STREAK
     */
    val type: GreetingType,

    /** 현재 연속 출석 일수 (출석 처리 후 값) */
    val streakDays: Int,

    /** 닉네임이 치환된 최종 인사 메시지 */
    val message: String,
)
