package com.bifriends.domain.emotion.service

import com.bifriends.domain.emotion.dto.EmotionScenarioRequest
import com.bifriends.domain.emotion.dto.EmotionScenarioResponse
import com.bifriends.domain.home.service.UserStatsService
import com.bifriends.domain.home.model.RewardPolicy
import com.bifriends.domain.home.model.RewardSource
import com.bifriends.infrastructure.ai.AiEmotionScenarioClient
import com.bifriends.infrastructure.ai.dto.AiComicCut
import com.bifriends.infrastructure.ai.dto.AiEmotionScenarioRequest
import com.bifriends.infrastructure.ai.dto.AiStep3
import com.bifriends.infrastructure.firebase.FallbackImageUrlProvider
import com.bifriends.infrastructure.firebase.FirebaseStorageService
import com.bifriends.infrastructure.firebase.FirestoreService
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.repository.MemberInterestRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 친구랑 감정 학습 시나리오 서비스 (EMO-04)
 *
 * ── 처리 순서 ─────────────────────────────────────────────────────────────
 * 1. 회원 정보 조회 (nickname, interests)
 * 2. Firestore에서 이미 학습한 표현 목록 조회 (중복 방지)
 * 3. AI에 시나리오 생성 요청
 * 4. step3 comic 각 컷의 image_b64 → Firebase Storage 업로드 → image_url 교체
 * 5. 완성된 세션을 Firestore mindSessions에 저장
 * 6. 감정 학습 완료 보상 지급 (+3풀, EMOTION)
 * 7. FE 응답 반환
 * ────────────────────────────────────────────────────────────────────────
 */
@Service
class EmotionScenarioService(
    private val memberRepository: MemberRepository,
    private val memberInterestRepository: MemberInterestRepository,
    private val aiClient: AiEmotionScenarioClient,
    private val storageService: FirebaseStorageService,
    private val firestoreService: FirestoreService,
    private val userStatsService: UserStatsService,
    private val fallbackImageUrlProvider: FallbackImageUrlProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateScenario(memberId: Long, request: EmotionScenarioRequest): EmotionScenarioResponse {
        // ── 1. 회원 정보 조회 ──────────────────────────────────────────────
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val nickname = member.nickname ?: member.name

        val interests = memberInterestRepository.findAllByMemberId(memberId)
            .map { it.interest.name }

        // ── 2. 이미 학습한 표현 조회 (Firestore) ───────────────────────────
        val learnedExpressions = firestoreService.getLearnedExpressions(memberId)
        log.info(
            "[EmotionScenario] 시나리오 생성 시작 — memberId={}, learnedCount={}",
            memberId, learnedExpressions.size
        )

        // ── 3. AI 시나리오 생성 요청 ────────────────────────────────────────
        // fallbackUrls: 감정별 step1·step2 고정 폴백 이미지 URL 맵을 함께 전달.
        // AI는 폴백 시나리오 생성 시 이 맵에서 image_url을 꺼내 응답에 채운다.
        val fallbackUrls = runCatching { fallbackImageUrlProvider.buildFallbackUrlMap() }
            .onFailure { log.warn("[EmotionScenario] 폴백 URL 맵 생성 실패 (토큰 미설정?): {}", it.message) }
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

        // ── 4. step3 image_b64 → Firebase Storage 업로드 → image_url 교체 ──
        val step3WithUrls = uploadStep3Images(aiResponse.steps.step3, aiResponse.setId)

        // ── 5. Firestore mindSessions 저장 ─────────────────────────────────
        saveToFirestore(memberId, aiResponse, step3WithUrls)

        // ── 6. 감정 학습 완료 보상 지급 (+3풀) ───────────────────────────────
        // 완료 버튼(EMO-23)을 누를 때가 아닌 세션 생성 시점에 지급.
        // 명세(EMO-21)는 step4 정답 선택 시 지급이므로, 추후 별도 완료 API로 분리 가능.
        // 현재 MVP에서는 세션 생성 완료 = 학습 의지 있음으로 간주하여 생성 시 지급.
        runCatching {
            userStatsService.earnReward(
                memberId = memberId,
                source = RewardSource.EMOTION,
                amount = RewardPolicy.EMOTION,
            )
        }.onFailure {
            log.warn("[EmotionScenario] 보상 지급 실패 (비치명적) — memberId={}: {}", memberId, it.message)
        }

        // ── 7. FE 응답 반환 ────────────────────────────────────────────────
        log.info(
            "[EmotionScenario] 시나리오 생성 완료 — memberId={}, setId={}, isFallback={}",
            memberId, aiResponse.setId, aiResponse.isFallback
        )
        return EmotionScenarioResponse.from(aiResponse, step3WithUrls)
    }

    /**
     * step3 각 컷의 image_b64를 Firebase Storage에 업로드하고 image_url로 교체한다.
     * image_url이 이미 있으면 (폴백 시나리오) 그대로 유지한다.
     */
    private fun uploadStep3Images(step3: AiStep3, setId: String): AiStep3 {
        val updatedComic = step3.comic.map { cut ->
            when {
                // 이미 image_url이 있으면 (폴백) 그대로 통과
                cut.imageUrl != null -> cut

                // image_b64가 있으면 Firebase Storage에 업로드
                cut.imageB64 != null -> {
                    val url = storageService.uploadBase64Image(
                        base64 = cut.imageB64,
                        contentType = "image/png",
                        folder = "mindSessions/$setId/comic",
                    )
                    cut.copy(imageUrl = url, imageB64 = null)  // FE에 image_b64는 노출하지 않음
                }

                else -> {
                    log.warn("[EmotionScenario] step3 컷 {} — image_b64, image_url 모두 없음", cut.cut)
                    cut
                }
            }
        }
        return step3.copy(comic = updatedComic)
    }

    /**
     * 완료된 학습 세션 전체를 Firestore에 저장한다.
     * 저장 실패는 로그만 남기고 메인 응답을 블로킹하지 않는다.
     */
    /**
     * 완료된 학습 세션 전체(step1~4)를 Firestore에 저장한다.
     *
     * 저장 경로: users/{memberId}/mindSessions/{setId}
     * 저장 필드: set_id, emotion, situation, learned_expression, steps 전체, timestamp
     *
     * 저장 실패는 로그만 남기고 메인 응답(FE 반환)을 블로킹하지 않는다.
     * (히스토리 저장 실패가 학습 완료 화면을 막아서는 안 됨)
     */
    private fun saveToFirestore(
        memberId: Long,
        aiResponse: com.bifriends.infrastructure.ai.dto.AiEmotionScenarioResponse,
        step3: AiStep3,
    ) {
        runCatching {
            val step4 = aiResponse.steps.step4

            val sessionData: Map<String, Any> = mapOf(
                "setId"             to aiResponse.setId,
                "emotion"           to aiResponse.emotion,
                "situation"         to aiResponse.situation,
                "learnedExpression" to aiResponse.learnedExpression,
                "isFallback"        to aiResponse.isFallback,
                "completedAt"       to Instant.now().toString(),

                // ── step1 ──────────────────────────────────────────────────
                "step1" to mapOf(
                    "title"           to aiResponse.steps.step1.title,
                    "expression"      to aiResponse.steps.step1.expression,
                    "emotion"         to aiResponse.steps.step1.emotion,
                    "bodySensation"   to aiResponse.steps.step1.bodySensation,
                    "situationExample" to aiResponse.steps.step1.situationExample,
                    "imageUrl"        to aiResponse.steps.step1.imageUrl,
                    "nextButtonText"  to aiResponse.steps.step1.nextButtonText,
                ),

                // ── step2 ──────────────────────────────────────────────────
                "step2" to mapOf(
                    "title"          to aiResponse.steps.step2.title,
                    "visualClue"     to aiResponse.steps.step2.visualClue,
                    "question"       to aiResponse.steps.step2.question,
                    "imageUrl"       to aiResponse.steps.step2.imageUrl,
                    "retryMessage"   to aiResponse.steps.step2.retryMessage,
                    "nextButtonText" to aiResponse.steps.step2.nextButtonText,
                    "choices"        to aiResponse.steps.step2.choices.map {
                        mapOf("id" to it.id, "text" to it.text, "isCorrect" to it.isCorrect, "feedback" to it.feedback)
                    },
                ),

                // ── step3 (image_b64 → image_url 교체 완료) ────────────────
                "step3" to mapOf(
                    "title"          to step3.title,
                    "question"       to step3.question,
                    "retryMessage"   to step3.retryMessage,
                    "nextButtonText" to step3.nextButtonText,
                    "comic"          to step3.comic.map { cut ->
                        mapOf(
                            "cut"         to cut.cut,
                            "text"        to cut.text,
                            "imagePrompt" to cut.imagePrompt,  // 기록용
                            "imageUrl"    to (cut.imageUrl ?: ""),
                        )
                    },
                    "choices"        to step3.choices.map {
                        mapOf("id" to it.id, "text" to it.text, "isCorrect" to it.isCorrect, "feedback" to it.feedback)
                    },
                ),

                // ── step4 ──────────────────────────────────────────────────
                "step4" to mapOf(
                    "title"             to step4.title,
                    "leoIntro"          to step4.leoIntro,
                    "question"          to step4.question,
                    "retryMessage"      to step4.retryMessage,
                    "successMessage"    to step4.successMessage,
                    "completeButtonText" to step4.completeButtonText,
                    "reward"            to mapOf("type" to step4.reward.type, "amount" to step4.reward.amount),
                    "choices"           to step4.choices.map {
                        mapOf(
                            "id"        to it.id,
                            "text"      to it.text,
                            "type"      to it.type,
                            "isCorrect" to it.isCorrect,
                            "feedback"  to it.feedback,
                        )
                    },
                ),
            )

            firestoreService.saveMindSession(memberId, aiResponse.setId, sessionData)
        }.onFailure {
            log.error(
                "[EmotionScenario] Firestore 저장 실패 (비치명적) — memberId={}, setId={}: {}",
                memberId, aiResponse.setId, it.message
            )
        }
    }
}
