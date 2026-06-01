package com.bifriends.domain.report.model

/**
 * 챗 안전 신호 (RPT-07)
 *
 * 점수 기준:
 *   0 ~ 3  → GREEN  (정상 사용)
 *   4 ~ 7  → YELLOW (주의 필요)
 *   8 이상 → RED    (보호자 확인 필요)
 */
enum class SafetySignal {
    GREEN,   // 정상 사용
    YELLOW,  // 주의 필요
    RED,     // 보호자 확인 필요
    ;

    companion object {
        fun fromScore(score: Int): SafetySignal = when {
            score >= 8 -> RED
            score >= 4 -> YELLOW
            else       -> GREEN
        }
    }
}
