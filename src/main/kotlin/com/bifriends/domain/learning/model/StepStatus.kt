package com.bifriends.domain.learning.model

enum class StepStatus {
    COMPLETED,    // 완료한 스텝
    IN_PROGRESS,  // 진행 중인 스텝 (progress 행 존재, 미완료)
    AVAILABLE,    // 진입 가능한 스텝 (이전 스텝 완료 or 첫 스텝)
    LOCKED        // 잠긴 스텝 (이전 스텝 미완료)
}
