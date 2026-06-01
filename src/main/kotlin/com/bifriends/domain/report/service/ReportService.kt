package com.bifriends.domain.report.service

import com.bifriends.domain.home.model.TodoType
import com.bifriends.domain.home.repository.TodoRepository
import com.bifriends.infrastructure.ai.AiParentMissionClient
import com.bifriends.infrastructure.ai.dto.AiParentMissionRequest
import com.bifriends.domain.report.dto.*
import com.bifriends.domain.report.model.WeeklyReport
import com.bifriends.domain.report.repository.WeeklyReportRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek

@Service
@Transactional(readOnly = true)
class ReportService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val todoRepository: TodoRepository,
    private val objectMapper: ObjectMapper,
    private val aiParentMissionClient: AiParentMissionClient,
) {

    // ── RPT-02 리포트 목록 ─────────────────────────────────────────────────

    fun getReports(memberId: Long): ReportListResponse {
        val reports = weeklyReportRepository
            .findAllByMemberIdOrderByWeekStartDesc(memberId)
            .map { ReportSummaryItem.from(it) }
        return ReportListResponse(reports = reports)
    }

    // ── RPT-03~07 리포트 상세 ──────────────────────────────────────────────

    fun getReportDetail(memberId: Long, reportId: Long): ReportDetailResponse {
        val report = weeklyReportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("리포트를 찾을 수 없습니다. id=$reportId") }

        check(report.member.id == memberId) { "본인의 리포트만 조회할 수 있습니다." }

        // RPT-05 학습 패턴 — DB에서 직접 계산
        val learningPattern = calculateLearningPattern(memberId, report)

        // RPT-06 학습 현황
        val learningStatus = LearningStatusResponse(
            math    = report.mathSummary?.let { SubjectStatusResponse(it) },
            korean  = report.koreanSummary?.let { SubjectStatusResponse(it) },
            emotion = report.emotionSummary?.let { SubjectStatusResponse(it) },
        )

        // 키워드 파싱
        val keywords = parseKeywords(report.keywordsJson)

        return ReportDetailResponse(
            reportId = report.id,
            weekStart = report.weekStart,
            weekEnd = report.weekEnd,
            growth = GrowthResponse(
                summary = report.growthSummary,
                parentTip = report.parentTip,
            ),
            learningPattern = learningPattern,
            learningStatus = learningStatus,
            chatSafety = ChatSafetyResponse(
                signal = report.safetySignal,
                score = report.safetyScore,
                reasonSummary = report.safetyReasonSummary,
            ),
            parentMission = if (report.parentMission != null && report.parentMissionPraise != null) {
                ParentMissionResponse(
                    praise = report.parentMissionPraise!!,
                    mission = report.parentMission!!,
                )
            } else null,
            keywords = keywords,
        )
    }

    // ── RPT-08 보호자 미션 (on-demand 생성 + 캐시) ──────────────────────

    /**
     * 보호자 미션 버튼 클릭 시 호출.
     * 이미 생성된 미션이 있으면 캐시를 반환하고, 없으면 AI에 요청 후 저장한다.
     */
    @Transactional
    fun getOrGenerateParentMission(memberId: Long, reportId: Long): ParentMissionResponse {
        val report = weeklyReportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("리포트를 찾을 수 없습니다. id=$reportId") }

        check(report.member.id == memberId) { "본인의 리포트만 조회할 수 있습니다." }

        // 캐시 HIT — 이미 생성된 미션 반환
        if (report.parentMission != null && report.parentMissionPraise != null) {
            return ParentMissionResponse(
                praise = report.parentMissionPraise!!,
                mission = report.parentMission!!,
            )
        }

        // 캐시 MISS — AI에 생성 요청
        val aiRequest = AiParentMissionRequest(
            memberId = report.member.id,
            weekStart = report.weekStart.toString(),
            safetySignal = report.safetySignal.name,
            growthSummary = report.growthSummary,
            mathSummary = report.mathSummary,
            koreanSummary = report.koreanSummary,
            emotionSummary = report.emotionSummary,
            keywords = parseKeywords(report.keywordsJson),
        )

        val aiResponse = aiParentMissionClient.generateMission(aiRequest)

        // DB 캐시 저장
        report.cacheParentMission(
            praise = aiResponse.praise,
            mission = aiResponse.mission,
        )

        return ParentMissionResponse(
            praise = aiResponse.praise,
            mission = aiResponse.mission,
        )
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────

    /**
     * RPT-05 학습 패턴 계산
     * 해당 주에 완료된 할 일 기준으로 학습 요일·횟수를 집계한다.
     */
    private fun calculateLearningPattern(
        memberId: Long,
        report: WeeklyReport,
    ): LearningPatternResponse {
        val completedTodos = todoRepository.findCompletedBetween(
            memberId = memberId,
            from = report.weekStart,
            to = report.weekEnd,
        )

        // 학습 완료 횟수 (LEARNING 타입)
        val learningCount = completedTodos.count { it.type == TodoType.LEARNING }

        // 학습한 요일 (1=월 ~ 7=일, 중복 제거)
        val learningDays = completedTodos
            .filter { it.type == TodoType.LEARNING }
            .map { it.assignedDate.dayOfWeek.value }  // DayOfWeek.MONDAY = 1
            .distinct()
            .sorted()

        return LearningPatternResponse(
            learningDays = learningDays,
            completedTodoCount = learningCount,
        )
    }

    private fun parseKeywords(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
