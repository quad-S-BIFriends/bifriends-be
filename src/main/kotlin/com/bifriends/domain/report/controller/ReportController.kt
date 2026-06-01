package com.bifriends.domain.report.controller

import com.bifriends.domain.report.dto.*
import com.bifriends.domain.report.service.ReportService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 부모 모드 — 성장일기 리포트 조회 API
 * JWT 인증 필수 (부모 모드 PIN 확인은 FE에서 처리)
 */
@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val reportService: ReportService,
    private val jwtProvider: JwtProvider,
) {

    /** RPT-02 — 주간 리포트 목록 (최신순) */
    @GetMapping
    fun getReports(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ReportListResponse> {
        return ResponseEntity.ok(reportService.getReports(extractMemberId(token)))
    }

    /** RPT-03~07 — 리포트 상세 (성장요약·학습패턴·학습현황·챗안전신호 통합) */
    @GetMapping("/{reportId}")
    fun getReportDetail(
        @RequestHeader("Authorization") token: String,
        @PathVariable reportId: Long,
    ): ResponseEntity<ReportDetailResponse> {
        return ResponseEntity.ok(
            reportService.getReportDetail(extractMemberId(token), reportId)
        )
    }

    /**
     * RPT-08 — 보호자 미션 받기
     * 이미 생성된 미션이 있으면 캐시 반환, 없으면 AI에 생성 요청 후 저장
     */
    @PostMapping("/{reportId}/parent-mission")
    fun getOrGenerateParentMission(
        @RequestHeader("Authorization") token: String,
        @PathVariable reportId: Long,
    ): ResponseEntity<ParentMissionResponse> {
        return ResponseEntity.ok(
            reportService.getOrGenerateParentMission(extractMemberId(token), reportId)
        )
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
