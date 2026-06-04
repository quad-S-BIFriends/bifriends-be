package com.bifriends.domain.learning.service

import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.learning.dto.*
import com.bifriends.domain.learning.model.KoreanStep
import com.bifriends.domain.learning.model.LearningSubject
import com.bifriends.domain.learning.model.StepStatus
import com.bifriends.domain.learning.model.UserKoreanProgress
import com.bifriends.domain.learning.repository.KoreanStepRepository
import com.bifriends.domain.learning.repository.UserKoreanProgressRepository
import com.bifriends.domain.learning.util.LearningCycleContentSanitizer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class StudyKoreanService(
    private val memberRepository: MemberRepository,
    private val koreanStepRepository: KoreanStepRepository,
    private val userKoreanProgressRepository: UserKoreanProgressRepository,
    private val learningAttemptService: LearningAttemptService,
) {

    // ───────────────────────────────────────────────────────────
    // 4-1. 로드맵 조회
    // ───────────────────────────────────────────────────────────

    fun getRoadmap(memberId: Long): KoreanRoadmapResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다. 온보딩을 먼저 완료해 주세요.")

        val steps = koreanStepRepository.findByGradeOrderByStepNumber(grade)
        val progressMap = userKoreanProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.koreanStep.id }

        val lastStepId = progressMap.values
            .filter { !it.isStepCompleted && it.lastAccessedAt != null }
            .maxByOrNull { it.lastAccessedAt!! }?.koreanStep?.id
            ?: progressMap.values
                .filter { it.isStepCompleted }
                .maxByOrNull { it.lastAccessedAt ?: LocalDateTime.MIN }?.koreanStep?.id
            ?: steps.firstOrNull()?.id

        val stepResponses = steps.mapIndexed { index, step ->
            val progress = progressMap[step.id]
            val prevStep = if (index == 0) null else steps[index - 1]
            val prevCompleted = prevStep == null || progressMap[prevStep.id]?.isStepCompleted == true

            val status = when {
                progress?.isStepCompleted == true -> StepStatus.COMPLETED
                progress != null -> StepStatus.IN_PROGRESS
                prevCompleted -> StepStatus.AVAILABLE
                else -> StepStatus.LOCKED
            }

            KoreanStepSummaryResponse(
                stepId = step.id,
                stepNumber = step.stepNumber,
                stepTitle = step.stepTitle,
                concept = step.concept,
                status = status,
                completedCycles = progress?.completedCycles?.sorted() ?: emptyList(),
            )
        }

        return KoreanRoadmapResponse(grade = grade, lastStepId = lastStepId, steps = stepResponses)
    }

    // ───────────────────────────────────────────────────────────
    // 4-2. 스텝 콘텐츠 조회
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun getStepContent(memberId: Long, stepId: Long): KoreanStepContentResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val step = koreanStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        // LRN_KOR_05: 스텝 진입 시점에 lastAccessedAt 갱신
        val progress = findOrCreateProgress(member, step)
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        val passage = step.contentJson.get("passage")
            ?: throw IllegalStateException("content_json에 passage 필드가 없습니다. stepId=$stepId")

        val cyclesNode = step.contentJson.get("cycles")
            ?: throw IllegalStateException("content_json에 cycles 필드가 없습니다. stepId=$stepId")

        val sanitizedCycles = (cyclesNode as ArrayNode).map { cycle ->
            LearningCycleContentSanitizer.sanitizeKoreanCycle(cycle)
        }

        return KoreanStepContentResponse(
            stepId = step.id,
            stepTitle = step.stepTitle,
            concept = step.concept,
            grade = step.grade,
            passage = passage,
            cycles = sanitizedCycles,
        )
    }

    // ───────────────────────────────────────────────────────────
    // 4-3. 진도 조회
    // ───────────────────────────────────────────────────────────

    fun getProgress(memberId: Long): KoreanProgressResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = koreanStepRepository.findByGradeOrderByStepNumber(grade)
        val progressList = userKoreanProgressRepository.findAllByMemberIdAndGrade(memberId, grade)

        val lastStepId = progressList
            .filter { it.lastAccessedAt != null }
            .maxByOrNull { it.lastAccessedAt!! }?.koreanStep?.id

        return KoreanProgressResponse(
            lastStepId = lastStepId,
            totalSteps = steps.size,
            completedSteps = progressList.count { it.isStepCompleted },
            progress = progressList.map { p ->
                KoreanStepProgressItem(
                    stepId = p.koreanStep.id,
                    isStepCompleted = p.isStepCompleted,
                    completedCycles = p.completedCycles.sorted(),
                    lastAccessedAt = p.lastAccessedAt,
                )
            },
        )
    }

    // ───────────────────────────────────────────────────────────
    // 4-4. 답안 검증 (Cycle 2~5)
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun validateAnswer(
        memberId: Long,
        stepId: Long,
        cycleNumber: Int,
        questionIndex: Int,
        request: KoreanValidateAnswerRequest,
    ): KoreanValidateAnswerResponse {
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val step = koreanStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val question = extractQuestion(step.contentJson, cycleNumber, questionIndex)

        val correctAnswer = question.get("answer")?.asText()
            ?: throw IllegalStateException("answer 필드가 없습니다. stepId=$stepId, cycle=$cycleNumber, q=$questionIndex")

        val isCorrect = correctAnswer.trim() == request.answer.trim()

        learningAttemptService.recordValidation(
            memberId = memberId,
            subject = LearningSubject.KOREAN,
            concept = step.concept,
            stepId = stepId,
            cycleNumber = cycleNumber,
            questionIndex = questionIndex,
            isCorrect = isCorrect,
            hintsUsed = request.hintsUsed,
        )

        return KoreanValidateAnswerResponse(correct = isCorrect)
    }

    private fun extractQuestion(contentJson: JsonNode, cycleNumber: Int, questionIndex: Int): JsonNode {
        val cycles = contentJson.get("cycles")
            ?: throw IllegalStateException("cycles 필드 없음")
        val cycle = cycles.get(cycleNumber - 1)
            ?: throw IllegalArgumentException("cycleNumber=$cycleNumber 범위 초과")
        val questions = cycle.get("questions")
            ?: throw IllegalStateException("questions 필드 없음 (cycleNumber=$cycleNumber — word_card 사이클에는 validate 불필요)")
        return questions.get(questionIndex)
            ?: throw IllegalArgumentException("questionIndex=$questionIndex 범위 초과")
    }

    // ───────────────────────────────────────────────────────────
    // 4-5. 사이클 완료 처리
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun completeCycle(memberId: Long, stepId: Long, cycleNumber: Int): KoreanCompleteCycleResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
        val step = koreanStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val progress = findOrCreateProgress(member, step)
        progress.completedCycles.add(cycleNumber) // MutableSet → 중복 무시 (멱등)
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        return KoreanCompleteCycleResponse(
            stepId = stepId,
            cycleNumber = cycleNumber,
            completedCycles = progress.completedCycles.sorted(),
            isStepCompleted = progress.isStepCompleted,
        )
    }

    // ───────────────────────────────────────────────────────────
    // 4-6. 스텝 완료 처리
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun completeStep(memberId: Long, stepId: Long): KoreanCompleteStepResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
        val step = koreanStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val progress = findOrCreateProgress(member, step)
        progress.isStepCompleted = true
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        val nextStep = koreanStepRepository.findByGradeAndStepNumber(step.grade, step.stepNumber + 1)

        return KoreanCompleteStepResponse(
            stepId = stepId,
            isStepCompleted = true,
            nextStepId = nextStep?.id,
            nextStepStatus = nextStep?.let { StepStatus.AVAILABLE },
        )
    }

    // ───────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ───────────────────────────────────────────────────────────

    private fun findOrCreateProgress(member: Member, step: KoreanStep): UserKoreanProgress {
        return userKoreanProgressRepository.findByMemberIdAndKoreanStepId(member.id, step.id)
            ?: userKoreanProgressRepository.save(
                UserKoreanProgress(member = member, koreanStep = step)
            )
    }

    // ───────────────────────────────────────────────────────────
    // Leo 연동 — 현재 진입 가능한 국어 lesson (LRN_33)
    // 우선순위: IN_PROGRESS → AVAILABLE → 첫 스텝(AVAILABLE)
    // ───────────────────────────────────────────────────────────

    fun getCurrentKoreanLesson(memberId: Long): KoreanCurrentLessonResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = koreanStepRepository.findByGradeOrderByStepNumber(grade)
        if (steps.isEmpty()) throw IllegalStateException("해당 학년의 국어 콘텐츠가 없습니다. grade=$grade")

        val progressMap = userKoreanProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.koreanStep.id }

        // 스텝별 상태 계산
        val stepWithStatus = steps.mapIndexed { index, step ->
            val progress = progressMap[step.id]
            val prev = if (index == 0) null else steps[index - 1]
            val prevCompleted = prev == null || progressMap[prev.id]?.isStepCompleted == true

            val status = when {
                progress?.isStepCompleted == true -> StepStatus.COMPLETED
                progress != null -> StepStatus.IN_PROGRESS
                prevCompleted -> StepStatus.AVAILABLE
                else -> StepStatus.LOCKED
            }
            step to status
        }

        // 우선순위: IN_PROGRESS → AVAILABLE → 첫 스텝
        val (targetStep, targetStatus) = stepWithStatus
            .firstOrNull { (_, s) -> s == StepStatus.IN_PROGRESS }
            ?: stepWithStatus.firstOrNull { (_, s) -> s == StepStatus.AVAILABLE }
            ?: (steps.first() to StepStatus.AVAILABLE)

        return KoreanCurrentLessonResponse(
            stepId = targetStep.id,
            stepTitle = targetStep.stepTitle,
            concept = targetStep.concept,
            lessonStatus = targetStatus,
        )
    }

    // ───────────────────────────────────────────────────────────
    // Leo 연동 — 국어 스텝 전체 목록 + 상태
    // ───────────────────────────────────────────────────────────

    fun getKoreanSteps(memberId: Long): KoreanStepsResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = koreanStepRepository.findByGradeOrderByStepNumber(grade)
        val progressMap = userKoreanProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.koreanStep.id }

        val stepItems = steps.mapIndexed { index, step ->
            val progress = progressMap[step.id]
            val prevStep = if (index == 0) null else steps[index - 1]
            val prevCompleted = prevStep == null || progressMap[prevStep.id]?.isStepCompleted == true

            val status = when {
                progress?.isStepCompleted == true -> StepStatus.COMPLETED
                progress != null -> StepStatus.IN_PROGRESS
                prevCompleted -> StepStatus.AVAILABLE
                else -> StepStatus.LOCKED
            }

            KoreanStepStatusItem(
                stepId = step.id,
                stepNumber = step.stepNumber,
                stepTitle = step.stepTitle,
                concept = step.concept,
                status = status,
                completedCycles = progress?.completedCycles?.sorted() ?: emptyList(),
            )
        }

        return KoreanStepsResponse(
            grade = grade,
            totalSteps = steps.size,
            completedSteps = stepItems.count { it.status == StepStatus.COMPLETED },
            steps = stepItems,
        )
    }
}
