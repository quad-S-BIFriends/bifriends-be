package com.bifriends.infrastructure.ai

import com.bifriends.infrastructure.ai.dto.AiEmotionScenarioRequest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.http.client.SimpleClientHttpRequestFactory

class AiEmotionScenarioClientHttpTest {

    @Test
    fun `generateScenario sends Content-Type application/json and non-empty JSON body`() {
        val restClient = RestClient.builder()
            .baseUrl("http://ai-test")
            .requestFactory(SimpleClientHttpRequestFactory())
            .build()
        val mockServer = MockRestServiceServer.bindTo(restClient).build()

        val properties = AiServiceProperties(
            enabled = true,
            baseUrl = "http://ai-test",
            emotionScenarioPath = "/api/v1/ai/content/scenario",
        )
        val client = AiEmotionScenarioClient(restClient, properties)

        mockServer.expect(requestTo("http://ai-test/api/v1/ai/content/scenario"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
            .andExpect(
                content().json(
                    """
                    {
                      "member_id": 42,
                      "nickname": "테스트",
                      "interests": ["DINOSAUR"],
                      "learned_expressions": ["고마워"],
                      "emotion": "기쁨",
                      "fallback_urls": {}
                    }
                    """.trimIndent(),
                ),
            )
            .andRespond(withSuccess(minimalAiResponseJson(), MediaType.APPLICATION_JSON))

        client.generateScenario(
            AiEmotionScenarioRequest(
                memberId = 42,
                nickname = "테스트",
                interests = listOf("DINOSAUR"),
                learnedExpressions = listOf("고마워"),
                emotion = "기쁨",
            ),
        )

        mockServer.verify()
    }

    private fun minimalAiResponseJson(): String = """
        {
          "set_id": "set-1",
          "emotion": "기쁨",
          "situation": "s",
          "learned_expression": "e",
          "is_fallback": true,
          "steps": {
            "step1": {
              "title": "t", "expression": "e", "emotion": "기쁨",
              "body_sensation": "b", "situation_example": "s",
              "image_url": "http://img", "next_button_text": "n"
            },
            "step2": {
              "title": "t", "visual_clue": "v", "question": "q",
              "choices": [{"id": "1", "text": "a", "is_correct": true, "feedback": "f"}],
              "image_url": "http://img", "retry_message": "r", "next_button_text": "n"
            },
            "step3": {
              "title": "t",
              "comic": [{"cut": 1, "text": "c", "image_prompt": "p", "image_url": "http://img"}],
              "question": "q",
              "choices": [{"id": "1", "text": "a", "is_correct": true, "feedback": "f"}],
              "retry_message": "r", "next_button_text": "n"
            },
            "step4": {
              "title": "t", "leo_intro": "l", "question": "q",
              "choices": [{"id": "1", "text": "a", "type": "empathetic", "is_correct": true, "feedback": "f"}],
              "retry_message": "r", "success_message": "s",
              "reward": {"type": "pool", "amount": 3},
              "complete_button_text": "done"
            }
          }
        }
    """.trimIndent()
}
