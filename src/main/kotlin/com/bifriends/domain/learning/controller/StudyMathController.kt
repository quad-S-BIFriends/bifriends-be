package com.bifriends.domain.learning.controller

import com.bifriends.domain.learning.dto.*
import com.bifriends.domain.learning.service.StudyMathService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/learning/math")
class StudyMathController(
    private val studyMathService: StudyMathService,
) {

    /** 4-1. 로드맵 조회 */
    @GetMapping("/roadmap")
    fun getRoadmap(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<RoadmapResponse> {
        return ResponseEntity.ok(studyMathService.getRoadmap(memberId))
    }

    /** 4-2. 스텝 콘텐츠 조회 */
    @GetMapping("/steps/{stepId}/content")
    fun getStepContent(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
    ): ResponseEntity<StepContentResponse> {
        return ResponseEntity.ok(studyMathService.getStepContent(memberId, stepId))
    }

    /** 4-3. 진도 조회 */
    @GetMapping("/progress")
    fun getProgress(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<ProgressResponse> {
        return ResponseEntity.ok(studyMathService.getProgress(memberId))
    }

    /** 4-4. 답안 검증 */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate")
    fun validateAnswer(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
        @PathVariable questionIndex: Int,
        @RequestBody request: ValidateAnswerRequest,
    ): ResponseEntity<ValidateAnswerResponse> {
        return ResponseEntity.ok(
            studyMathService.validateAnswer(memberId, stepId, cycleNumber, questionIndex, request)
        )
    }

    /** 4-5. 사이클 완료 처리 */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/complete")
    fun completeCycle(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
    ): ResponseEntity<CompleteCycleResponse> {
        return ResponseEntity.ok(studyMathService.completeCycle(memberId, stepId, cycleNumber))
    }

    /** 4-6. 스텝 완료 처리 */
    @PostMapping("/steps/{stepId}/complete")
    fun completeStep(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
    ): ResponseEntity<CompleteStepResponse> {
        return ResponseEntity.ok(studyMathService.completeStep(memberId, stepId))
    }

    // ───────────────────────────────────────────────────────────────
    // Leo 연동 API (LRN_13 / LRN_14·15·16)
    // ───────────────────────────────────────────────────────────────

    /** Leo 연동 — 수학 스텝 전체 목록 + 상태 (Leo 전용, JWT 없음) */
    @GetMapping("/steps")
    fun getMathSteps(
        @RequestParam memberId: Long,
    ): ResponseEntity<MathStepsResponse> {
        return ResponseEntity.ok(studyMathService.getMathSteps(memberId))
    }

    /** LRN_13 — 학년별 수학 concept 목록 (Leo 전용, JWT 없음) */
    @GetMapping("/concepts")
    fun getMathConcepts(
        @RequestParam memberId: Long,
    ): ResponseEntity<MathConceptListResponse> {
        return ResponseEntity.ok(studyMathService.getMathConcepts(memberId))
    }

    /** LRN_14/15/16 — concept별 lesson 상태 조회 (Leo 전용, JWT 없음) */
    @GetMapping("/concepts/lesson-status")
    fun getMathLessonStatus(
        @RequestParam memberId: Long,
        @RequestParam concept: String,
    ): ResponseEntity<MathLessonStatusResponse> {
        return ResponseEntity.ok(studyMathService.getMathLessonStatus(memberId, concept))
    }

}
