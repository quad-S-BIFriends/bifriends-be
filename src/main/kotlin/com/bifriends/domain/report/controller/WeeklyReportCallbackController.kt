package com.bifriends.domain.report.controller

import com.bifriends.domain.report.dto.WeeklyReportCallbackRequest
import com.bifriends.domain.report.dto.WeeklyReportCallbackResponse
import com.bifriends.domain.report.service.WeeklyReportCallbackService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * AI → BE 주간 성장 리포트 콜백 수신
 * 인증: X-Internal-Service 헤더
 */
@RestController
@RequestMapping("/api/v1")
class WeeklyReportCallbackController(
    private val weeklyReportCallbackService: WeeklyReportCallbackService,
) {
    @PostMapping("/weekly-report")
    fun receiveWeeklyReport(
        @RequestBody request: WeeklyReportCallbackRequest,
    ): ResponseEntity<WeeklyReportCallbackResponse> {
        weeklyReportCallbackService.receiveReport(request)
        return ResponseEntity.ok(WeeklyReportCallbackResponse(received = true))
    }
}
