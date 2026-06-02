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
import com.bifriends.infrastructure.ai.AiParentMissionClient
import com.bifriends.infrastructure.ai.dto.AiParentMissionRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReportService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val weeklySafetyReportRepository: WeeklySafetyReportRepository,
    private val todoRepository: TodoRepository,
    private val learningAttemptRepository: LearningAttemptRepository,
    private val objectMapper: ObjectMapper,
    private val aiParentMissionClient: AiParentMissionClient,
) {

    // ── RPT-02 리포트 목록 ─────────────────────────────────────────────────

    fun getReports(memberId: Long): ReportListResponse {
        val reports = weeklyReportRepository
            .findAllByMemberIdOrderByWeekStartDesc(memberId)
            .map { report ->
                val sections = parseSections(report.sectionsJson)
                ReportSummaryItem(
                    reportId = report.id,
                    weekStart = report.weekStart,
                    weekEnd = report.weekEnd,
                    safetySignal = extractSafetySignal(sections),
                    hasMission = extractParentMission(sections) != null,
                )
            }
        return ReportListResponse(reports = reports)
    }

    // ── RPT-03~07/08 리포트 상세 ───────────────────────────────────────────

    fun getReportDetail(memberId: Long, reportId: Long): ReportDetailResponse {
        val report = weeklyReportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("리포트를 찾을 수 없습니다. id=$reportId") }

        check(report.member.id == memberId) { "본인의 리포트만 조회할 수 있습니다." }

        val sections = parseSections(report.sectionsJson)
        val learningPattern = calculateLearningPattern(memberId, report)

        return ReportDetailResponse(
            reportId = report.id,
            weekStart = report.weekStart,
            weekEnd = report.weekEnd,
            growth = GrowthResponse(
                summary = sections["growth"]?.get("summary")?.asText(),
                parentTip = sections["growth"]?.get("parentTip")?.asText(),
            ),
            learningPattern = learningPattern,
            learningStatus = LearningStatusResponse(
                math    = sections["learningStatus"]?.get("math")?.asText()?.let { SubjectStatusResponse(it) },
                korean  = sections["learningStatus"]?.get("korean")?.asText()?.let { SubjectStatusResponse(it) },
                emotion = sections["learningStatus"]?.get("emotion")?.asText()?.let { SubjectStatusResponse(it) },
            ),
            chatSafety = ChatSafetyResponse(
                signal = extractSafetySignal(sections),
                score  = sections["chatSafety"]?.get("score")?.asInt() ?: 0,
                reasonSummary = sections["chatSafety"]?.get("reasonSummary")?.asText() ?: "",
            ),
            parentMission = extractParentMission(sections),
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
        val report = weeklyReportRepository.findById(reportId)
            .orElseThrow { IllegalArgumentException("리포트를 찾을 수 없습니다. id=$reportId") }

        check(report.member.id == memberId) { "본인의 리포트만 조회할 수 있습니다." }

        val sections = parseSections(report.sectionsJson).toMutableMap()

        // 캐시 HIT
        val cached = extractParentMission(sections)
        if (cached != null) return cached

        // 캐시 MISS — AI 호출
        val aiResponse = aiParentMissionClient.generateMission(
            AiParentMissionRequest(
                memberId = memberId,
                weekStart = report.weekStart.toString(),
                safetySignal = extractSafetySignal(sections).name,
                growthSummary = sections["growth"]?.get("summary")?.asText(),
                mathSummary = sections["learningStatus"]?.get("math")?.asText(),
                koreanSummary = sections["learningStatus"]?.get("korean")?.asText(),
                emotionSummary = sections["learningStatus"]?.get("emotion")?.asText(),
            )
        )

        // sections에 parentMission 추가 후 저장
        val node = objectMapper.readTree(report.sectionsJson) as com.fasterxml.jackson.databind.node.ObjectNode
        node.set<com.fasterxml.jackson.databind.JsonNode>(
            "parentMission",
            objectMapper.valueToTree(mapOf("praise" to aiResponse.praise, "mission" to aiResponse.mission))
        )
        report.updateSections(objectMapper.writeValueAsString(node))

        return ParentMissionResponse(praise = aiResponse.praise, mission = aiResponse.mission)
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────

    private fun parseSections(json: String): Map<String, com.fasterxml.jackson.databind.JsonNode> {
        return try {
            val node = objectMapper.readTree(json)
            node.fields().asSequence().associate { it.key to it.value }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun extractSafetySignal(sections: Map<String, com.fasterxml.jackson.databind.JsonNode>): SafetySignal {
        val signalStr = sections["chatSafety"]?.get("signal")?.asText() ?: "GREEN"
        return try { SafetySignal.valueOf(signalStr) } catch (e: Exception) { SafetySignal.GREEN }
    }

    private fun extractParentMission(sections: Map<String, com.fasterxml.jackson.databind.JsonNode>): ParentMissionResponse? {
        val missionNode = sections["parentMission"] ?: return null
        val praise = missionNode.get("praise")?.asText() ?: return null
        val mission = missionNode.get("mission")?.asText() ?: return null
        return ParentMissionResponse(praise = praise, mission = mission)
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
