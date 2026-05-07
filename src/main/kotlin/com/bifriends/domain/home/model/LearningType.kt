package com.bifriends.domain.home.model
//Todo에서 오늘의 문제 3개 풀기 (type:learningType) 에 대한 정의
//Erum 은 가능한 값의 목록(상태나 분류를 나타냄)

enum class LearningType {
    MATH,      // 생각하는 힘 (수학) — 월·수·금
    LANGUAGE,  // 말하는 힘 (국어)  — 화·목·토
    // null    → 일요일: 전체 공부방 목록으로 이동 (자유 선택)
}
