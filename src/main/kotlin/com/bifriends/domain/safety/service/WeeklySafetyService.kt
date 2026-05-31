package com.bifriends.domain.safety.service

import com.bifriends.domain.safety.dto.WeeklySafetyReportRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 주간 안전 보고서 처리 서비스
 *
 * MVP: 수신 로깅만 처리.
 * 이후 확장 포인트:
 *   - DB 저장 (WeeklySafetyReport 엔티티)
 *   - CONCERN / DANGER 시 보호자 푸시 알림 발송
 *   - 관리자 대시보드 제공
 */
@Service
class WeeklySafetyService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun receiveReport(request: WeeklySafetyReportRequest) {
        log.info(
            "[WeeklySafety] 보고서 수신 — memberId={}, date={}, level={}, keywords={}",
            request.memberId,
            request.reportDate,
            request.safetyLevel,
            request.keywords,
        )

        // TODO: DB 저장
        // TODO: CONCERN / DANGER 시 보호자 푸시 알림
    }
}
