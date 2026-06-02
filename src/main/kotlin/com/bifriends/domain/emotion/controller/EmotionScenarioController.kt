package com.bifriends.domain.emotion.controller

import com.bifriends.domain.emotion.dto.EmotionScenarioRequest
import com.bifriends.domain.emotion.dto.EmotionScenarioResponse
import com.bifriends.domain.emotion.service.EmotionScenarioService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/emotion")
class EmotionScenarioController(
    private val emotionScenarioService: EmotionScenarioService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * 친구랑 감정 학습 세트 생성 (EMO-02, EMO-04)
     *
     * 사용자가 "이야기 보러 가기!" 버튼을 누르면 호출된다.
     * BE가 AI에 4단계 감정 학습 세트 생성을 요청하고,
     * step3 이미지를 Firebase Storage에 업로드한 뒤 전체 세트를 반환한다.
     *
     * ⚠️ AI 호출 + Storage 업로드로 응답이 10~30초 소요될 수 있음.
     *    클라이언트는 로딩 화면을 표시해야 한다.
     *
     * Authorization: Bearer {accessToken}
     */
    @PostMapping("/scenarios")
    fun generateScenario(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: EmotionScenarioRequest,
    ): ResponseEntity<EmotionScenarioResponse> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        return ResponseEntity.ok(emotionScenarioService.generateScenario(memberId, request))
    }
}
