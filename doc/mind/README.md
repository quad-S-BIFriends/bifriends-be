# Mind 도메인 — 친구랑 감정 학습

## 구현 범위

| 엔드포인트 | 메서드 | 기능명세 | 설명 |
|---|---|---|---|
| `/api/v1/mind/scenario` | POST | EMO-02, EMO-04 | 감정 학습 시나리오 생성 + step3 이미지 업로드 |
| `/api/v1/mind/sessions` | POST | EMO-23 | 학습 완료 세션 저장 + 보상 지급 |
| `/api/v1/mind/sessions` | GET | EMO-03 | 학습 히스토리 목록 조회 |
| `/api/v1/mind/sessions/{sessionId}` | GET | EMO-27 | 특정 세션 상세 조회(다시보기) |

## 핵심 설계 결정

### 시나리오 생성과 세션 저장 분리
- **기존 `/api/v1/emotion/scenarios`**: 시나리오 생성 → Firestore 저장 → 보상 지급 한 번에 처리
- **신규 `/api/v1/mind/`**: 생성과 저장을 분리
  - `POST /scenario`: AI 호출 + 이미지 업로드만 담당
  - `POST /sessions`: 아이가 step4까지 완료했을 때 FE가 호출 → Firestore 저장 + 보상 지급

→ **완료된 세션만** `users/{userId}/mindSessions`에 쌓인다.

### 중복 표현 방지 로직
`getLearnedExpressions()`는 Firestore에 저장된 세션의 `learnedExpression` 필드를 읽는다.
완료 세션만 Firestore에 저장되므로, 아이가 중간에 그만둔 표현은 "학습한 표현"으로 카운트되지 않는다.

## DB 스키마 (Firestore)

```
users/{memberId}/mindSessions/{setId}
├── setId             String   AI가 생성한 고유 ID
├── emotion           String   감정명 (기쁨 / 속상함 / 부끄러움 / 화남 / 실망 / 고마움)
├── situation         String   상황 설명
├── learnedExpression String   배운 표현
├── isFallback        Boolean  폴백 시나리오 여부
├── completedAt       String   완료 시각 (ISO-8601 UTC)
├── step1             Map      오늘의 표현 배우기
├── step2             Map      어떤 기분일까요?
├── step3             Map      3컷 만화 (imageUrl 저장, image_b64 미저장)
└── step4             Map      이렇게 말하고 싶어!
```

상세 설계 → [mind_details.md](mind_details.md)
