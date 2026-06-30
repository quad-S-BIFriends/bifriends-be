package com.bifriends.domain.report.controller

import com.bifriends.domain.report.dto.*
import com.bifriends.domain.report.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 부모 모드 — 성장일기 리포트 조회 API
 * JWT 인증 필수 (부모 모드 PIN 확인은 FE에서 처리)
 */
@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val reportService: ReportService,
) {

    /** RPT-02 — 주간 리포트 목록 (최신순) */
    @GetMapping
    fun getReports(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<ReportListResponse> {
        return ResponseEntity.ok(reportService.getReports(memberId))
    }

    /** RPT-03~07 — 리포트 상세 (성장요약·학습패턴·학습현황·챗안전신호 통합) */
    @GetMapping("/{reportId}")
    fun getReportDetail(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable reportId: Long,
    ): ResponseEntity<ReportDetailResponse> {
        return ResponseEntity.ok(
            reportService.getReportDetail(memberId, reportId)
        )
    }

    /**
     * RPT-08 — 보호자 미션 받기
     * weekly 리포트에 저장된 parent_mission을 수령(reveal)한다. AI 별도 호출 없음.
     */
    @PostMapping("/{reportId}/parent-mission")
    fun revealParentMission(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable reportId: Long,
    ): ResponseEntity<ParentMissionResponse> {
        return ResponseEntity.ok(
            reportService.getOrGenerateParentMission(memberId, reportId)
        )
    }

    /** 리포트 수동 생성 트리거 — 특정 주의 학습 데이터를 AI에 전송해 리포트 생성 요청 */
    @PostMapping("/generate")
    fun generateReport(
        @AuthenticationPrincipal memberId: Long,
        @RequestBody request: GenerateReportRequest,
    ): ResponseEntity<GenerateReportResponse> {
        return ResponseEntity.ok(
            reportService.generateReport(memberId, request.weekStart)
        )
    }

}
