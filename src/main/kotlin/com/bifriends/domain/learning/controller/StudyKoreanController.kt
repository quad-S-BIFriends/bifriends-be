package com.bifriends.domain.learning.controller

import com.bifriends.domain.learning.dto.*
import com.bifriends.domain.learning.service.StudyKoreanService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/learning/korean")
class StudyKoreanController(
    private val studyKoreanService: StudyKoreanService,
) {

    /** 로드맵 조회 */
    @GetMapping("/roadmap")
    fun getRoadmap(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<KoreanRoadmapResponse> {
        return ResponseEntity.ok(studyKoreanService.getRoadmap(memberId))
    }

    /** 스텝 콘텐츠 조회 (passage 포함, answer 제거) */
    @GetMapping("/steps/{stepId}/content")
    fun getStepContent(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
    ): ResponseEntity<KoreanStepContentResponse> {
        return ResponseEntity.ok(studyKoreanService.getStepContent(memberId, stepId))
    }

    /** 진도 조회 */
    @GetMapping("/progress")
    fun getProgress(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<KoreanProgressResponse> {
        return ResponseEntity.ok(studyKoreanService.getProgress(memberId))
    }

    /**
     * 답안 검증 (Cycle 2~5)
     * Cycle 1 (word_card)은 문제가 없으므로 이 API를 호출하지 않음
     */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate")
    fun validateAnswer(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
        @PathVariable questionIndex: Int,
        @RequestBody request: KoreanValidateAnswerRequest,
    ): ResponseEntity<KoreanValidateAnswerResponse> {
        return ResponseEntity.ok(
            studyKoreanService.validateAnswer(memberId, stepId, cycleNumber, questionIndex, request)
        )
    }

    /** 사이클 완료 처리 */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/complete")
    fun completeCycle(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
    ): ResponseEntity<KoreanCompleteCycleResponse> {
        return ResponseEntity.ok(studyKoreanService.completeCycle(memberId, stepId, cycleNumber))
    }

    /** 스텝 완료 처리 */
    @PostMapping("/steps/{stepId}/complete")
    fun completeStep(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable stepId: Long,
    ): ResponseEntity<KoreanCompleteStepResponse> {
        return ResponseEntity.ok(studyKoreanService.completeStep(memberId, stepId))
    }

    // ───────────────────────────────────────────────────────────────
    // Leo 연동 API (LRN_32 / LRN_33)
    // ───────────────────────────────────────────────────────────────

    /** Leo 연동 — 국어 스텝 전체 목록 + 상태 (Leo 전용, JWT 없음) */
    @GetMapping("/steps")
    fun getKoreanSteps(
        @RequestParam memberId: Long,
    ): ResponseEntity<KoreanStepsResponse> {
        return ResponseEntity.ok(studyKoreanService.getKoreanSteps(memberId))
    }

    /** LRN_32/33 — 현재 진행 중이거나 진입 가능한 국어 lesson 조회 (Leo 전용, JWT 없음) */
    @GetMapping("/lessons/current")
    fun getCurrentKoreanLesson(
        @RequestParam memberId: Long,
    ): ResponseEntity<KoreanCurrentLessonResponse> {
        return ResponseEntity.ok(studyKoreanService.getCurrentKoreanLesson(memberId))
    }

}
