package com.bifriends.domain.learning.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.learning.dto.*
import com.bifriends.domain.learning.model.StepStatus
import com.bifriends.domain.learning.model.UserMathProgress
import com.bifriends.domain.learning.repository.MathStepRepository
import com.bifriends.domain.learning.repository.UserMathProgressRepository
import com.bifriends.domain.learning.util.LearningCycleContentSanitizer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class StudyMathService(
    private val memberRepository: MemberRepository,
    private val mathStepRepository: MathStepRepository,
    private val userMathProgressRepository: UserMathProgressRepository,
) {

    // ───────────────────────────────────────────────────────────
    // 4-1. 로드맵 조회
    // ───────────────────────────────────────────────────────────

    fun getRoadmap(memberId: Long): RoadmapResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다. 온보딩을 먼저 완료해 주세요.")

        val steps = mathStepRepository.findByGradeOrderByStepNumber(grade)
        val progressMap = userMathProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.mathStep.id }

        // lastStepId: last_accessed_at 최신 미완료 → 없으면 마지막 완료 스텝 → 없으면 첫 스텝
        val lastStepId = progressMap.values
            .filter { !it.isStepCompleted && it.lastAccessedAt != null }
            .maxByOrNull { it.lastAccessedAt!! }?.mathStep?.id
            ?: progressMap.values
                .filter { it.isStepCompleted }
                .maxByOrNull { it.lastAccessedAt ?: LocalDateTime.MIN }?.mathStep?.id
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

            StepSummaryResponse(
                stepId = step.id,
                stepNumber = step.stepNumber,
                stepTitle = step.stepTitle,
                concept = step.concept,
                status = status,
                completedCycles = progress?.completedCycles?.sorted() ?: emptyList(),
            )
        }

        return RoadmapResponse(grade = grade, lastStepId = lastStepId, steps = stepResponses)
    }

    // ───────────────────────────────────────────────────────────
    // 4-2. 스텝 콘텐츠 조회 (answer/explanation 제거)
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun getStepContent(memberId: Long, stepId: Long): StepContentResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val step = mathStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        // LRN_MATH_06: 스텝 진입 시점에 lastAccessedAt 갱신 → 로드맵 "마지막 위치" 복원에 사용
        val progress = findOrCreateProgress(member, step)
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        val cyclesNode = step.contentJson.get("cycles")
            ?: throw IllegalStateException("content_json에 cycles 필드가 없습니다. stepId=$stepId")

        val sanitizedCycles = (cyclesNode as ArrayNode).map { cycle ->
            LearningCycleContentSanitizer.sanitizeMathCycle(cycle)
        }

        return StepContentResponse(
            stepId = step.id,
            stepTitle = step.stepTitle,
            concept = step.concept,
            grade = step.grade,
            cycles = sanitizedCycles,
        )
    }

    // ───────────────────────────────────────────────────────────
    // 4-3. 진도 조회
    // ───────────────────────────────────────────────────────────

    fun getProgress(memberId: Long): ProgressResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = mathStepRepository.findByGradeOrderByStepNumber(grade)
        val progressList = userMathProgressRepository.findAllByMemberIdAndGrade(memberId, grade)
        val progressMap = progressList.associateBy { it.mathStep.id }

        val lastStepId = progressList
            .filter { it.lastAccessedAt != null }
            .maxByOrNull { it.lastAccessedAt!! }?.mathStep?.id

        return ProgressResponse(
            lastStepId = lastStepId,
            totalSteps = steps.size,
            completedSteps = progressList.count { it.isStepCompleted },
            progress = progressList.map { p ->
                StepProgressItem(
                    stepId = p.mathStep.id,
                    isStepCompleted = p.isStepCompleted,
                    completedCycles = p.completedCycles.sorted(),
                    lastAccessedAt = p.lastAccessedAt,
                )
            },
        )
    }

    // ───────────────────────────────────────────────────────────
    // 4-4. 답안 검증
    // ───────────────────────────────────────────────────────────

    fun validateAnswer(
        memberId: Long,
        stepId: Long,
        cycleNumber: Int,
        questionIndex: Int,
        request: ValidateAnswerRequest,
    ): ValidateAnswerResponse {
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val step = mathStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val question = extractQuestion(step.contentJson, cycleNumber, questionIndex)

        val correctAnswer = question.get("answer")
            ?: throw IllegalStateException("answer 필드가 없습니다. stepId=$stepId, cycle=$cycleNumber, q=$questionIndex")

        val isCorrect = compareAnswers(correctAnswer, request.answer)

        return if (isCorrect) {
            ValidateAnswerResponse(correct = true, explanation = question.get("explanation"))
        } else {
            ValidateAnswerResponse(correct = false)
        }
    }

    private fun extractQuestion(contentJson: JsonNode, cycleNumber: Int, questionIndex: Int): JsonNode {
        val cycles = contentJson.get("cycles")
            ?: throw IllegalStateException("cycles 필드 없음")
        val cycle = cycles.get(cycleNumber - 1)
            ?: throw IllegalArgumentException("cycleNumber=$cycleNumber 범위 초과")
        val questions = cycle.get("questions")
            ?: throw IllegalStateException("questions 필드 없음 (cycleNumber=$cycleNumber)")
        return questions.get(questionIndex)
            ?: throw IllegalArgumentException("questionIndex=$questionIndex 범위 초과")
    }

    /**
     * 정답 비교:
     * - 텍스트(choice/short_answer): 문자열 동등 비교 (trim)
     * - 분수(grade 6): numerator, denominator 각각 비교
     */
    private fun compareAnswers(correct: JsonNode, submitted: JsonNode): Boolean {
        return if (correct.isObject && correct.has("numerator")) {
            // 분수 타입
            correct.get("numerator")?.asInt() == submitted.get("numerator")?.asInt() &&
                correct.get("denominator")?.asInt() == submitted.get("denominator")?.asInt()
        } else {
            // 텍스트 타입
            correct.asText().trim() == submitted.asText().trim()
        }
    }

    // ───────────────────────────────────────────────────────────
    // 4-5. 사이클 완료 처리
    // ───────────────────────────────────────────────────────────

    @Transactional
    fun completeCycle(memberId: Long, stepId: Long, cycleNumber: Int): CompleteCycleResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
        val step = mathStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val progress = findOrCreateProgress(member, step)

        // MutableSet이므로 중복 추가는 자동으로 무시됨 (멱등)
        progress.completedCycles.add(cycleNumber)
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        return CompleteCycleResponse(
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
    fun completeStep(memberId: Long, stepId: Long): CompleteStepResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
        val step = mathStepRepository.findById(stepId)
            .orElseThrow { IllegalArgumentException("스텝을 찾을 수 없습니다. id=$stepId") }

        val progress = findOrCreateProgress(member, step)
        progress.isStepCompleted = true
        progress.lastAccessedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        // 다음 스텝 조회 (같은 학년 내 step_number + 1)
        val nextStep = mathStepRepository.findByGradeAndStepNumber(step.grade, step.stepNumber + 1)
        val nextStepStatus = nextStep?.let { StepStatus.AVAILABLE }

        return CompleteStepResponse(
            stepId = stepId,
            isStepCompleted = true,
            nextStepId = nextStep?.id,
            nextStepStatus = nextStepStatus,
        )
    }

    // ───────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ───────────────────────────────────────────────────────────

    private fun findOrCreateProgress(
        member: com.bifriends.domain.member.model.Member,
        step: com.bifriends.domain.learning.model.MathStep,
    ): UserMathProgress {
        return userMathProgressRepository.findByMemberIdAndMathStepId(member.id, step.id)
            ?: userMathProgressRepository.save(
                UserMathProgress(member = member, mathStep = step)
            )
    }

    // ───────────────────────────────────────────────────────────
    // Leo 연동 — 수학 concept 목록 (LRN_13)
    // ───────────────────────────────────────────────────────────

    fun getMathConcepts(memberId: Long): MathConceptListResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = mathStepRepository.findByGradeOrderByStepNumber(grade)

        return MathConceptListResponse(
            grade = grade,
            concepts = steps.map { step ->
                MathConceptItem(
                    concept = step.concept,
                    stepId = step.id,
                    stepNumber = step.stepNumber,
                    stepTitle = step.stepTitle,
                )
            },
        )
    }

    // ───────────────────────────────────────────────────────────
    // Leo 연동 — 수학 스텝 전체 목록 + 상태
    // ───────────────────────────────────────────────────────────

    fun getMathSteps(memberId: Long): MathStepsResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = mathStepRepository.findByGradeOrderByStepNumber(grade)
        val progressMap = userMathProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.mathStep.id }

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
            MathStepStatusItem(
                stepId = step.id,
                stepNumber = step.stepNumber,
                stepTitle = step.stepTitle,
                concept = step.concept,
                status = status,
                completedCycles = progress?.completedCycles?.sorted() ?: emptyList(),
            )
        }

        return MathStepsResponse(
            grade = grade,
            totalSteps = steps.size,
            completedSteps = stepItems.count { it.status == StepStatus.COMPLETED },
            steps = stepItems,
        )
    }

    // ───────────────────────────────────────────────────────────
    // Leo 연동 — concept별 lesson 상태 (LRN_14 / 15 / 16)
    // ───────────────────────────────────────────────────────────

    fun getMathLessonStatus(memberId: Long, concept: String): MathLessonStatusResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val grade = member.grade
            ?: throw IllegalStateException("학년 정보가 없습니다.")

        val steps = mathStepRepository.findByGradeOrderByStepNumber(grade)
        val progressMap = userMathProgressRepository
            .findAllByMemberIdAndGrade(memberId, grade)
            .associateBy { it.mathStep.id }

        // concept 매칭 (완전 일치 우선, 없으면 포함 검색)
        val matchedIndex = steps.indexOfFirst { it.concept == concept }
            .takeIf { it >= 0 }
            ?: steps.indexOfFirst { it.concept.contains(concept, ignoreCase = true) }
                .takeIf { it >= 0 }
            ?: -1

        if (matchedIndex < 0) {
            return MathLessonStatusResponse(
                concept = concept,
                lessonStatus = LessonStatus.NOT_FOUND,
                stepId = null,
                stepTitle = null,
                currentAvailableStepId = null,
                currentAvailableStepTitle = null,
            )
        }

        val matchedStep = steps[matchedIndex]
        val progress = progressMap[matchedStep.id]
        val prevStep = if (matchedIndex == 0) null else steps[matchedIndex - 1]
        val prevCompleted = prevStep == null || progressMap[prevStep.id]?.isStepCompleted == true

        val lessonStatus = when {
            progress?.isStepCompleted == true -> LessonStatus.COMPLETED
            progress != null -> LessonStatus.IN_PROGRESS
            prevCompleted -> LessonStatus.AVAILABLE
            else -> LessonStatus.LOCKED
        }

        // LOCKED일 때 현재 진입 가능한 스텝 탐색 (LRN_16)
        val currentAvailableStep = if (lessonStatus == LessonStatus.LOCKED) {
            steps.mapIndexed { index, step ->
                val p = progressMap[step.id]
                val prev = if (index == 0) null else steps[index - 1]
                val prevDone = prev == null || progressMap[prev.id]?.isStepCompleted == true
                val status = when {
                    p?.isStepCompleted == true -> LessonStatus.COMPLETED
                    p != null -> LessonStatus.IN_PROGRESS
                    prevDone -> LessonStatus.AVAILABLE
                    else -> LessonStatus.LOCKED
                }
                step to status
            }.firstOrNull { (_, s) -> s == LessonStatus.IN_PROGRESS || s == LessonStatus.AVAILABLE }?.first
        } else null

        return MathLessonStatusResponse(
            concept = concept,
            lessonStatus = lessonStatus,
            stepId = matchedStep.id,
            stepTitle = matchedStep.stepTitle,
            currentAvailableStepId = currentAvailableStep?.id,
            currentAvailableStepTitle = currentAvailableStep?.stepTitle,
        )
    }
}
