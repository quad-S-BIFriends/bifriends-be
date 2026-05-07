package com.bifriends.domain.home.service

import com.bifriends.domain.home.dto.GreetingResponse
import com.bifriends.domain.home.dto.HomeResponse
import com.bifriends.domain.home.dto.MemberSummary
import com.bifriends.domain.home.model.GreetingPolicy
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

/**
 * 홈 화면 조회 서비스
 *
 * GET /api/v1/home 한 번의 요청으로 홈 화면 렌더링에 필요한 모든 데이터를 반환한다.
 *
 * ── 처리 순서 ────────────────────────────────────────────────────────────
 * 1. 출석 처리 전(前) lastAttendanceDate 캡처  →  인사 유형 결정 기준
 * 2. 출석 처리 (recordAttendance)         →  streak 갱신 + 풀 보상 지급
 * 3. 인사 유형 결정 (pre-attendance 기준)
 * 4. 메시지 선택 (post-attendance streakDays 기준으로 STREAK bucket 결정)
 * 5. 최신 stats 조회 (출석 후 레벨/풀 반영)
 * 6. 오늘의 할 일 조회
 * ────────────────────────────────────────────────────────────────────────
 *
 * ── pre/post attendance 분리 이유 ────────────────────────────────────────
 * 인사 유형(first_login / comeback / streak)은 "오늘 오기 전 상태"를 봐야 정확하다.
 * 예) 3일만에 재접속 → recordAttendance 후에는 lastAttendanceDate = today 로 바뀌어
 *     comeback 여부를 알 수 없게 된다.
 *
 * streak bucket(1일 / 2~3일 / 4~6일 / 7+일)은 오늘 출석을 반영한 최신 streakDays 로 결정한다.
 * 예) 어제까지 3일 연속 → 오늘 출석 후 4일 → streak_day_4_6 bucket 에 해당
 * ────────────────────────────────────────────────────────────────────────
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val userStatsService: UserStatsService,
    private val todoService: TodoService,
    private val memberRepository: MemberRepository,
) {

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }

    @Transactional
    fun getHome(memberId: Long): HomeResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val today = LocalDate.now(KST)

        // ── 1. 출석 처리 전 lastAttendanceDate 캡처 ─────────────────────────
        // 같은 트랜잭션 내에서 호출되므로 JPA 1차 캐시에서 반환된다 (추가 쿼리 없음).
        val statsBefore = userStatsService.getOrCreate(memberId)
        val lastAttendanceBefore = statsBefore.lastAttendanceDate  // LocalDate? — 변경 불가능한 값 복사

        // ── 2. 출석 처리 ────────────────────────────────────────────────────
        // streak 갱신 + 보상 지급. 오늘 이미 처리된 경우 멱등 처리 (reward = null).
        val attendanceResult = userStatsService.recordAttendance(memberId)

        // ── 3. 인사 유형 결정 (pre-attendance 기준) ─────────────────────────
        val greetingType = GreetingPolicy.determineType(lastAttendanceBefore, today)

        // ── 4. 메시지 선택 (post-attendance streakDays 기준) ────────────────
        val nickname = member.nickname ?: member.name
        val message = GreetingPolicy.selectMessage(
            type = greetingType,
            streakDays = attendanceResult.streakDays,
            nickname = nickname,
        )

        // ── 5. 최신 stats 조회 ──────────────────────────────────────────────
        // 출석 처리 후 레벨/풀이 반영된 UserStatsResponse 를 반환한다.
        val updatedStats = userStatsService.getUserStats(memberId)

        // ── 6. 오늘의 할 일 조회 ────────────────────────────────────────────
        val todos = todoService.getTodayTodos(memberId)

        return HomeResponse(
            member = MemberSummary(nickname = nickname),
            greeting = GreetingResponse(
                type = greetingType,
                streakDays = attendanceResult.streakDays,
                message = message,
            ),
            stats = updatedStats,
            attendance = attendanceResult,
            todos = todos,
        )
    }
}
