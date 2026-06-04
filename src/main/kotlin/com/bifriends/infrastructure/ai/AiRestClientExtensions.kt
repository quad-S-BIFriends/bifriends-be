package com.bifriends.infrastructure.ai

import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/** BE → AI POST: Content-Type + JSON body를 명시 (FastAPI body 누락 방지). */
internal fun RestClient.RequestBodyUriSpec.postJson(body: Any): RestClient.ResponseSpec =
    contentType(MediaType.APPLICATION_JSON).body(body)
