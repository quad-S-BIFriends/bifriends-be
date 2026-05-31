package com.bifriends.domain.safety.controller

import com.bifriends.domain.safety.dto.WeeklySafetyReportRequest
import com.bifriends.domain.safety.dto.WeeklySafetyReportResponse
import com.bifriends.domain.safety.service.WeeklySafetyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * AI → BE 주간 안전 보고서 콜백 수신 컨트롤러
 *
 * 인증: X-Internal-Service 헤더 (JWT 불필요)
 * 호출 주체: bifriends-ai (Leo)
 */
@RestController
@RequestMapping("/api/v1")
class WeeklySafetyController(
    private val weeklySafetyService: WeeklySafetyService,
) {

    @PostMapping("/weekly-safety-report")
    fun receiveWeeklySafetyReport(
        @RequestBody request: WeeklySafetyReportRequest,
    ): ResponseEntity<WeeklySafetyReportResponse> {
        weeklySafetyService.receiveReport(request)
        return ResponseEntity.ok(WeeklySafetyReportResponse(received = true))
    }
}
