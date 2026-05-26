package com.bifriends.domain.study.controller

import com.bifriends.domain.study.dto.*
import com.bifriends.domain.study.service.StudyMathService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/study/math")
class StudyMathController(
    private val studyMathService: StudyMathService,
    private val jwtProvider: JwtProvider,
) {

    /** 4-1. 로드맵 조회 */
    @GetMapping("/roadmap")
    fun getRoadmap(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<RoadmapResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(studyMathService.getRoadmap(memberId))
    }

    /** 4-2. 스텝 콘텐츠 조회 */
    @GetMapping("/steps/{stepId}/content")
    fun getStepContent(
        @RequestHeader("Authorization") token: String,
        @PathVariable stepId: Long,
    ): ResponseEntity<StepContentResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(studyMathService.getStepContent(memberId, stepId))
    }

    /** 4-3. 진도 조회 */
    @GetMapping("/progress")
    fun getProgress(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ProgressResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(studyMathService.getProgress(memberId))
    }

    /** 4-4. 답안 검증 */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate")
    fun validateAnswer(
        @RequestHeader("Authorization") token: String,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
        @PathVariable questionIndex: Int,
        @RequestBody request: ValidateAnswerRequest,
    ): ResponseEntity<ValidateAnswerResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(
            studyMathService.validateAnswer(memberId, stepId, cycleNumber, questionIndex, request)
        )
    }

    /** 4-5. 사이클 완료 처리 */
    @PostMapping("/steps/{stepId}/cycles/{cycleNumber}/complete")
    fun completeCycle(
        @RequestHeader("Authorization") token: String,
        @PathVariable stepId: Long,
        @PathVariable cycleNumber: Int,
    ): ResponseEntity<CompleteCycleResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(studyMathService.completeCycle(memberId, stepId, cycleNumber))
    }

    /** 4-6. 스텝 완료 처리 */
    @PostMapping("/steps/{stepId}/complete")
    fun completeStep(
        @RequestHeader("Authorization") token: String,
        @PathVariable stepId: Long,
    ): ResponseEntity<CompleteStepResponse> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(studyMathService.completeStep(memberId, stepId))
    }

    private fun extractMemberId(token: String): Long {
        val jwt = token.removePrefix("Bearer ")
        return jwtProvider.getMemberId(jwt)
    }
}
