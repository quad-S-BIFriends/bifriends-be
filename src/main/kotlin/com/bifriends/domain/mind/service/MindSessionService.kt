package com.bifriends.domain.mind.service

import com.bifriends.domain.home.model.RewardPolicy
import com.bifriends.domain.home.model.RewardSource
import com.bifriends.domain.home.service.UserStatsService
import com.bifriends.domain.mind.dto.MindChoiceData
import com.bifriends.domain.mind.dto.MindComicCutData
import com.bifriends.domain.mind.dto.MindRewardData
import com.bifriends.domain.mind.dto.MindSessionDetailResponse
import com.bifriends.domain.mind.dto.MindSessionListResponse
import com.bifriends.domain.mind.dto.MindSessionSaveRequest
import com.bifriends.domain.mind.dto.MindSessionSaveResponse
import com.bifriends.domain.mind.dto.MindSessionSummary
import com.bifriends.domain.mind.dto.MindStep1Data
import com.bifriends.domain.mind.dto.MindStep2Data
import com.bifriends.domain.mind.dto.MindStep3Data
import com.bifriends.domain.mind.dto.MindStep4ChoiceData
import com.bifriends.domain.mind.dto.MindStep4Data
import com.bifriends.domain.mind.dto.MindStepsData
import com.bifriends.infrastructure.firebase.FirestoreOperationException
import com.bifriends.infrastructure.firebase.FirestoreService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * 친구랑 학습 세션 저장·조회 서비스
 *
 * 아이가 step4까지 완료하면 FE가 POST /api/v1/mind/sessions를 호출한다.
 * 이 시점에 Firestore 저장과 보상 지급이 함께 이루어진다.
 */
@Service
class MindSessionService(
    private val firestoreService: FirestoreService,
    private val userStatsService: UserStatsService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun saveSession(memberId: Long, request: MindSessionSaveRequest): MindSessionSaveResponse {
        val sessionData = buildSessionMap(request)
        try {
            firestoreService.saveMindSession(memberId, request.setId, sessionData)
        } catch (e: FirestoreOperationException) {
            throw firestoreUnavailable("학습 세션을 저장하지 못했습니다.", e)
        }

        var rewardAmount = 0
        runCatching {
            userStatsService.earnReward(
                memberId = memberId,
                source = RewardSource.EMOTION,
                amount = RewardPolicy.EMOTION,
            )
            rewardAmount = RewardPolicy.EMOTION
        }.onFailure {
            log.warn("[MindSession] 보상 지급 실패 (비치명적) — memberId={}: {}", memberId, it.message)
        }

        log.info("[MindSession] 세션 저장 완료 — memberId={}, setId={}, reward={}", memberId, request.setId, rewardAmount)
        return MindSessionSaveResponse(setId = request.setId, rewardAmount = rewardAmount)
    }

    fun getSessionList(memberId: Long): MindSessionListResponse {
        val docs = try {
            firestoreService.getMindSessionList(memberId)
        } catch (e: FirestoreOperationException) {
            throw firestoreUnavailable("학습 히스토리를 불러오지 못했습니다.", e)
        }
        val summaries = docs.map { data ->
            MindSessionSummary(
                setId             = data["setId"] as? String ?: "",
                emotion           = data["emotion"] as? String ?: "",
                learnedExpression = data["learnedExpression"] as? String ?: "",
                completedAt       = data["completedAt"] as? String ?: "",
                isFallback        = data["isFallback"] as? Boolean ?: false,
            )
        }
        return MindSessionListResponse(sessions = summaries, totalCount = summaries.size)
    }

    fun getSession(memberId: Long, sessionId: String): MindSessionDetailResponse {
        val data = try {
            firestoreService.getMindSession(memberId, sessionId)
        } catch (e: FirestoreOperationException) {
            throw firestoreUnavailable("학습 세션을 불러오지 못했습니다.", e)
        }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다. sessionId=$sessionId")
        return mapToDetail(data)
    }

    private fun firestoreUnavailable(message: String, cause: FirestoreOperationException) =
        ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message, cause)

    private fun buildSessionMap(req: MindSessionSaveRequest): Map<String, Any> {
        val s = req.steps
        return mapOf(
            "setId"             to req.setId,
            "emotion"           to req.emotion,
            "situation"         to req.situation,
            "learnedExpression" to req.learnedExpression,
            "isFallback"        to req.isFallback,
            "completedAt"       to Instant.now().toString(),
            "step1" to mapOf(
                "title"            to s.step1.title,
                "expression"       to s.step1.expression,
                "emotion"          to s.step1.emotion,
                "bodySensation"    to s.step1.bodySensation,
                "situationExample" to s.step1.situationExample,
                "imageUrl"         to s.step1.imageUrl,
                "nextButtonText"   to s.step1.nextButtonText,
            ),
            "step2" to mapOf(
                "title"          to s.step2.title,
                "visualClue"     to s.step2.visualClue,
                "question"       to s.step2.question,
                "imageUrl"       to s.step2.imageUrl,
                "retryMessage"   to s.step2.retryMessage,
                "nextButtonText" to s.step2.nextButtonText,
                "choices"        to s.step2.choices.map { c ->
                    mapOf("id" to c.id, "text" to c.text, "isCorrect" to c.isCorrect, "feedback" to c.feedback)
                },
            ),
            "step3" to mapOf(
                "title"          to s.step3.title,
                "question"       to s.step3.question,
                "retryMessage"   to s.step3.retryMessage,
                "nextButtonText" to s.step3.nextButtonText,
                "comic"          to s.step3.comic.map { cut ->
                    mapOf("cut" to cut.cut, "text" to cut.text, "imageUrl" to cut.imageUrl)
                },
                "choices"        to s.step3.choices.map { c ->
                    mapOf("id" to c.id, "text" to c.text, "isCorrect" to c.isCorrect, "feedback" to c.feedback)
                },
            ),
            "step4" to mapOf(
                "title"              to s.step4.title,
                "leoIntro"           to s.step4.leoIntro,
                "question"           to s.step4.question,
                "retryMessage"       to s.step4.retryMessage,
                "successMessage"     to s.step4.successMessage,
                "completeButtonText" to s.step4.completeButtonText,
                "reward"             to mapOf("type" to s.step4.reward.type, "amount" to s.step4.reward.amount),
                "choices"            to s.step4.choices.map { c ->
                    mapOf(
                        "id" to c.id, "text" to c.text, "type" to c.type,
                        "isCorrect" to c.isCorrect, "feedback" to c.feedback,
                    )
                },
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToDetail(data: Map<String, Any>): MindSessionDetailResponse {
        val s1 = data["step1"] as? Map<String, Any> ?: emptyMap()
        val s2 = data["step2"] as? Map<String, Any> ?: emptyMap()
        val s3 = data["step3"] as? Map<String, Any> ?: emptyMap()
        val s4 = data["step4"] as? Map<String, Any> ?: emptyMap()

        return MindSessionDetailResponse(
            setId             = data["setId"] as? String ?: "",
            emotion           = data["emotion"] as? String ?: "",
            situation         = data["situation"] as? String ?: "",
            learnedExpression = data["learnedExpression"] as? String ?: "",
            isFallback        = data["isFallback"] as? Boolean ?: false,
            completedAt       = data["completedAt"] as? String ?: "",
            steps = MindStepsData(
                step1 = MindStep1Data(
                    title            = s1["title"] as? String ?: "",
                    expression       = s1["expression"] as? String ?: "",
                    emotion          = s1["emotion"] as? String ?: "",
                    bodySensation    = s1["bodySensation"] as? String ?: "",
                    situationExample = s1["situationExample"] as? String ?: "",
                    imageUrl         = s1["imageUrl"] as? String ?: "",
                    nextButtonText   = s1["nextButtonText"] as? String ?: "",
                ),
                step2 = MindStep2Data(
                    title          = s2["title"] as? String ?: "",
                    visualClue     = s2["visualClue"] as? String ?: "",
                    question       = s2["question"] as? String ?: "",
                    imageUrl       = s2["imageUrl"] as? String ?: "",
                    retryMessage   = s2["retryMessage"] as? String ?: "",
                    nextButtonText = s2["nextButtonText"] as? String ?: "",
                    choices        = toChoiceList(s2["choices"]),
                ),
                step3 = MindStep3Data(
                    title          = s3["title"] as? String ?: "",
                    question       = s3["question"] as? String ?: "",
                    retryMessage   = s3["retryMessage"] as? String ?: "",
                    nextButtonText = s3["nextButtonText"] as? String ?: "",
                    comic          = toComicList(s3["comic"]),
                    choices        = toChoiceList(s3["choices"]),
                ),
                step4 = run {
                    val reward = s4["reward"] as? Map<String, Any> ?: emptyMap()
                    MindStep4Data(
                        title              = s4["title"] as? String ?: "",
                        leoIntro           = s4["leoIntro"] as? String ?: "",
                        question           = s4["question"] as? String ?: "",
                        retryMessage       = s4["retryMessage"] as? String ?: "",
                        successMessage     = s4["successMessage"] as? String ?: "",
                        completeButtonText = s4["completeButtonText"] as? String ?: "",
                        reward = MindRewardData(
                            type   = reward["type"] as? String ?: "",
                            amount = (reward["amount"] as? Long)?.toInt() ?: 0,
                        ),
                        choices = toStep4ChoiceList(s4["choices"]),
                    )
                },
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun toChoiceList(raw: Any?): List<MindChoiceData> =
        (raw as? List<Map<String, Any>> ?: emptyList()).map { c ->
            MindChoiceData(
                id        = c["id"] as? String ?: "",
                text      = c["text"] as? String ?: "",
                isCorrect = c["isCorrect"] as? Boolean ?: false,
                feedback  = c["feedback"] as? String ?: "",
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun toComicList(raw: Any?): List<MindComicCutData> =
        (raw as? List<Map<String, Any>> ?: emptyList()).map { cut ->
            MindComicCutData(
                cut      = (cut["cut"] as? Long)?.toInt() ?: 0,
                text     = cut["text"] as? String ?: "",
                imageUrl = cut["imageUrl"] as? String ?: "",
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun toStep4ChoiceList(raw: Any?): List<MindStep4ChoiceData> =
        (raw as? List<Map<String, Any>> ?: emptyList()).map { c ->
            MindStep4ChoiceData(
                id        = c["id"] as? String ?: "",
                text      = c["text"] as? String ?: "",
                type      = c["type"] as? String ?: "",
                isCorrect = c["isCorrect"] as? Boolean ?: false,
                feedback  = c["feedback"] as? String ?: "",
            )
        }
}
