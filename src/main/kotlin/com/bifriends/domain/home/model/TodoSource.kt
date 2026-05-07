package com.bifriends.domain.home.model
//Todo에서 오늘의 할 일 출처 구분 (SYSTEM vs AGENT)
//Enum 은 가능한 값의 목록(상태나 분류를 나타냄)

enum class TodoSource {
    SYSTEM,  // 매일 자동 생성된 할 일 (삭제 불가)
    AGENT,   // AI Agent가 추가한 할 일 (수정/삭제 가능)
}
