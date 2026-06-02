package com.bifriends.domain.report.controller

import com.bifriends.domain.report.dto.LearningSummaryResponse
import com.bifriends.domain.report.service.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Leo(AI) 내부 리포트 집계 API.
 * 인증: X-Internal-Service 헤더 (JWT 없음)
 */
@RestController
@RequestMapping("/api/v1/report")
class InternalReportController(
    private val reportService: ReportService,
) {

    @GetMapping("/learning-summary")
    fun getLearningSummary(
        @RequestParam memberId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<LearningSummaryResponse> {
        return ResponseEntity.ok(reportService.getLearningSummary(memberId, from, to))
    }
}
