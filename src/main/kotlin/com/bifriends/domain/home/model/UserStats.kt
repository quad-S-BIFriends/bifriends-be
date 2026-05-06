package com.bifriends.domain.home.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사용자 성장 통계 (레벨 · 풀 · 연속 출석)
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ totalPoolEarned  : 누적 획득 풀 → 절대 감소하지 않음        │
 * │                    레벨 계산의 기준이 된다                   │
 * │ availablePool    : 현재 사용 가능한 풀 (상점 소비 시 감소)    │
 * │ level            : totalPoolEarned 에서 파생, 자동 갱신     │
 * └─────────────────────────────────────────────────────────┘
 *
 * 도메인 로직은 엔티티 내부 메서드로 캡슐화하여 외부에서 필드를 직접 변경하지 않는다.
 */
@Entity
@Table(
    name = "user_stats",
    indexes = [Index(name = "idx_user_stats_member_id", columnList = "member_id")]
)
class UserStats(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    val member: Member,

    /** 현재 레벨 — totalPoolEarned 변경 시 자동 갱신됨 */
    @Column(nullable = false)
    var level: Int = 1,

    /** 누적 획득 풀 (레벨 계산 기준, 감소하지 않음) */
    @Column(nullable = false)
    var totalPoolEarned: Int = 0,

    /** 현재 사용 가능한 풀 (상점 소비 시 감소) */
    @Column(nullable = false)
    var availablePool: Int = 0,

    /** 연속 접속 일수 */
    @Column(nullable = false)
    var streakDays: Int = 0,

    /** 마지막 출석 날짜 (KST 기준 LocalDate) */
    @Column
    var lastLoginDate: LocalDate? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    // ── 풀 획득 ────────────────────────────────────────────────────────────

    /**
     * 풀을 [amount]만큼 획득한다.
     * totalPoolEarned 와 availablePool 을 동시에 증가시키고 레벨업을 체크한다.
     *
     * @return 레벨업이 발생했으면 true
     */
    fun earnPool(amount: Int): Boolean {
        require(amount > 0) { "획득 풀은 1 이상이어야 합니다. amount=$amount" }

        totalPoolEarned += amount
        availablePool += amount

        val newLevel = LevelPolicy.calculateLevel(totalPoolEarned)
        val leveledUp = newLevel > level
        level = newLevel

        updatedAt = LocalDateTime.now()
        return leveledUp
    }

    // ── 풀 소비 ────────────────────────────────────────────────────────────

    /**
     * 상점 구매 등으로 풀 [amount]를 소비한다.
     * availablePool 만 감소하며 totalPoolEarned / level 은 변하지 않는다.
     *
     * @throws IllegalStateException 보유 풀이 부족한 경우
     */
    fun spendPool(amount: Int) {
        require(amount > 0) { "소비 풀은 1 이상이어야 합니다. amount=$amount" }
        check(availablePool >= amount) {
            "풀이 부족합니다. 현재 availablePool=$availablePool, 필요=$amount"
        }
        availablePool -= amount
        updatedAt = LocalDateTime.now()
    }

    // ── 출석 처리 ──────────────────────────────────────────────────────────

    /**
     * 오늘([today]) 출석을 기록하고, 지급해야 할 풀 양을 반환한다.
     *
     * - 이미 오늘 출석한 경우 → null 반환 (멱등성 보장)
     * - 연속 출석(어제 접속) → streakDays += 1
     * - 컴백(2일 이상 공백) / 첫 로그인 → streakDays = 1
     *
     * ⚠️  이 메서드는 streak 및 lastLoginDate 만 변경한다.
     *     실제 풀 지급은 서비스에서 earnPool() 을 별도로 호출해야 한다.
     *
     * @return 지급할 출석 보상 풀 양, 이미 처리된 경우 null
     */
    fun recordAttendance(today: LocalDate): Int? {
        if (lastLoginDate == today) return null  // 오늘 이미 출석했으면 출석 보상 없음

        val yesterday = today.minusDays(1)
        //어제 출석했으면 streak 유지, 아니면 1로 초기화
        streakDays = if (lastLoginDate == yesterday) streakDays + 1 else 1
        //마지막 출석일 갱신
        lastLoginDate = today
        updatedAt = LocalDateTime.now()

        return RewardPolicy.attendancePool(streakDays)
    }

    // ── 레벨 진행도 조회 ───────────────────────────────────────────────────

    /** 현재 레벨 내에서 진행한 풀 (0부터 시작) */
    fun currentLevelProgress(): Int = LevelPolicy.progressInCurrentLevel(totalPoolEarned)

    /** 다음 레벨까지 필요한 잔여 풀 */
    fun poolNeededForNextLevel(): Int = LevelPolicy.poolNeededForNextLevel(totalPoolEarned)

    /** 현재 레벨 → 다음 레벨 기준 총 필요 풀 */
    fun totalPoolForCurrentLevelUp(): Int = LevelPolicy.poolRequiredForLevelUp(level)
}
