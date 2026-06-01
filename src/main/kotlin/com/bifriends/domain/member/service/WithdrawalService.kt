package com.bifriends.domain.member.service

import com.bifriends.domain.chat.repository.ChatMessageRepository
import com.bifriends.domain.chat.repository.ChatSessionRepository
import com.bifriends.domain.home.repository.RewardHistoryRepository
import com.bifriends.domain.home.repository.TodoRepository
import com.bifriends.domain.home.repository.UserStatsRepository
import com.bifriends.domain.learning.repository.UserKoreanProgressRepository
import com.bifriends.domain.learning.repository.UserMathProgressRepository
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.repository.MemberInterestRepository
import com.bifriends.domain.onboarding.repository.MemberItemRepository
import com.bifriends.domain.report.repository.WeeklyReportRepository
import com.bifriends.domain.shop.repository.MemberShopItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 탈퇴 서비스 (RPT-11)
 *
 * FK 제약 순서에 따라 자식 테이블부터 삭제 후 member를 삭제한다.
 *
 * 삭제 순서:
 *   1. chat_messages        (chat_sessions FK)
 *   2. chat_sessions        (members FK)
 *   3. todos                (members FK)
 *   4. reward_history       (members FK)
 *   5. user_stats           (members FK)
 *   6. user_math_progress   (members FK)
 *   7. user_korean_progress (members FK)
 *   8. member_interests     (members FK)
 *   9. member_items         (members FK)
 *  10. member_shop_items    (members FK)
 *  11. weekly_reports       (members FK)
 *  12. members              (최종 삭제)
 */
@Service
class WithdrawalService(
    private val memberRepository: MemberRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val todoRepository: TodoRepository,
    private val rewardHistoryRepository: RewardHistoryRepository,
    private val userStatsRepository: UserStatsRepository,
    private val userMathProgressRepository: UserMathProgressRepository,
    private val userKoreanProgressRepository: UserKoreanProgressRepository,
    private val memberInterestRepository: MemberInterestRepository,
    private val memberItemRepository: MemberItemRepository,
    private val memberShopItemRepository: MemberShopItemRepository,
    private val weeklyReportRepository: WeeklyReportRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun withdraw(memberId: Long) {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        log.info("[Withdrawal] 탈퇴 처리 시작 — memberId={}, email={}", memberId, member.email)

        chatMessageRepository.deleteAllByMemberId(memberId)
        chatSessionRepository.deleteAllByMemberId(memberId)
        todoRepository.deleteAllByMemberId(memberId)
        rewardHistoryRepository.deleteAllByMemberId(memberId)
        userStatsRepository.deleteByMemberId(memberId)
        userMathProgressRepository.deleteAllByMemberId(memberId)
        userKoreanProgressRepository.deleteAllByMemberId(memberId)
        memberInterestRepository.deleteAllByMemberId(memberId)
        memberItemRepository.deleteAllByMemberId(memberId)
        memberShopItemRepository.deleteAllByMemberId(memberId)
        weeklyReportRepository.deleteAllByMemberId(memberId)
        memberRepository.delete(member)

        log.info("[Withdrawal] 탈퇴 처리 완료 — memberId={}", memberId)
    }
}
