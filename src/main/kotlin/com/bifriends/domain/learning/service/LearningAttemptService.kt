package com.bifriends.domain.learning.service

import com.bifriends.domain.learning.model.LearningAttempt
import com.bifriends.domain.learning.model.LearningSubject
import com.bifriends.domain.learning.repository.LearningAttemptRepository
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 문제별 풀이 시도 기록 (주간 리포트 집계·학습 현황용)
 *
 * - 오답 응답마다 attempts +1
 * - 정답 시 attempts 확정(이번 시도 포함) 후 solved = true, solved_at 설정
 * - hints_used: validate 요청 시 FE가 전달한 힌트 단계 수 (0~3)
 */
@Service
class LearningAttemptService(
    private val learningAttemptRepository: LearningAttemptRepository,
    private val memberRepository: MemberRepository,
) {

    @Transactional
    fun recordValidation(
        memberId: Long,
        subject: LearningSubject,
        concept: String,
        stepId: Long,
        cycleNumber: Int,
        questionIndex: Int,
        isCorrect: Boolean,
        hintsUsed: Int,
    ) {
        require(hintsUsed in 0..3) { "hintsUsed는 0~3 사이여야 합니다." }

        val existing = learningAttemptRepository.findByMemberIdAndStepIdAndCycleNumberAndQuestionIndex(
            memberId, stepId, cycleNumber, questionIndex,
        )

        if (existing != null) {
            if (existing.solved) return
            if (isCorrect) {
                existing.attempts++
                existing.markSolved(hintsUsed)
            } else {
                existing.markWrong()
            }
            return
        }

        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        if (isCorrect) {
            learningAttemptRepository.save(
                LearningAttempt(
                    member = member,
                    subject = subject,
                    concept = concept,
                    stepId = stepId,
                    cycleNumber = cycleNumber,
                    questionIndex = questionIndex,
                    attempts = 1,
                    hintsUsed = hintsUsed,
                    solved = true,
                    solvedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                )
            )
        } else {
            learningAttemptRepository.save(
                LearningAttempt(
                    member = member,
                    subject = subject,
                    concept = concept,
                    stepId = stepId,
                    cycleNumber = cycleNumber,
                    questionIndex = questionIndex,
                    attempts = 1,
                    hintsUsed = 0,
                    solved = false,
                    solvedAt = null,
                )
            )
        }
    }
}
