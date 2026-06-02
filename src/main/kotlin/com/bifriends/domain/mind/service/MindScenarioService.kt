package com.bifriends.domain.mind.service

import com.bifriends.domain.emotion.dto.EmotionScenarioResponse
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.mind.dto.MindScenarioRequest
import com.bifriends.domain.onboarding.repository.MemberInterestRepository
import com.bifriends.infrastructure.ai.AiEmotionScenarioClient
import com.bifriends.infrastructure.ai.dto.AiEmotionScenarioRequest
import com.bifriends.infrastructure.ai.dto.AiStep3
import com.bifriends.infrastructure.firebase.FallbackImageUrlProvider
import com.bifriends.infrastructure.firebase.FirebaseStorageService
import com.bifriends.infrastructure.firebase.FirestoreService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 친구랑 감정 학습 시나리오 생성 서비스
 *
 * POST /api/v1/mind/scenario 핸들러.
 * AI 시나리오 생성 + step3 이미지 업로드까지만 담당하며,
 * Firestore 저장과 보상 지급은 MindSessionService(POST /api/v1/mind/sessions)가 처리한다.
 *
 * 완료된 세션만 Firestore에 쌓이므로, learnedExpressions 조회 결과는
 * 아이가 끝까지 마친 세션만 반영된다.
 */
@Service
class MindScenarioService(
    private val memberRepository: MemberRepository,
    private val memberInterestRepository: MemberInterestRepository,
    private val aiClient: AiEmotionScenarioClient,
    private val storageService: FirebaseStorageService,
    private val firestoreService: FirestoreService,
    private val fallbackImageUrlProvider: FallbackImageUrlProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateScenario(memberId: Long, request: MindScenarioRequest): EmotionScenarioResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
        val nickname = member.nickname ?: "친구"
        val interests = memberInterestRepository.findAllByMemberId(memberId).map { it.interest.name }

        // 완료된 세션에서만 learned expressions를 읽어 중복 방지
        val learnedExpressions = firestoreService.getLearnedExpressions(memberId)
        log.info("[MindScenario] 시나리오 생성 시작 — memberId={}, learnedCount={}", memberId, learnedExpressions.size)

        val fallbackUrls = runCatching { fallbackImageUrlProvider.buildFallbackUrlMap() }
            .onFailure { log.warn("[MindScenario] 폴백 URL 맵 생성 실패: {}", it.message) }
            .getOrDefault(emptyMap())

        val aiResponse = aiClient.generateScenario(
            AiEmotionScenarioRequest(
                memberId = memberId,
                nickname = nickname,
                interests = interests,
                learnedExpressions = learnedExpressions,
                emotion = request.emotion,
                fallbackUrls = fallbackUrls,
            )
        )

        val step3WithUrls = uploadStep3Images(aiResponse.steps.step3, aiResponse.setId)

        log.info(
            "[MindScenario] 시나리오 생성 완료 — memberId={}, setId={}, isFallback={}",
            memberId, aiResponse.setId, aiResponse.isFallback,
        )
        return EmotionScenarioResponse.from(aiResponse, step3WithUrls)
    }

    private fun uploadStep3Images(step3: AiStep3, setId: String): AiStep3 {
        val updatedComic = step3.comic.map { cut ->
            when {
                cut.imageUrl != null -> cut
                cut.imageB64 != null -> {
                    val url = storageService.uploadBase64Image(
                        base64 = cut.imageB64,
                        contentType = "image/png",
                        folder = "mindSessions/$setId/comic",
                    )
                    cut.copy(imageUrl = url, imageB64 = null)
                }
                else -> {
                    log.warn("[MindScenario] step3 컷 {} — image_b64, image_url 모두 없음", cut.cut)
                    cut
                }
            }
        }
        return step3.copy(comic = updatedComic)
    }
}
