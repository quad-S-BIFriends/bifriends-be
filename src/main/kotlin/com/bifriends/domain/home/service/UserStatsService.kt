package com.bifriends.domain.home.service

import com.bifriends.domain.home.dto.AttendanceResult
import com.bifriends.domain.home.dto.RewardResult
import com.bifriends.domain.home.dto.UserStatsResponse
import com.bifriends.domain.home.model.RewardHistory
import com.bifriends.domain.home.model.RewardSource
import com.bifriends.domain.home.model.UserStats
import com.bifriends.domain.home.repository.RewardHistoryRepository
import com.bifriends.domain.home.repository.UserStatsRepository
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class UserStatsService(
    private val memberRepository: MemberRepository,
    private val userStatsRepository: UserStatsRepository,
    private val rewardHistoryRepository: RewardHistoryRepository,
) {

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }

    // ── 조회 ────────────────────────────────────────────────────────────────

    fun getUserStats(memberId: Long): UserStatsResponse {
        val stats = getOrCreate(memberId)
        return UserStatsResponse.from(stats)
    }

    // ── 출석 처리 (홈 진입 시 호출) ─────────────────────────────────────────

    /**
     * 홈 화면 진입 시 출석을 기록하고 보상을 지급한다.
     *
     * - 오늘 이미 처리된 경우 보상 없이 현재 상태만 반환 (멱등)
     * - streak 계산 및 레벨업 처리 포함
     * - 모든 변경은 단일 트랜잭션으로 처리
     */
    @Transactional
    fun recordAttendance(memberId: Long): AttendanceResult {
        val stats = getOrCreate(memberId)
        val today = LocalDate.now(KST)

        // UserStats 도메인 메서드: streak 갱신 + 지급할 풀 양 반환 (이미 처리됐으면 null)
        val rewardAmount = stats.recordAttendance(today)

        if (rewardAmount == null) {
            // 오늘 이미 출석 처리됨 → 보상 없이 현재 상태 반환
            return AttendanceResult(
                isFirstAttendanceToday = false,
                streakDays = stats.streakDays,
                reward = null,
            )
        }

        // 풀 획득 + 레벨업 체크
        val levelBefore = stats.level
        stats.earnPool(rewardAmount)

        // 보상 이력 저장
        rewardHistoryRepository.save(
            RewardHistory(
                member = stats.member,
                source = RewardSource.ATTENDANCE,
                amount = rewardAmount,
            )
        )

        return AttendanceResult(
            isFirstAttendanceToday = true,
            streakDays = stats.streakDays,
            reward = RewardResult(
                earnedPool = rewardAmount,
                availablePool = stats.availablePool,
                totalPoolEarned = stats.totalPoolEarned,
                levelBefore = levelBefore,
                levelAfter = stats.level,
            ),
        )
    }

    // ── 보상 지급 (다른 도메인에서 호출) ────────────────────────────────────

    /**
     * 특정 행동([source])에 대한 풀 보상을 지급한다.
     *
     * - 학습 정답, 할 일 완료, 마음 시나리오 완료 등에서 호출
     * - [refId]: 관련 엔티티 ID (todo.id, question.id 등) — 선택
     *
     * 호출 예시:
     * ```
     * userStatsService.earnReward(memberId, RewardSource.TODO_SINGLE, RewardPolicy.TODO_SINGLE, todoId)
     * ```
     */
    @Transactional
    fun earnReward(
        memberId: Long,
        source: RewardSource,
        amount: Int,
        refId: Long? = null,
    ): RewardResult {
        require(amount > 0) { "보상 풀은 1 이상이어야 합니다." }

        val stats = getOrCreate(memberId)
        val levelBefore = stats.level

        stats.earnPool(amount)

        rewardHistoryRepository.save(
            RewardHistory(
                member = stats.member,
                source = source,
                amount = amount,
                refId = refId,
            )
        )

        return RewardResult(
            earnedPool = amount,
            availablePool = stats.availablePool,
            totalPoolEarned = stats.totalPoolEarned,
            levelBefore = levelBefore,
            levelAfter = stats.level,
        )
    }

    // ── 풀 소비 (상점 구매 등) ───────────────────────────────────────────────

    /**
     * 상점 구매 등으로 풀을 소비한다.
     * availablePool 만 감소하며 레벨은 변하지 않는다.
     */
    @Transactional
    fun spendPool(memberId: Long, amount: Int): UserStatsResponse {
        val stats = getOrCreate(memberId)
        stats.spendPool(amount)
        return UserStatsResponse.from(stats)
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    /**
     * UserStats 조회 또는 신규 생성
     * 온보딩 완료 후 홈 첫 진입 시 자동으로 생성된다.
     */
    @Transactional
    fun getOrCreate(memberId: Long): UserStats {
        return userStatsRepository.findByMemberId(memberId)
            ?: run {
                val member = memberRepository.findById(memberId)
                    .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
                userStatsRepository.save(UserStats(member = member))
            }
    }
}
