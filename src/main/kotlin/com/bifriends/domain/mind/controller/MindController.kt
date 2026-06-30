package com.bifriends.domain.mind.controller

import com.bifriends.domain.emotion.dto.EmotionScenarioResponse
import com.bifriends.domain.mind.dto.MindScenarioRequest
import com.bifriends.domain.mind.dto.MindSessionDetailResponse
import com.bifriends.domain.mind.dto.MindSessionListResponse
import com.bifriends.domain.mind.dto.MindSessionSaveRequest
import com.bifriends.domain.mind.dto.MindSessionSaveResponse
import com.bifriends.domain.mind.service.MindScenarioService
import com.bifriends.domain.mind.service.MindSessionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/mind")
class MindController(
    private val mindScenarioService: MindScenarioService,
    private val mindSessionService: MindSessionService,
) {

    /**
     * 친구랑 감정 학습 시나리오 생성 (EMO-02, EMO-04)
     *
     * "이야기 보러 가기!" 버튼 클릭 시 호출된다.
     * AI에 4단계 학습 세트 생성을 요청하고 step3 이미지를 Firebase Storage에 업로드하여 반환한다.
     * Firestore 저장과 보상 지급은 POST /api/v1/mind/sessions 에서 처리한다.
     *
     * ⚠️ AI 호출 + Storage 업로드로 응답이 10~30초 소요될 수 있음.
     */
    @PostMapping("/scenario")
    fun generateScenario(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody request: MindScenarioRequest,
    ): ResponseEntity<EmotionScenarioResponse> {
        return ResponseEntity.ok(mindScenarioService.generateScenario(memberId, request))
    }

    /**
     * 학습 완료 세션 저장 (EMO-23)
     *
     * 아이가 step4까지 완료하면 FE가 수신한 세션 전체를 전송한다.
     * BE는 Firestore users/{userId}/mindSessions/{setId}에 저장하고 보상(+3풀)을 지급한다.
     */
    @PostMapping("/sessions")
    fun saveSession(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody request: MindSessionSaveRequest,
    ): ResponseEntity<MindSessionSaveResponse> {
        return ResponseEntity.ok(mindSessionService.saveSession(memberId, request))
    }

    /**
     * 학습 히스토리 목록 조회 (EMO-03)
     *
     * 완료된 세션 목록을 최신순으로 반환한다.
     */
    @GetMapping("/sessions")
    fun getSessionList(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<MindSessionListResponse> {
        return ResponseEntity.ok(mindSessionService.getSessionList(memberId))
    }

    /**
     * 특정 세션 상세 조회 (EMO-27)
     *
     * 완료된 세션을 다시 볼 때 사용한다.
     */
    @GetMapping("/sessions/{sessionId}")
    fun getSession(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable sessionId: String,
    ): ResponseEntity<MindSessionDetailResponse> {
        return ResponseEntity.ok(mindSessionService.getSession(memberId, sessionId))
    }
}
