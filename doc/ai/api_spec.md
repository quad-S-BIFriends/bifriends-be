# BiFriends — AI 연동 API 명세서

> **대상**: bifriends-ai 서비스 개발  
> **발신**: bifriends-be (백엔드)  
> **버전**: 2026-05-28 (초안)  
> **Base URL (Docker 내부)**: `http://bifriends-be:8080`  
> **호스트 테스트**: `http://localhost:18080`

---

## 1. 공통

### 1.1 연동 구조

| 방향 | 설명 |
|------|------|
| **FE → BE → AI** | 사용자 채팅. FE는 JWT로 BE 호출, BE가 AI로 중계 |
| **AI → BE** | 학습 데이터·채팅 이력·할 일 생성 등. Docker 내부망, JWT 없음 |

### 1.2 인증

#### AI → BE (내부 API)

| 항목 | 값 |
|------|-----|
| 방식 | 공유 시크릿 헤더 (JWT 사용 안 함) |
| 헤더 이름 | `X-Internal-Service` |
| 헤더 값 (개발 기본) | `bifriends-ai` |
| 운영 | BE·AI 동일 env `INTERNAL_SERVICE_TOKEN` |

```http
X-Internal-Service: bifriends-ai
```

- 헤더 없음 / 값 불일치 → **401**
- `/api/v1/members/me` 등 JWT API는 AI에서 호출 불가

#### FE → BE (참고, AI 명세 범위 밖)

| 항목 | 값 |
|------|-----|
| 헤더 | `Authorization: Bearer <access_token>` |

#### BE → AI (채팅)

| 항목 | 값 |
|------|-----|
| Base URL (개발) | `http://bifriends-ai:8000` (env `AI_SERVICE_BASE_URL`) |
| 경로 (기본값) | `POST /v1/chat` (env `AI_CHAT_PATH`, **AI 팀 확정 필요**) |
| 인증 | 별도 협의 (내부망 only 등) |

### 1.3 JSON 규칙

| 구간 | 필드명 규칙 |
|------|-------------|
| **AI → BE** (BE가 제공하는 API) | **camelCase** (`memberId`, `sessionId`) |
| **BE → AI** (채팅 요청 body) | **snake_case** (`member_id`, `session_id`) |
| **BE → AI** (채팅 응답) | **camelCase 제안** (`reply`) — AI 팀 확정 후 BE DTO 반영 |

### 1.4 공통 enum

#### Interest (관심사, 최대 3개)

```
DINOSAUR | ANIMAL | SPACE | SPORTS | KPOP_MUSIC | GAME | COOKING | CRAFTING | SCIENCE
```

#### grade (학년)

- 정수 **3 ~ 6**

#### subject (학습 진도 API용, 협의)

```
MATH | KOREAN
```

> FE 학습 탭은 `MATH` / `LANGUAGE`(국어) 용어를 쓰고 있음. AI API에서는 `KOREAN` 사용 제안.

---

## 2. AI가 제공하는 API (BE → AI)

### 2.1 채팅 — `POST /v1/chat` ✅ BE 연동 준비됨

사용자 메시지 1건에 대한 레오 응답 생성.

**상태**: BE `ChatService` 구현 완료. AI URL·응답 필드명 확정 필요.  
**활성화**: BE env `AI_SERVICE_ENABLED=true`

#### Request

```http
POST /v1/chat
Content-Type: application/json
```

```json
{
  "member_id": 1,
  "nickname": "혜나",
  "grade": 4,
  "interests": ["DINOSAUR", "GAME"],
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "message": "수학 도와줘"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `member_id` | number | O | 회원 ID (BE가 JWT에서 설정, FE가 보내지 않음) |
| `nickname` | string | O | 표시 이름 |
| `grade` | number | O | 3~6 |
| `interests` | string[] | O | Interest enum, 1~3개 |
| `session_id` | string | O | 대화 세션 ID |
| `message` | string | O | 사용자 발화 |

> BE는 **DB 조회 없이** FE가 `/api/v1/chat/messages`에 실어 보낸 프로필을 그대로 전달합니다.

#### Response (BE 기대 초안 — **AI 팀 확정 필요**)

```json
{
  "reply": "좋아, 수학 같이 해보자!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reply` | string | O | 레오 응답 텍스트 |

**AI 팀 확인 요청**

- [ ] 실제 path (`/v1/chat` 맞는지)
- [ ] 응답 필드명·추가 필드 (예: `session_id`, `tool_calls`, 스트리밍 여부)
- [ ] 에러 응답 형식

---

## 3. BE가 제공하는 API (AI → BE)

모든 요청에 **`X-Internal-Service: bifriends-ai`** 필수.

### 구현 상태 범례

| 표기 | 의미 |
|------|------|
| ✅ | BE 구현 완료 |
| 🚧 | Security·경로만 등록, Request/Response **협의 후 구현** |

---

### 3.1 회원 프로필 조회 ✅

`member_id`만 알 때 nickname / grade / interests 조회. (`/members/me` 대체)

```http
GET /api/v1/members/{memberId}/profile
X-Internal-Service: bifriends-ai
```

#### Path

| 이름 | 타입 | 설명 |
|------|------|------|
| `memberId` | number | 회원 ID |

#### Response `200`

```json
{
  "memberId": 1,
  "nickname": "혜나",
  "grade": 4,
  "interests": ["DINOSAUR", "GAME"]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | number | 회원 ID |
| `nickname` | string \| null | 미설정 시 null |
| `grade` | number \| null | 미설정 시 null |
| `interests` | string[] | 빈 배열 가능 |

#### Errors

| HTTP | 설명 |
|------|------|
| 401 | 내부 인증 실패 |
| 500 | 존재하지 않는 memberId (추후 404 정리 예정) |

---

### 3.2 수학 스텝 목록 (학년별) 🚧

```http
GET /api/v1/learning/math/steps?grade={grade}
X-Internal-Service: bifriends-ai
```

#### Query

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `grade` | number | O | 3~6 |

#### Response `200` (BE 제안 초안)

> FE용 `/api/v1/study/math/roadmap`과 달리 **회원 진도 없이** 학년별 스텝 카탈로그만 반환.

```json
{
  "grade": 4,
  "steps": [
    {
      "stepId": 1,
      "stepNumber": 1,
      "stepTitle": "분수의 기초",
      "concept": "분수"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `grade` | number | 학년 |
| `steps[].stepId` | number | 스텝 PK |
| `steps[].stepNumber` | number | 학년 내 순서 |
| `steps[].stepTitle` | string | 제목 |
| `steps[].concept` | string | 개념 키워드 |

**AI 팀 확인**: 상세 `content_json`(문제 데이터) 포함 여부

---

### 3.3 국어 스텝 목록 (학년별) 🚧

```http
GET /api/v1/learning/korean/steps?grade={grade}
X-Internal-Service: bifriends-ai
```

#### Query / Response

3.2와 동일 구조. `steps` 데이터 소스만 `korean_step` 테이블.

---

### 3.4 회원 학습 진도 🚧

```http
GET /api/v1/members/{memberId}/learning-progress?subject={subject}
X-Internal-Service: bifriends-ai
```

#### Path

| 이름 | 타입 | 설명 |
|------|------|------|
| `memberId` | number | 회원 ID |

#### Query

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `subject` | string | O | `MATH` \| `KOREAN` |

#### Response `200` (BE 제안 초안)

```json
{
  "memberId": 1,
  "subject": "MATH",
  "lastStepId": 3,
  "totalSteps": 12,
  "completedSteps": 2,
  "progress": [
    {
      "stepId": 1,
      "isStepCompleted": true,
      "completedCycles": [1, 2, 3],
      "lastAccessedAt": "2026-05-28T10:00:00"
    }
  ]
}
```

> FE용 `GET /api/v1/study/math/progress` 응답과 유사. `subject`로 수학/국어 분기.

---

### 3.5 채팅 세션 메시지 목록 🚧

```http
GET /api/v1/chat/sessions/{sessionId}/messages
X-Internal-Service: bifriends-ai
```

#### Path

| 이름 | 타입 | 설명 |
|------|------|------|
| `sessionId` | string | 세션 ID |

#### Response `200` (BE 제안 초안 — **협의 필요**)

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "memberId": 1,
  "messages": [
    {
      "id": "msg-001",
      "role": "user",
      "content": "수학 도와줘",
      "createdAt": "2026-05-28T10:00:00"
    },
    {
      "id": "msg-002",
      "role": "assistant",
      "content": "좋아, 같이 해보자!",
      "createdAt": "2026-05-28T10:00:05"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `messages[].role` | string | `user` \| `assistant` |
| `messages[].content` | string | 메시지 본문 |

**AI 팀 확인**: 페이징, 저장 주체(BE DB vs AI DB), 메시지 ID 형식

---

### 3.6 기간별 채팅 메시지 조회 🚧

```http
GET /api/v1/chat/messages?member_id={memberId}&from={from}&to={to}
X-Internal-Service: bifriends-ai
```

#### Query (AI 팀 제안 — BE query param naming 협의)

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `member_id` | number | O | 회원 ID |
| `from` | string (ISO-8601) | O | 시작 시각 |
| `to` | string (ISO-8601) | O | 종료 시각 |

> BE JSON은 camelCase 관례이나, query는 AI 제안대로 snake_case 가능. **확정 필요**.

#### Response `200` (BE 제안 초안)

```json
{
  "memberId": 1,
  "from": "2026-05-01T00:00:00",
  "to": "2026-05-28T23:59:59",
  "messages": [
    {
      "id": "msg-001",
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "role": "user",
      "content": "안녕",
      "createdAt": "2026-05-28T09:00:00"
    }
  ]
}
```

---

### 3.7 할 일 생성 (Agent) 🚧

```http
POST /api/v1/todos
X-Internal-Service: bifriends-ai
Content-Type: application/json
```

#### Request (BE 제안 초안)

```json
{
  "memberId": 1,
  "type": "CUSTOM",
  "title": "오늘 배운 분수 복습하기",
  "estimatedTimeSec": 120,
  "assignedDate": "2026-05-28"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `memberId` | number | O | 대상 회원 |
| `type` | string | O | `CUSTOM` (Agent 생성) |
| `title` | string | O | 할 일 제목 |
| `estimatedTimeSec` | number | X | 기본값 정책 협의 |
| `assignedDate` | string (date) | X | 미입력 시 KST 오늘 |

#### Response `201` (BE 제안 초안)

```json
{
  "id": 42,
  "type": "CUSTOM",
  "title": "오늘 배운 분수 복습하기",
  "status": "PENDING",
  "estimatedTimeSec": 120,
  "source": "AGENT",
  "learningType": null,
  "assignedDate": "2026-05-28"
}
```

| TodoType | 설명 |
|----------|------|
| `CHAT` | 레오랑 이야기하기 |
| `LEARNING` | 오늘의 문제 풀기 |
| `EMOTION` | 친구 기분 알아보기 |
| `CUSTOM` | Agent 추가 |

| TodoStatus | `PENDING` \| `COMPLETED` |

---

### 3.8 주간 안전 리포트 저장 ✅

```http
POST /api/v1/weekly-safety-report
X-Internal-Service: bifriends-ai
Content-Type: application/json
```

챗 안전 신호 전용. 성장 리포트(`weekly-report`)와 **별도** 배치·저장.

#### Request (snake_case)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `member_id` | long | O | 회원 ID |
| `week_start` | string (date) | O | 주 시작일 |
| `week_end` | string (date) | O | 주 종료일 |
| `safety_signal` | string | O | `GREEN` \| `YELLOW` \| `RED` |
| `score` | int | O | 안전 점수 |
| `reason_summary` | string | X | Gemini 요약 |

```json
{
  "member_id": 1,
  "week_start": "2026-05-26",
  "week_end": "2026-06-01",
  "safety_signal": "YELLOW",
  "score": 5,
  "reason_summary": "이번 주 대화에서 학교 친구 관계에 대한 부정적 감정이 감지되었습니다."
}
```

#### Response `200`

```json
{ "received": true }
```

---

### 3.9 주간 성장 리포트 저장 ✅

```http
POST /api/v1/weekly-report
X-Internal-Service: bifriends-ai
Content-Type: application/json
```

AI가 `POST /api/v1/ai/report/weekly` 등으로 생성한 **4섹션**을 BE에 저장합니다.

> **보호자 미션**: weekly 생성 시 `parent_mission`을 sections에 **함께 포함**합니다.  
> BE → AI 별도 `parent-mission` API 호출은 **사용하지 않습니다**.  
> 부모 `미션 받기` 클릭 시 BE가 저장된 값을 reveal 합니다.

#### Request (snake_case)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `member_id` | long | O | 회원 ID |
| `week_start` | string (date) | O | 주 시작일 |
| `week_end` | string (date) | O | 주 종료일 |
| `sections` | string | O | 4섹션 JSON 문자열 |

`sections` 내부 구조 (AI 생성):

```json
{
  "growth_summary": "이번 주 민준이는 할 일을 15개 중 13개나 꾸준히 해냈어요.",
  "math": {
    "well_done": "받아올림이 없는 세 자리 덧셈을 대부분 한 번에 맞혔어요.",
    "struggled": "받아올림이 있는 뺄셈은 힌트를 여러 번 보며 풀었어요."
  },
  "korean": {
    "well_done": "낱말 익히기를 큰 어려움 없이 해냈어요.",
    "struggled": "이번 주 국어는 특별히 어려워한 부분은 없었어요."
  },
  "parent_mission": {
    "praise": "이번 주 덧셈 정말 척척 풀더라, 너무 멋졌어!",
    "activity": "마트에서 물건 두 개의 값을 함께 빼서 거스름돈을 맞혀보며 뺄셈을 놀이처럼 연습해보세요."
  }
}
```

#### Response `200`

```json
{ "received": true }
```

---

### 3.10 학습 리포트 집계 ✅

```http
GET /api/v1/report/learning-summary?memberId={memberId}&from={from}&to={to}
X-Internal-Service: bifriends-ai
```

주간 성장 리포트 생성 전, 해당 주간의 학습·할 일 집계를 조회합니다.

> **주간 범위**: `week_start` / `week_end`는 **AI가 정한 기간**을 사용합니다.  
> BE는 `weekly-report`·`weekly-safety-report` 콜백의 날짜를 그대로 저장하며, 별도로 주를 재계산하지 않습니다.  
> 이 API도 AI가 정한 `from`·`to`(보통 콜백과 동일한 `week_start`·`week_end`)를 쿼리로 넘기면, 응답 `weekStart`·`weekEnd`에 **실제 집계에 쓴 inclusive 범위**를 돌려줍니다.

#### Query (camelCase)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `memberId` | long | O | 회원 ID |
| `from` | string (date) | O | 집계 시작일 (`yyyy-MM-dd`, inclusive) |
| `to` | string (date) | O | 집계 종료일 (`yyyy-MM-dd`, inclusive) |

- `from`이 `to`보다 늦으면 **400**

#### Response `200` (camelCase)

| Field | Type | Description |
|-------|------|-------------|
| `weekStart` | string (date) | 집계 시작일 (`yyyy-MM-dd`) |
| `weekEnd` | string (date) | 집계 종료일 (`yyyy-MM-dd`) |
| `math` | array | 수학 개념별 집계 |
| `korean` | array | 국어 개념별 집계 |
| `todos` | object | 할 일 배정·완료 건수 |

`math` / `korean` 항목: `{ concept, solved, avg_attempts, avg_hints }`  
`todos`: `{ assigned, completed }`

```json
{
  "weekStart": "2026-05-26",
  "weekEnd": "2026-06-01",
  "math": [
    { "concept": "받아올림 없는 세 자리 덧셈", "solved": 5, "avg_attempts": 1.2, "avg_hints": 0.4 }
  ],
  "korean": [
    { "concept": "낱말 익히기", "solved": 3, "avg_attempts": 2.0, "avg_hints": 1.7 }
  ],
  "todos": { "assigned": 15, "completed": 12 }
}
```

---

### 3.11 채팅 세션 수정 🚧

```http
PATCH /api/v1/chat/sessions/{sessionId}
X-Internal-Service: bifriends-ai
Content-Type: application/json
```

#### Request (BE 제안 초안)

```json
{
  "title": "수학 이야기",
  "status": "ACTIVE"
}
```

#### Response `200` (BE 제안 초안)

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "memberId": 1,
  "title": "수학 이야기",
  "status": "ACTIVE",
  "updatedAt": "2026-05-28T10:00:00"
}
```

**AI 팀 확인**: 수정 가능 필드, 세션 생성 주체(BE/AI/FE)

---

## 4. 참고 — FE → BE 채팅 (AI 직접 호출 아님)

AI 명세 공유용 참고. **호출 주체는 Flutter 앱**.

```http
POST /api/v1/chat/messages
Authorization: Bearer <access_token>
Content-Type: application/json
```

#### Request

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "수학 도와줘",
  "nickname": "혜나",
  "grade": 4,
  "interests": ["DINOSAUR", "GAME"]
}
```

#### Response `200`

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "reply": "좋아, 수학 같이 해보자!"
}
```

| 필드 | 설명 |
|------|------|
| `reply` | AI 응답. `AI_SERVICE_ENABLED=false`이면 `null` |

---

## 5. 경로 정리 (FE vs AI)

| 용도 | FE (JWT) | AI (내부 헤더) |
|------|----------|----------------|
| 수학 학습 | `/api/v1/study/math/**` | `/api/v1/learning/math/**` |
| 국어 학습 | `/api/v1/study/korean/**` | `/api/v1/learning/korean/**` |
| 성장일기 리포트 | `GET/POST /api/v1/reports/**` | — |
| 학습 리포트 집계 | — | `GET /api/v1/report/learning-summary` |
| 주간 안전 콜백 | — | `POST /api/v1/weekly-safety-report` |
| 주간 성장 콜백 | — | `POST /api/v1/weekly-report` |
| 회원 프로필 | `GET /api/v1/members/me` | `GET /api/v1/members/{id}/profile` |
| 채팅 전송 | `POST /api/v1/chat/messages` | — (BE가 AI 호출) |

---

## 6. 환경 변수 체크리스트

### bifriends-be

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `INTERNAL_SERVICE_TOKEN` | `bifriends-ai` | AI → BE 헤더 값 |
| `INTERNAL_SERVICE_AUTH_ENABLED` | `true` | 내부 인증 on/off |
| `AI_SERVICE_ENABLED` | `false` | BE → AI 호출 on/off |
| `AI_SERVICE_BASE_URL` | `http://bifriends-ai:8000` | AI 서버 URL |
| `AI_CHAT_PATH` | `/v1/ai/chat` | 채팅 path |
| `AI_BATCH_WEEKLY_SAFETY_PATH` | `/api/v1/ai/batch/weekly-safety` | 안전 배치 트리거 |
| `AI_EMOTION_SCENARIO_PATH` | `/api/v1/ai/content/scenario` | 감정 시나리오 생성 |

### bifriends-ai (권장)

| 변수 | 설명 |
|------|------|
| `BIFRIENDS_BE_BASE_URL` | `http://bifriends-be:8080` |
| `INTERNAL_SERVICE_TOKEN` | BE와 동일 값 |

---

## 7. AI 팀 회신 요청 사항

1. **채팅 API** (`POST` path, request/response 최종안, 스트리밍 여부)
2. **🚧 API** 중 우선 구현 순서
3. **3.6 query param** naming: `member_id` vs `memberId`
4. **채팅 메시지 저장** — BE DB vs AI DB vs 양쪽
5. **weekly-safety-report** / **weekly-report** 스키마 — ✅ BE 반영 완료 (2026-06-03)
6. **에러 응답** 공통 형식 예시

---

## 8. 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-06-03 | 주간 성장 리포트 콜백(`weekly-report`) 추가, 보호자 미션 weekly 포함·별도 API 폐기, 안전 리포트 스키마 확정 |
| 2026-05-28 | 초안 작성 (프로필·채팅 구현 반영, 나머지 협의 초안) |
