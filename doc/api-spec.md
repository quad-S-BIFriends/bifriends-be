# BiFriends BE — API 명세서

> **Base URL**: `https://<host>`  
> **인증**: 별도 표기 없으면 `Authorization: Bearer <accessToken>` 헤더 필수  
> **내부 전용**: `X-Internal-Service: <INTERNAL_SERVICE_TOKEN>` 헤더 (JWT 불필요, Leo 전용)  
> **Content-Type**: `application/json`

---

## 목차

1. [헬스체크](#1-헬스체크)
2. [인증 (Auth)](#2-인증-auth)
3. [온보딩 (Onboarding)](#3-온보딩-onboarding)
4. [회원 (Member)](#4-회원-member)
5. [홈 (Home)](#5-홈-home)
6. [공부방 — 수학 (Learning Math)](#6-공부방--수학-learning-math)
7. [공부방 — 국어 (Learning Korean)](#7-공부방--국어-learning-korean)
8. [할 일 (Todo)](#8-할-일-todo)
9. [채팅 (Chat)](#9-채팅-chat)
10. [내부 전용 API (Internal — Leo)](#10-내부-전용-api-internal--leo)

---

## 1. 헬스체크

### GET `/health`

서버 생존 여부 확인. 인증 불필요.

**Response** `200 OK`
```json
{ "status": "ok" }
```

---

## 2. 인증 (Auth)

Base path: `/api/v1/members/auth`

### 2-1. 구글 로그인 (모바일)

**POST** `/api/v1/members/auth/google`

Flutter 앱에서 Google Sign-In으로 발급받은 Firebase ID Token을 전달하면 JWT를 반환합니다. 신규 회원이면 자동 가입 처리됩니다.

**Request Header**

| Key | Value | Required |
|-----|-------|----------|
| (없음) | | |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `idToken` | string | O | Firebase Google ID Token |

```json
{ "idToken": "eyJhbGci..." }
```

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string | JWT Access Token |
| `refreshToken` | string | JWT Refresh Token |
| `email` | string | 회원 이메일 |
| `nickname` | string \| null | 닉네임 (온보딩 전 null) |
| `profileImageUrl` | string \| null | 프로필 이미지 URL |
| `onboardingCompleted` | boolean | 온보딩 완료 여부 |

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "email": "user@example.com",
  "nickname": null,
  "profileImageUrl": "https://...",
  "onboardingCompleted": false
}
```

---

### 2-2. OAuth2 로그인 성공 콜백

**GET** `/api/v1/members/auth/login/success`

Spring Security OAuth2 로그인 성공 후 JWT를 반환합니다. 웹 기반 OAuth2 플로우용.

**Response** `200 OK` — 2-1과 동일 구조

---

## 3. 온보딩 (Onboarding)

Base path: `/api/v1/onboarding` · **JWT 필수**

### 3-1. 보호자 전화번호 저장

**POST** `/api/v1/onboarding/guardian`

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `phoneNumber` | string | O | 보호자 전화번호 (`01[016789]XXXXXXXX`) |

```json
{ "phoneNumber": "01012345678" }
```

**Response** `200 OK`

```json
{ "verified": true }
```

---

### 3-2. 프로필 업데이트

**PATCH** `/api/v1/onboarding/profile`

**Request Body** (모두 선택 — 전달된 필드만 업데이트)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nickname` | string | X | 닉네임 (1~20자) |
| `grade` | int | X | 학년 (3~6) |

```json
{ "nickname": "민준이", "grade": 4 }
```

**Response** `200 OK`

```json
{ "nickname": "민준이", "grade": 4 }
```

---

### 3-3. 관심사 저장

**PUT** `/api/v1/onboarding/interests`

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `interests` | array\<Interest\> | O | 관심사 목록 (1~3개) |

`Interest` 가능 값: `DINOSAUR`, `ANIMAL`, `SPACE`, `SPORTS`, `KPOP_MUSIC`, `GAME`, `COOKING`, `CRAFTING`, `SCIENCE`

```json
{ "interests": ["ANIMAL", "SPACE", "GAME"] }
```

**Response** `200 OK`

```json
{ "interests": ["ANIMAL", "SPACE", "GAME"] }
```

---

### 3-4. 선물 아이템 선택

**POST** `/api/v1/onboarding/gift`

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemType` | ItemType | O | 선택한 아이템 |

`ItemType` 가능 값: `GIFT_1`, `GIFT_2`, `GIFT_3`, `GIFT_4`

```json
{ "itemType": "GIFT_2" }
```

**Response** `200 OK`

```json
{ "itemType": "GIFT_2", "acquiredAt": "2026-05-28T12:00:00" }
```

---

### 3-5. 권한 설정 업데이트

**PATCH** `/api/v1/onboarding/permissions`

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `notificationEnabled` | boolean | O | 푸시 알림 허용 여부 |
| `microphoneEnabled` | boolean | O | 마이크 허용 여부 |

```json
{ "notificationEnabled": true, "microphoneEnabled": false }
```

**Response** `200 OK`

```json
{ "notificationEnabled": true, "microphoneEnabled": false }
```

---

### 3-6. 온보딩 완료 처리

**POST** `/api/v1/onboarding/complete`

Request Body 없음.

**Response** `200 OK`

```json
{ "completed": true }
```

---

## 4. 회원 (Member)

Base path: `/api/v1/members` · **JWT 필수**

### 4-1. 내 프로필 조회

**GET** `/api/v1/members/me`

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | 회원 ID |
| `email` | string | 이메일 |
| `nickname` | string \| null | 닉네임 |
| `profileImageUrl` | string \| null | 프로필 이미지 URL |
| `grade` | int \| null | 학년 |
| `interests` | array\<Interest\> | 관심사 목록 |
| `items` | array\<MemberItemInfo\> | 보유 아이템 목록 |
| `representativeItemType` | ItemType \| null | 대표 아이템 |
| `onboardingCompleted` | boolean | 온보딩 완료 여부 |

`MemberItemInfo`: `{ itemType: ItemType, acquiredAt: string }`

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "민준이",
  "profileImageUrl": "https://...",
  "grade": 4,
  "interests": ["ANIMAL", "SPACE"],
  "items": [{ "itemType": "GIFT_2", "acquiredAt": "2026-05-28T12:00:00" }],
  "representativeItemType": "GIFT_2",
  "onboardingCompleted": true
}
```

---

### 4-2. 대표 아이템 설정

**PATCH** `/api/v1/members/me/representative-item`

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemType` | ItemType | O | 대표로 설정할 아이템 |

```json
{ "itemType": "GIFT_1" }
```

**Response** `200 OK`

```json
{ "representativeItemType": "GIFT_1" }
```

---

## 5. 홈 (Home)

### 5-1. 홈 화면 조회

**GET** `/api/v1/home` · **JWT 필수**

앱 오픈 / 홈 탭 진입 시 호출. 출석 체크(streak 갱신 + 보상 지급)가 **멱등 처리**되어 포함됩니다.

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `member.nickname` | string | 닉네임 |
| `greeting.type` | GreetingType | `FIRST_LOGIN` \| `COMEBACK_SHORT` \| `COMEBACK_LONG` \| `STREAK` |
| `greeting.streakDays` | int | 현재 연속 출석 일수 |
| `greeting.message` | string | 닉네임이 치환된 최종 인사 메시지 |
| `stats.level` | int | 현재 레벨 |
| `stats.availablePool` | int | 사용 가능 풀 |
| `stats.totalPoolEarned` | int | 누적 획득 풀 |
| `stats.streakDays` | int | 연속 출석 일수 |
| `stats.currentLevelProgress` | int | 현재 레벨 내 진행 풀 |
| `stats.totalPoolForCurrentLevelUp` | int | 현재 레벨 → 다음 레벨 총 필요 풀 |
| `stats.poolNeededForNextLevel` | int | 다음 레벨까지 남은 풀 |
| `attendance.isFirstAttendanceToday` | boolean | 오늘 첫 출석 여부 |
| `attendance.streakDays` | int | 연속 출석 일수 |
| `attendance.reward` | RewardResult \| null | 지급된 보상 (이미 처리됐으면 null) |
| `todos` | array\<TodoResponse\> | 오늘의 할 일 목록 |

`TodoResponse` 필드:

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | 할 일 ID |
| `type` | TodoType | `CHAT` \| `LEARNING` \| `EMOTION` \| `CUSTOM` |
| `title` | string | 할 일 제목 |
| `status` | TodoStatus | `PENDING` \| `COMPLETED` |
| `estimatedTimeSec` | int | 예상 소요 시간(초) |
| `source` | TodoSource | `SYSTEM` \| `AGENT` |
| `learningType` | LearningType \| null | `MATH` \| `LANGUAGE` \| null(일요일) |
| `assignedDate` | string | 할당 날짜 (`yyyy-MM-dd`) |

```json
{
  "member": { "nickname": "민준이" },
  "greeting": { "type": "STREAK", "streakDays": 5, "message": "민준아, 5일 연속 출석이야! 🔥" },
  "stats": {
    "level": 3, "availablePool": 12, "totalPoolEarned": 42,
    "streakDays": 5, "currentLevelProgress": 2,
    "totalPoolForCurrentLevelUp": 10, "poolNeededForNextLevel": 8
  },
  "attendance": {
    "isFirstAttendanceToday": true, "streakDays": 5,
    "reward": { "earnedPool": 1, "availablePool": 12, "totalPoolEarned": 42, "levelBefore": 3, "levelAfter": 3 }
  },
  "todos": [
    { "id": 1, "type": "CHAT", "title": "레오랑 이야기하기 🗣️", "status": "PENDING",
      "estimatedTimeSec": 60, "source": "SYSTEM", "learningType": null, "assignedDate": "2026-05-28" },
    { "id": 2, "type": "LEARNING", "title": "오늘의 문제 3개 풀기 📚", "status": "PENDING",
      "estimatedTimeSec": 180, "source": "SYSTEM", "learningType": "MATH", "assignedDate": "2026-05-28" },
    { "id": 3, "type": "EMOTION", "title": "친구 기분 알아보기 💌", "status": "COMPLETED",
      "estimatedTimeSec": 120, "source": "SYSTEM", "learningType": null, "assignedDate": "2026-05-28" }
  ]
}
```

---

## 6. 공부방 — 수학 (Learning Math)

Base path: `/api/v1/learning/math` · **JWT 필수**

### 6-1. 로드맵 조회

**GET** `/api/v1/learning/math/roadmap`

회원의 학년에 해당하는 수학 스텝 목록과 각 스텝의 진행 상태를 반환합니다.

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `grade` | int | 학년 |
| `lastStepId` | long \| null | 마지막으로 접근한 스텝 ID |
| `steps` | array\<StepSummaryResponse\> | 스텝 목록 |

`StepSummaryResponse`:

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `stepNumber` | int | 스텝 번호 |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `status` | StepStatus | `AVAILABLE` \| `IN_PROGRESS` \| `COMPLETED` \| `LOCKED` |
| `completedCycles` | array\<int\> | 완료한 사이클 번호 목록 |

```json
{
  "grade": 4,
  "lastStepId": 2,
  "steps": [
    { "stepId": 1, "stepNumber": 1, "stepTitle": "자연수의 덧셈", "concept": "덧셈",
      "status": "COMPLETED", "completedCycles": [1, 2, 3, 4, 5] },
    { "stepId": 2, "stepNumber": 2, "stepTitle": "자연수의 뺄셈", "concept": "뺄셈",
      "status": "IN_PROGRESS", "completedCycles": [1, 2] },
    { "stepId": 3, "stepNumber": 3, "stepTitle": "자연수의 곱셈", "concept": "곱셈",
      "status": "LOCKED", "completedCycles": [] }
  ]
}
```

---

### 6-2. 스텝 콘텐츠 조회

**GET** `/api/v1/learning/math/steps/{stepId}/content`

`answer`, `explanation` 필드는 서버에서 제거됩니다. 호출 시 `lastAccessedAt`이 갱신됩니다.

**Path Parameter**

| Name | Type | Description |
|------|------|-------------|
| `stepId` | long | 스텝 ID |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `grade` | int | 학년 |
| `cycles` | array\<JsonNode\> | 사이클 목록 (answer/explanation 제거) |

> `cycles` 내 JSON 키는 snake_case입니다 (`cycle_number`, `cycle_type` 등).

```json
{
  "stepId": 2,
  "stepTitle": "자연수의 뺄셈",
  "concept": "뺄셈",
  "grade": 4,
  "cycles": [
    { "cycle_number": 1, "cycle_type": "concept", ... },
    { "cycle_number": 2, "cycle_type": "choice", "question_index": 0, "questions": [...] }
  ]
}
```

---

### 6-3. 진도 조회

**GET** `/api/v1/learning/math/progress`

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `lastStepId` | long \| null | 마지막 접근 스텝 ID |
| `totalSteps` | int | 전체 스텝 수 |
| `completedSteps` | int | 완료된 스텝 수 |
| `progress` | array\<StepProgressItem\> | 스텝별 진도 목록 |

`StepProgressItem`:

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `isStepCompleted` | boolean | 스텝 완료 여부 |
| `completedCycles` | array\<int\> | 완료한 사이클 번호 목록 |
| `lastAccessedAt` | string \| null | 마지막 접근 시각 (ISO 8601) |

```json
{
  "lastStepId": 2,
  "totalSteps": 3,
  "completedSteps": 1,
  "progress": [
    { "stepId": 1, "isStepCompleted": true, "completedCycles": [1,2,3,4,5], "lastAccessedAt": "2026-05-27T10:00:00" },
    { "stepId": 2, "isStepCompleted": false, "completedCycles": [1,2], "lastAccessedAt": "2026-05-28T09:30:00" },
    { "stepId": 3, "isStepCompleted": false, "completedCycles": [], "lastAccessedAt": null }
  ]
}
```

---

### 6-4. 답안 검증

**POST** `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate`

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `stepId` | long | 스텝 ID |
| `cycleNumber` | int | 사이클 번호 (1~5) |
| `questionIndex` | int | 문제 인덱스 (0-based) |

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `answer` | JsonNode | O | 정수형 문자열 또는 `{"numerator": N, "denominator": N}` 분수 객체 |

```json
{ "answer": "42" }
```
```json
{ "answer": { "numerator": 3, "denominator": 4 } }
```

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `correct` | boolean | 정답 여부 |
| `explanation` | JsonNode \| null | 오답 시 해설 (정답이면 null) |

```json
{ "correct": true, "explanation": null }
```
```json
{ "correct": false, "explanation": "3을 더해야 합니다." }
```

---

### 6-5. 사이클 완료 처리

**POST** `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/complete`

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `stepId` | long | 스텝 ID |
| `cycleNumber` | int | 완료한 사이클 번호 |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `cycleNumber` | int | 완료한 사이클 번호 |
| `completedCycles` | array\<int\> | 전체 완료 사이클 목록 |
| `isStepCompleted` | boolean | 5사이클 모두 완료 시 true |

```json
{ "stepId": 2, "cycleNumber": 3, "completedCycles": [1, 2, 3], "isStepCompleted": false }
```

---

### 6-6. 스텝 완료 처리

**POST** `/api/v1/learning/math/steps/{stepId}/complete`

**Path Parameter**

| Name | Type | Description |
|------|------|-------------|
| `stepId` | long | 스텝 ID |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 완료된 스텝 ID |
| `isStepCompleted` | boolean | 완료 처리 결과 |
| `nextStepId` | long \| null | 다음 스텝 ID (마지막이면 null) |
| `nextStepStatus` | StepStatus \| null | 다음 스텝 상태 |

```json
{ "stepId": 2, "isStepCompleted": true, "nextStepId": 3, "nextStepStatus": "AVAILABLE" }
```

---

## 7. 공부방 — 국어 (Learning Korean)

Base path: `/api/v1/learning/korean` · **JWT 필수**

> 수학과 동일한 구조이나 세 가지 차이가 있습니다.
> 1. 콘텐츠 응답에 `passage` 객체 포함
> 2. 답안은 `String` 타입 (분수 없음)
> 3. Cycle 1은 `word_card` 타입 — 문제 없음, 답안 검증 API 호출 불필요

### 7-1. 로드맵 조회

**GET** `/api/v1/learning/korean/roadmap`

응답 구조: 수학 6-1과 동일 (`grade`, `lastStepId`, `steps`)

---

### 7-2. 스텝 콘텐츠 조회

**GET** `/api/v1/learning/korean/steps/{stepId}/content`

수학과 동일하게 `answer` 제거. 국어 전용으로 `passage` 객체가 추가됩니다.

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `grade` | int | 학년 |
| `passage` | JsonNode | 지문 (`title`, `text`, `image`) |
| `cycles` | array\<JsonNode\> | 사이클 목록 (answer 제거) |

---

### 7-3. 진도 조회

**GET** `/api/v1/learning/korean/progress`

응답 구조: 수학 6-3과 동일

---

### 7-4. 답안 검증

**POST** `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate`

> Cycle 1 (`word_card`)은 문제가 없으므로 호출하지 않습니다.

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `answer` | string | O | 텍스트 답안 |

```json
{ "answer": "강아지" }
```

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `correct` | boolean | 정답 여부 |
| `explanation` | string \| null | 오답 시 해설 |

```json
{ "correct": false, "explanation": "정답은 '고양이'입니다." }
```

---

### 7-5. 사이클 완료 처리

**POST** `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/complete`

응답 구조: 수학 6-5와 동일

---

### 7-6. 스텝 완료 처리

**POST** `/api/v1/learning/korean/steps/{stepId}/complete`

응답 구조: 수학 6-6과 동일

---

## 8. 할 일 (Todo)

### 8-1. 할 일 완료 처리

**PATCH** `/api/v1/todos/{todoId}/complete` · **JWT 필수**

할 일을 완료 처리하고 풀 보상을 지급합니다.

- 단일 완료: +1풀
- 오늘 할 일 전부 완료 시: +3풀 보너스 추가 지급

**Path Parameter**

| Name | Type | Description |
|------|------|-------------|
| `todoId` | long | 완료할 할 일 ID |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `todo` | TodoResponse | 완료된 할 일 정보 |
| `singleReward` | RewardResult | 단일 완료 보상 (+1풀) |
| `allCompleteBonus` | RewardResult \| null | 전체 완료 보너스 (+3풀), 남은 할 일이 있으면 null |
| `leveledUp` | boolean | 보상 지급 후 레벨업 여부 |

`RewardResult` 필드:

| Field | Type | Description |
|-------|------|-------------|
| `earnedPool` | int | 이번에 획득한 풀 |
| `availablePool` | int | 보상 후 사용 가능한 풀 |
| `totalPoolEarned` | int | 보상 후 누적 획득 풀 |
| `levelBefore` | int | 보상 전 레벨 |
| `levelAfter` | int | 보상 후 레벨 |

```json
{
  "todo": {
    "id": 2, "type": "LEARNING", "title": "오늘의 문제 3개 풀기 📚",
    "status": "COMPLETED", "estimatedTimeSec": 180,
    "source": "SYSTEM", "learningType": "MATH", "assignedDate": "2026-05-28"
  },
  "singleReward": {
    "earnedPool": 1, "availablePool": 13, "totalPoolEarned": 43,
    "levelBefore": 3, "levelAfter": 3
  },
  "allCompleteBonus": null,
  "leveledUp": false
}
```

### 8-2. Agent 할 일 추가 (Leo 전용)

**POST** `/api/v1/todos` · **X-Internal-Service 인증**

Leo가 회원에게 할 일을 추가합니다. 하루 최대 5개(시스템 3 + Agent 2) 제한.

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `memberId` | long | O | 할 일을 생성할 회원 ID |
| `title` | string | O | 할 일 제목 |
| `estimatedTimeSec` | int | X | 예상 소요 시간(초), 기본값 0 |

```json
{ "memberId": 1, "title": "수학 문제 한 문제 더 풀기", "estimatedTimeSec": 60 }
```

**Response** `200 OK` — `TodoResponse` (위 8-1 참조)

---

### 8-3. Agent 할 일 수정 (Leo 전용)

**PATCH** `/api/v1/todos/{todoId}` · **X-Internal-Service 인증**

> SYSTEM 할 일(스케줄러 생성)은 수정 불가 — `400` 반환

**Path Parameter**: `todoId` (long)

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `memberId` | long | O | 소유권 검증용 회원 ID |
| `title` | string | O | 변경할 제목 |

```json
{ "memberId": 1, "title": "수정된 할 일 제목" }
```

**Response** `200 OK` — `TodoResponse`

---

### 8-4. Agent 할 일 삭제 (Leo 전용)

**DELETE** `/api/v1/todos/{todoId}?memberId={memberId}` · **X-Internal-Service 인증**

> SYSTEM 할 일은 삭제 불가 — `400` 반환

**Path Parameter**: `todoId` (long)  
**Query Parameter**: `memberId` (long) — 소유권 검증용

**Response** `204 No Content`

---

## 9. 채팅 (Chat)

### 8-1. 채팅 메시지 전송

**POST** `/api/v1/chat/messages` · **JWT 필수**

사용자 메시지를 Leo(AI)에게 중계하고 응답을 반환합니다. 프로필 정보는 클라이언트가 `/members/me`에서 확보한 값을 함께 전달합니다.

> **흐름**: `FE → BE → AI(Leo)`  
> BE는 JWT에서 `member_id`를 추출하고, 나머지 프로필 정보는 FE에서 받은 값을 그대로 사용합니다. **DB 조회 없음.**

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | string | O | 채팅 세션 ID |
| `message` | string | O | 사용자 메시지 |
| `nickname` | string | O | 사용자 닉네임 |
| `grade` | int | O | 학년 (3~6) |
| `interests` | array\<Interest\> | O | 관심사 목록 (1~3개) |

```json
{
  "sessionId": "session-uuid-1234",
  "message": "안녕 레오!",
  "nickname": "민준이",
  "grade": 4,
  "interests": ["ANIMAL", "SPACE"]
}
```

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | string | 채팅 세션 ID |
| `reply` | string \| null | Leo의 응답 메시지 |

```json
{ "sessionId": "session-uuid-1234", "reply": "안녕, 민준아! 오늘도 같이 공부해볼까?" }
```

#### BE → AI 내부 호출 포맷

BE가 AI 서버로 보내는 실제 payload입니다. AI 팀 스펙에 맞게 **snake_case**로 직렬화됩니다.

| Field | Type | Description |
|-------|------|-------------|
| `member_id` | long | 회원 ID (JWT에서 추출) |
| `nickname` | string | 닉네임 (FE에서 전달받은 값) |
| `grade` | int | 학년 (FE에서 전달받은 값) |
| `interests` | array\<Interest\> | 관심사 목록 (FE에서 전달받은 값) |
| `session_id` | string | 채팅 세션 ID |
| `message` | string | 사용자 메시지 |

```json
{
  "member_id": 1,
  "nickname": "혜나",
  "grade": 4,
  "interests": ["DINOSAUR", "GAME"],
  "session_id": "session-uuid-1234",
  "message": "수학 도와줘"
}
```

---

## 9. 내부 전용 API (Internal — Leo)

> **인증**: `X-Internal-Service: <INTERNAL_SERVICE_TOKEN>` 헤더 필수  
> JWT 불필요. 앱 클라이언트는 호출 불가.

### 9-1. 회원 프로필 조회

**GET** `/api/v1/members/{memberId}/profile`

| Path Param | Type | Description |
|------------|------|-------------|
| `memberId` | long | 회원 ID |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `memberId` | long | 회원 ID |
| `nickname` | string \| null | 닉네임 |
| `grade` | int \| null | 학년 |
| `interests` | array\<Interest\> | 관심사 목록 |

```json
{ "memberId": 1, "nickname": "민준이", "grade": 4, "interests": ["ANIMAL", "SPACE"] }
```

---

### 9-2. 수학 concept 목록 조회 (LRN_13)

**GET** `/api/v1/learning/math/concepts?memberId={memberId}`

회원 학년의 전체 수학 concept 목록을 반환합니다. Leo가 학습 주제 안내에 사용합니다.

**Query Parameter**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `memberId` | long | O | 조회할 회원 ID (Leo가 채팅 세션에서 보유한 값) |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `grade` | int | 학년 |
| `concepts` | array\<MathConceptItem\> | concept 목록 |

`MathConceptItem`: `{ concept, stepId, stepNumber, stepTitle }`

```json
{
  "grade": 4,
  "concepts": [
    { "concept": "덧셈", "stepId": 1, "stepNumber": 1, "stepTitle": "자연수의 덧셈" },
    { "concept": "뺄셈", "stepId": 2, "stepNumber": 2, "stepTitle": "자연수의 뺄셈" },
    { "concept": "곱셈", "stepId": 3, "stepNumber": 3, "stepTitle": "자연수의 곱셈" }
  ]
}
```

---

### 9-3. 수학 concept별 lesson 상태 조회 (LRN_14/15/16)

**GET** `/api/v1/learning/math/concepts/lesson-status?memberId={memberId}&concept={concept}`

| Query Param | Type | Required | Description |
|-------------|------|----------|-------------|
| `memberId` | long | O | 조회할 회원 ID |
| `concept` | string | O | 조회할 학습 개념 |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `concept` | string | 조회한 concept |
| `lessonStatus` | LessonStatus | 아래 참조 |
| `stepId` | long \| null | 매칭된 스텝 ID (`NOT_FOUND`이면 null) |
| `stepTitle` | string \| null | 스텝 제목 |
| `currentAvailableStepId` | long \| null | `LOCKED`일 때 현재 진입 가능 스텝 ID |
| `currentAvailableStepTitle` | string \| null | `LOCKED`일 때 현재 진입 가능 스텝 제목 |

`LessonStatus` 값 및 Leo 처리 방식:

| 값 | 의미 | Leo 동작 |
|----|------|----------|
| `AVAILABLE` | 진입 가능 (미시작) | `stepId`로 이동 |
| `IN_PROGRESS` | 진행 중 | `stepId`로 이동 |
| `COMPLETED` | 완료 | `stepId`로 이동 |
| `LOCKED` | 이전 스텝 미완료로 잠김 | `currentAvailableStepId`로 이동 유도 |
| `NOT_FOUND` | 현재 학년 커리큘럼에 없음 | 안내 메시지 출력 |

```json
{
  "concept": "뺄셈",
  "lessonStatus": "IN_PROGRESS",
  "stepId": 2,
  "stepTitle": "자연수의 뺄셈",
  "currentAvailableStepId": null,
  "currentAvailableStepTitle": null
}
```

```json
{
  "concept": "곱셈",
  "lessonStatus": "LOCKED",
  "stepId": 3,
  "stepTitle": "자연수의 곱셈",
  "currentAvailableStepId": 2,
  "currentAvailableStepTitle": "자연수의 뺄셈"
}
```

---

### 9-4. 현재 국어 lesson 조회 (LRN_32/33)

**GET** `/api/v1/learning/korean/lessons/current?memberId={memberId}`

Leo가 "국어 공부 도움" 의도를 분류한 뒤 진입할 스텝을 조회합니다.

**Query Parameter**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `memberId` | long | O | 조회할 회원 ID |

우선순위: `IN_PROGRESS` → `AVAILABLE` → 첫 번째 스텝

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 진입할 스텝 ID |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `lessonStatus` | StepStatus | `IN_PROGRESS` \| `AVAILABLE` \| `COMPLETED` |

```json
{
  "stepId": 5,
  "stepTitle": "낱말 익히기",
  "concept": "어휘",
  "lessonStatus": "IN_PROGRESS"
}
```

---

### 9-5. 주간 안전 보고서 수신 (AI → BE 콜백)

**POST** `/api/v1/weekly-safety-report` · **X-Internal-Service 인증**

AI가 주간 채팅 분석을 완료한 뒤 결과를 BE로 전송합니다.  
BE 스케줄러가 매주 금요일 18:00 KST에 AI 배치를 트리거하고, AI가 분석 완료 후 이 엔드포인트로 콜백합니다.

> **필드 구조는 AI 팀과 최종 명세 확정 필요**

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `member_id` | long | O | 보고서 대상 회원 ID |
| `report_date` | string | O | 분석 기준 날짜 (`yyyy-MM-dd`, 해당 주 금요일) |
| `safety_level` | SafetyLevel | O | `SAFE` \| `CONCERN` \| `DANGER` |
| `summary` | string | O | 주간 대화 요약 |
| `keywords` | array\<string\> | X | 주요 감지 키워드 |

```json
{
  "member_id": 1,
  "report_date": "2026-05-29",
  "safety_level": "CONCERN",
  "summary": "이번 주 대화에서 학교 친구 관계에 대한 부정적 감정이 감지되었습니다.",
  "keywords": ["외로움", "친구"]
}
```

**Response** `200 OK`

```json
{ "received": true }
```

---

## 공통 에러 응답

| Status | 설명 |
|--------|------|
| `400 Bad Request` | 요청 파라미터/바디 유효성 오류 |
| `401 Unauthorized` | 토큰 누락 또는 만료 |
| `403 Forbidden` | 권한 없음 (타인 리소스 접근 등) |
| `404 Not Found` | 존재하지 않는 리소스 |
| `500 Internal Server Error` | 서버 오류 |

```json
{ "error": "Unauthorized" }
```

---

*최종 수정: 2026-05-28*
