package com.bifriends.domain.emotion.dto

import com.bifriends.infrastructure.ai.dto.AiEmotionScenarioResponse
import com.bifriends.infrastructure.ai.dto.AiStep3

// ── FE → BE 요청 ─────────────────────────────────────────────────────────

data class EmotionScenarioRequest(
    /** 감정 지정. 미지정 시 AI가 선택. (기쁨/속상함/부끄러움/화남/실망/고마움) */
    val emotion: String? = null,
)

// ── BE → FE 응답 ─────────────────────────────────────────────────────────

/**
 * FE로 반환하는 감정 학습 세트 응답.
 * AI 응답과 거의 동일하지만 step3 image_b64가 image_url로 교체된다.
 */
data class EmotionScenarioResponse(
    val setId: String,
    val emotion: String,
    val situation: String,
    val learnedExpression: String,
    val steps: StepsResponse,
    /** AI 생성 실패로 폴백 시나리오를 반환했는지 여부 */
    val isFallback: Boolean,
) {
    companion object {
        /**
         * AI 응답 + step3 업로드된 comic(imageUrl 교체 완료)으로 FE 응답을 생성한다.
         */
        fun from(
            ai: AiEmotionScenarioResponse,
            step3WithUrls: AiStep3,  // image_b64 → image_url 교체 완료된 step3
        ) = EmotionScenarioResponse(
            setId = ai.setId,
            emotion = ai.emotion,
            situation = ai.situation,
            learnedExpression = ai.learnedExpression,
            isFallback = ai.isFallback,
            steps = StepsResponse(
                step1 = Step1Response(
                    title = ai.steps.step1.title,
                    expression = ai.steps.step1.expression,
                    emotion = ai.steps.step1.emotion,
                    bodySensation = ai.steps.step1.bodySensation,
                    situationExample = ai.steps.step1.situationExample,
                    imageUrl = ai.steps.step1.imageUrl,
                    nextButtonText = ai.steps.step1.nextButtonText,
                ),
                step2 = Step2Response(
                    title = ai.steps.step2.title,
                    visualClue = ai.steps.step2.visualClue,
                    question = ai.steps.step2.question,
                    choices = ai.steps.step2.choices.map {
                        ChoiceResponse(id = it.id, text = it.text, isCorrect = it.isCorrect, feedback = it.feedback)
                    },
                    imageUrl = ai.steps.step2.imageUrl,
                    retryMessage = ai.steps.step2.retryMessage,
                    nextButtonText = ai.steps.step2.nextButtonText,
                ),
                step3 = Step3Response(
                    title = step3WithUrls.title,
                    comic = step3WithUrls.comic.map {
                        ComicCutResponse(
                            cut = it.cut,
                            text = it.text,
                            imageUrl = it.imageUrl ?: "",  // 업로드 후 반드시 채워짐
                        )
                    },
                    question = step3WithUrls.question,
                    choices = step3WithUrls.choices.map {
                        ChoiceResponse(id = it.id, text = it.text, isCorrect = it.isCorrect, feedback = it.feedback)
                    },
                    retryMessage = step3WithUrls.retryMessage,
                    nextButtonText = step3WithUrls.nextButtonText,
                ),
                step4 = Step4Response(
                    title = ai.steps.step4.title,
                    leoIntro = ai.steps.step4.leoIntro,
                    question = ai.steps.step4.question,
                    choices = ai.steps.step4.choices.map {
                        Step4ChoiceResponse(
                            id = it.id, text = it.text, type = it.type,
                            isCorrect = it.isCorrect, feedback = it.feedback,
                        )
                    },
                    retryMessage = ai.steps.step4.retryMessage,
                    successMessage = ai.steps.step4.successMessage,
                    reward = RewardResponse(type = ai.steps.step4.reward.type, amount = ai.steps.step4.reward.amount),
                    completeButtonText = ai.steps.step4.completeButtonText,
                ),
            ),
        )
    }
}

data class StepsResponse(val step1: Step1Response, val step2: Step2Response, val step3: Step3Response, val step4: Step4Response)

data class Step1Response(
    val title: String,
    val expression: String,
    val emotion: String,
    val bodySensation: String,
    val situationExample: String,
    val imageUrl: String,
    val nextButtonText: String,
)

data class Step2Response(
    val title: String,
    val visualClue: String,
    val question: String,
    val choices: List<ChoiceResponse>,
    val imageUrl: String,
    val retryMessage: String,
    val nextButtonText: String,
)

data class Step3Response(
    val title: String,
    val comic: List<ComicCutResponse>,
    val question: String,
    val choices: List<ChoiceResponse>,
    val retryMessage: String,
    val nextButtonText: String,
)

/** FE에는 image_url만 전달. image_b64는 포함하지 않는다. */
data class ComicCutResponse(
    val cut: Int,
    val text: String,
    val imageUrl: String,
)

data class Step4Response(
    val title: String,
    val leoIntro: String,
    val question: String,
    val choices: List<Step4ChoiceResponse>,
    val retryMessage: String,
    val successMessage: String,
    val reward: RewardResponse,
    val completeButtonText: String,
)

data class ChoiceResponse(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val feedback: String,
)

data class Step4ChoiceResponse(
    val id: String,
    val text: String,
    val type: String,
    val isCorrect: Boolean,
    val feedback: String,
)

data class RewardResponse(val type: String, val amount: Int)
