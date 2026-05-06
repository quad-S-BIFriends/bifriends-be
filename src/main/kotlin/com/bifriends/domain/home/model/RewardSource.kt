package com.bifriends.domain.home.model

/**
 * 풀 보상의 출처
 * reward_history.source 컬럼에 String으로 저장된다.
 */
enum class RewardSource {
    ATTENDANCE,             // 연속 출석 보상
    LEARNING_CORRECT,       // 문제 정답
    LEARNING_SET_COMPLETE,  // 3문제 세트 완료 보너스
    EMOTION,                // 마음 시나리오 완료
    TODO_SINGLE,            // 할 일 하나 완료
    TODO_ALL_COMPLETE,      // 할 일 전체 완료 보너스
}
