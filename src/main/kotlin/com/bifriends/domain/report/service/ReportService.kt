package com.bifriends.domain.report.service

import com.bifriends.domain.home.model.TodoType
import com.bifriends.domain.home.repository.TodoRepository
import com.bifriends.domain.learning.model.LearningSubject
import com.bifriends.domain.learning.repository.LearningAttemptRepository
import com.bifriends.domain.report.dto.*
import com.bifriends.domain.report.model.SafetySignal
import com.bifriends.domain.report.model.WeeklyReport
import com.bifriends.domain.report.repository.WeeklyReportRepository
import com.bifriends.domain.report.repository.WeeklySafetyReportRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ReportService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val weeklySafetyReportRepository: WeeklySafetyReportRepository,
    private val todoRepository: TodoRepository,
    private val learningAttemptRepository: LearningAttemptRepository,
    private val objectMapper: ObjectMapper,
) {

    // ── RPT-02 리포트 목록 ─────────────────────────────────────────────────

    fun getReports(memberId: Long): ReportListResponse {
        val reports = weeklyReportRepository
            .findAllByMemberIdOrderByWeekStartDesc(memberId)
            .map { report ->
                ReportSummaryItem(
                    reportId = report.id,
                    weekStart = report.weekStart,
                    weekEnd = report.weekEnd,
                    safetySignal = findSafetySignal(memberId, report.weekStart),
                    hasMission = report.missionRevealed,
                )
            }
        return ReportListResponse(reports = reports)
    }

    // ── RPT-03~07/08 리포트 상세 ───────────────────────────────────────────

    fun getReportDetail(memberId: Long, reportId: Long): ReportDetailResponse {
        val report = findOwnedReport(memberId, reportId)
        val sections = parseSections(report.sectionsJson)

        return ReportDetailResponse(
            reportId = report.id,
            weekStart = report.weekStart,
            weekEnd = report.weekEnd,
            growth = GrowthResponse(summary = extractGrowthSummary(sections)),
            learningPattern = calculateLearningPattern(memberId, report),
            learningStatus = LearningStatusResponse(
                math = extractSubjectStatus(sections["math"]),
                korean = extractSubjectStatus(sections["korean"]),
            ),
            chatSafety = findChatSafety(memberId, report.weekStart),
            parentMission = if (report.missionRevealed) extractParentMission(sections) else null,
            keywords = emptyList(),
        )
    }

    fun getLearningSummary(memberId: Long, from: java.time.LocalDate, to: java.time.LocalDate): LearningSummaryResponse {
        require(!from.isAfter(to)) { "from은 to보다 늦을 수 없습니다." }

        val fromDateTime = from.atStartOfDay()
        val toDateTime = to.atTime(23, 59, 59)

        return LearningSummaryResponse(
            math = aggregateLearningSummary(memberId, LearningSubject.MATH, fromDateTime, toDateTime),
            korean = aggregateLearningSummary(memberId, LearningSubject.KOREAN, fromDateTime, toDateTime),
            todos = TodoSummaryResponse(
                assigned = todoRepository.countAssignedBetween(memberId, from, to),
                completed = todoRepository.countCompletedBetween(memberId, from, to),
            ),
        )
    }

    // ── RPT-08 보호자 미션 ─────────────────────────────────────────────────

    @Transactional
    fun getOrGenerateParentMission(memberId: Long, reportId: Long): ParentMissionResponse {
        val report = findOwnedReport(memberId, reportId)
        val sections = parseSections(report.sectionsJson)

        if (report.missionRevealed) {
            return extractParentMission(sections)
                ?: throw IllegalStateException("보호자 미션이 아직 준비되지 않았습니다.")
        }

        val mission = extractParentMission(sections)
            ?: throw IllegalStateException("보호자 미션이 아직 준비되지 않았습니다.")

        report.revealMission()
        return mission
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────

    private fun findOwnedReport(memberId: Long, reportId: Long): WeeklyReport {
        val report = weeklyReportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("리포트를 찾을 수 없습니다. id=$reportId") }
        check(report.member.id == memberId) { "본인의 리포트만 조회할 수 있습니다." }
        return report
    }

    private fun parseSections(json: String): Map<String, JsonNode> {
        return try {
            val node = objectMapper.readTree(json)
            node.fields().asSequence().associate { it.key to it.value }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun extractGrowthSummary(sections: Map<String, JsonNode>): String? {
        return sections["growth_summary"]?.asText()
            ?: sections["growth"]?.get("summary")?.asText()
    }

    private fun extractSubjectStatus(node: JsonNode?): SubjectStatusResponse? {
        if (node == null || node.isNull) return null
        if (node.isTextual) {
            return SubjectStatusResponse(wellDone = node.asText(), struggled = null)
        }
        return SubjectStatusResponse(
            wellDone = node.get("well_done")?.asText() ?: node.get("wellDone")?.asText(),
            struggled = node.get("struggled")?.asText(),
        )
    }

    private fun extractParentMission(sections: Map<String, JsonNode>): ParentMissionResponse? {
        val missionNode = sections["parent_mission"] ?: sections["parentMission"] ?: return null
        val praise = missionNode.get("praise")?.asText() ?: return null
        val activity = missionNode.get("activity")?.asText()
            ?: missionNode.get("mission")?.asText()
            ?: return null
        return ParentMissionResponse(praise = praise, activity = activity)
    }

    private fun findSafetySignal(memberId: Long, weekStart: LocalDate): SafetySignal {
        return weeklySafetyReportRepository.findByMemberIdAndWeekStart(memberId, weekStart)
            ?.safetySignal
            ?: SafetySignal.GREEN
    }

    private fun findChatSafety(memberId: Long, weekStart: LocalDate): ChatSafetyResponse {
        val safety = weeklySafetyReportRepository.findByMemberIdAndWeekStart(memberId, weekStart)
        return if (safety != null) {
            ChatSafetyResponse(
                signal = safety.safetySignal,
                score = safety.score,
                reasonSummary = safety.reasonSummary ?: "",
            )
        } else {
            ChatSafetyResponse(
                signal = SafetySignal.GREEN,
                score = 0,
                reasonSummary = "",
            )
        }
    }

    private fun calculateLearningPattern(memberId: Long, report: WeeklyReport): LearningPatternResponse {
        val completed = todoRepository.findCompletedBetween(memberId, report.weekStart, report.weekEnd)
        val learningDays = completed
            .filter { it.type == TodoType.LEARNING }
            .map { it.assignedDate.dayOfWeek.value }
            .distinct().sorted()
        return LearningPatternResponse(
            learningDays = learningDays,
            completedTodoCount = completed.count { it.type == TodoType.LEARNING },
        )
    }

    private fun aggregateLearningSummary(
        memberId: Long,
        subject: LearningSubject,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime,
    ): List<LearningConceptSummaryItem> {
        return learningAttemptRepository.findWeeklySummaryBySubject(memberId, subject, from, to)
            .map { row ->
                val concept = row[0] as String
                val solved = row[1] as Long
                val avgAttempts = (row[2] as Number).toDouble()
                val avgHints = (row[3] as Number).toDouble()
                LearningConceptSummaryItem(
                    concept = concept,
                    solved = solved,
                    avgAttempts = avgAttempts,
                    avgHints = avgHints,
                )
            }
    }
}
