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
4. [부모 모드 (Parent)](#4-부모-모드-parent)
5. [회원 (Member)](#5-회원-member)
6. [홈 (Home)](#6-홈-home)
7. [상점 (Shop)](#7-상점-shop)
8. [공부방 — 수학 (Learning Math)](#8-공부방--수학-learning-math)
9. [공부방 — 국어 (Learning Korean)](#9-공부방--국어-learning-korean)
10. [할 일 (Todo)](#10-할-일-todo)
11. [친구랑 — 감정 학습 (Mind)](#11-친구랑--감정-학습-mind)
12. [채팅 (Chat)](#12-채팅-chat)
13. [내부 전용 API (Internal — Leo)](#13-내부-전용-api-internal--leo)

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

Flutter 앱에서 Google Sign-In으로 발급받은 Firebase ID Token을 전달하면 JWT를 반환합니다.
신규 회원이면 자동 가입 처리됩니다.

> 구글 계정은 **보호자(부모)** 계정입니다. 아동은 별도 계정 없이 앱을 사용합니다.

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
| `nickname` | string \| null | 아동 닉네임 (온보딩 전 null) |
| `profileImageUrl` | string \| null | 프로필 이미지 URL |
| `onboardingCompleted` | boolean | 온보딩 완료 여부 |

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "email": "parent@example.com",
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

> **흐름 (ONB-01 → ONB-11)**
> 구글 로그인 → **약관 동의** → **부모 PIN 설정** → 이름 입력 → (Leo 인사) → 학년 선택 → 관심사 → 선물 → (부모 안내) → 권한 → 완료

---

### 3-1. 약관 동의 (ONB-02)

**POST** `/api/v1/onboarding/terms`

서비스 이용약관·개인정보 처리방침 동의. 필수 항목이 `true`가 아니면 `400` 반환.

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `termsAgreed` | boolean | O | 서비스 이용약관 동의 (반드시 `true`) |
| `privacyAgreed` | boolean | O | 개인정보 처리방침 동의 (반드시 `true`) |
| `marketingAgreed` | boolean | X | 마케팅 수신 동의 (선택, 기본값 `false`) |

```json
{ "termsAgreed": true, "privacyAgreed": true, "marketingAgreed": false }
```

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `termsAgreed` | boolean | 이용약관 동의 여부 |
| `privacyAgreed` | boolean | 개인정보 동의 여부 |
| `marketingAgreed` | boolean | 마케팅 동의 여부 |
| `agreedAt` | datetime | 동의 시각 (법적 근거 보관) |

```json
{
  "termsAgreed": true,
  "privacyAgreed": true,
  "marketingAgreed": false,
  "agreedAt": "2026-06-01T10:00:00"
}
```

---

### 3-2. 부모 비밀번호 설정 (ONB-02-01)

**POST** `/api/v1/onboarding/parent-password`

부모 모드 진입에 사용할 4~6자리 숫자 PIN을 설정합니다. 두 값 불일치 시 `400` 반환.

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `password` | string | O | 4~6자리 숫자 PIN |
| `passwordConfirm` | string | O | PIN 확인 |

```json
{ "password": "1234", "passwordConfirm": "1234" }
```

**Response** `200 OK`

```json
{ "configured": true }
```

---

### 3-3. 프로필 입력 — 이름·학년 (ONB-04/06)

**PATCH** `/api/v1/onboarding/profile`

이름(ONB-04)과 학년(ONB-06)은 각각 별도 화면이지만 동일 엔드포인트를 사용합니다. 전달된 필드만 업데이트됩니다.

**Request Body** (모두 선택)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `nickname` | string | X | 아동 이름 (1~20자) |
| `grade` | int | X | 학년 (3~6) |

```json
{ "nickname": "민준이", "grade": 4 }
```

**Response** `200 OK`

```json
{ "nickname": "민준이", "grade": 4 }
```

---

### 3-4. 관심사 선택 (ONB-07)

**PUT** `/api/v1/onboarding/interests`

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

### 3-5. 선물 아이템 선택 (ONB-08)

**POST** `/api/v1/onboarding/gift`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemType` | ItemType | O | 선택한 아이템 |

`ItemType` (= 상점 `itemCode`): `GIFT_1`(책), `GIFT_2`(리본), `GIFT_3`(꽃다발), `GIFT_4`(선글라스)

```json
{ "itemType": "GIFT_2" }
```

**Response** `200 OK`

```json
{ "itemType": "GIFT_2", "acquiredAt": "2026-06-01T12:00:00" }
```

---

### 3-6. 권한 설정 (ONB-10)

**PATCH** `/api/v1/onboarding/permissions`

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

### 3-7. 온보딩 완료 (ONB-11)

**POST** `/api/v1/onboarding/complete`

아래 조건을 모두 충족해야 합니다. 미충족 시 `400` 반환.
- 약관 동의 완료
- 부모 비밀번호 설정 완료
- 닉네임(이름) 입력 완료
- 학년 선택 완료

Request Body 없음.

**Response** `200 OK`

```json
{ "completed": true }
```

---

## 4. 부모 모드 (Parent)

Base path: `/api/v1/parent` · **JWT 필수**

### 4-1. 부모 모드 PIN 확인 (RPT-01)

**POST** `/api/v1/parent/verify`

부모 모드 진입 시 PIN을 검증합니다. 틀려도 `200`을 반환하며, `verified: false`로 구분합니다 (보안상 오류 이유 미노출).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `password` | string | O | 입력한 PIN |

```json
{ "password": "1234" }
```

**Response** `200 OK`

```json
{ "verified": true }
```

---

### 4-2. 부모 비밀번호 변경 (RPT-12)

**PATCH** `/api/v1/parent/password`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `currentPassword` | string | O | 현재 PIN |
| `newPassword` | string | O | 새 PIN (4~6자리 숫자) |
| `newPasswordConfirm` | string | O | 새 PIN 확인 |

```json
{ "currentPassword": "1234", "newPassword": "5678", "newPasswordConfirm": "5678" }
```

**Response** `200 OK`

```json
{ "changed": true }
```

---

### 4-3. 주간 리포트 목록 (RPT-02)

**GET** `/api/v1/reports`

부모 모드에서 주간 성장일기 목록을 최신순으로 조회합니다.

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `reports` | array | 리포트 요약 목록 |
| `reports[].reportId` | long | 리포트 ID |
| `reports[].weekStart` | string (date) | 주 시작일 (`yyyy-MM-dd`) |
| `reports[].weekEnd` | string (date) | 주 종료일 |
| `reports[].safetySignal` | SafetySignal | 챗 안전 신호 (`GREEN` \| `YELLOW` \| `RED`) |
| `reports[].hasMission` | boolean | 보호자 미션 **수령** 여부 (`미션 받기` 클릭 완료) |

```json
{
  "reports": [
    {
      "reportId": 12,
      "weekStart": "2026-05-26",
      "weekEnd": "2026-06-01",
      "safetySignal": "GREEN",
      "hasMission": false
    }
  ]
}
```

> `safetySignal`은 `weekly_safety_report` 테이블(별도 AI 배치)에서 조회합니다.

---

### 4-4. 리포트 상세 (RPT-03~07)

**GET** `/api/v1/reports/{reportId}`

성장 요약·학습 패턴·학습 현황·챗 안전 신호를 통합 조회합니다.

| Field | Type | Description |
|-------|------|-------------|
| `reportId` | long | 리포트 ID |
| `weekStart` / `weekEnd` | string (date) | 주간 범위 |
| `growth.summary` | string \| null | AI 생성 성장 요약 |
| `learningPattern.learningDays` | array\<int\> | 학습한 요일 (1=월 ~ 7=일, BE 계산) |
| `learningPattern.completedTodoCount` | int | 해당 주 학습 할 일 완료 횟수 (BE 계산) |
| `learningStatus.math` | SubjectStatus \| null | 수학 잘한 점 / 어려운 점 |
| `learningStatus.korean` | SubjectStatus \| null | 국어 잘한 점 / 어려운 점 |
| `chatSafety.signal` | SafetySignal | 챗 안전 신호 |
| `chatSafety.score` | int | 안전 점수 |
| `chatSafety.reasonSummary` | string | 위험 요약 |
| `parentMission` | ParentMission \| null | **미션 받기 전 null** |
| `keywords` | array\<string\> | 주요 키워드 (현재 빈 배열) |

`SubjectStatus`: `{ "well_done": string \| null, "struggled": string \| null }`  
`ParentMission`: `{ "praise": string, "activity": string }`

```json
{
  "reportId": 12,
  "weekStart": "2026-05-26",
  "weekEnd": "2026-06-01",
  "growth": {
    "summary": "이번 주 민준이는 할 일을 15개 중 13개나 꾸준히 해냈어요."
  },
  "learningPattern": {
    "learningDays": [1, 2, 4, 5],
    "completedTodoCount": 8
  },
  "learningStatus": {
    "math": {
      "well_done": "받아올림이 없는 세 자리 덧셈을 대부분 한 번에 맞혔어요.",
      "struggled": "받아올림이 있는 뺄셈은 힌트를 여러 번 보며 풀었어요."
    },
    "korean": {
      "well_done": "낱말 익히기를 큰 어려움 없이 해냈어요.",
      "struggled": "이번 주 국어는 특별히 어려워한 부분은 없었어요."
    }
  },
  "chatSafety": {
    "signal": "GREEN",
    "score": 2,
    "reasonSummary": ""
  },
  "parentMission": null,
  "keywords": []
}
```

---

### 4-5. 보호자 미션 받기 (RPT-08)

**POST** `/api/v1/reports/{reportId}/parent-mission`

주간 리포트에 **이미 저장된** `parent_mission`을 수령(reveal)합니다. AI 별도 호출 없음.

- 최초 클릭: `missionRevealed = true`로 저장 후 미션 반환
- 재클릭: 저장된 미션 반환
- weekly 콜백에 `parent_mission`이 없으면 `500` (`보호자 미션이 아직 준비되지 않았습니다.`)

**Response** `200 OK`

```json
{
  "praise": "이번 주 덧셈 정말 척척 풀더라, 너무 멋졌어!",
  "activity": "마트에서 물건 두 개의 값을 함께 빼서 거스름돈을 맞혀보며 뺄셈을 놀이처럼 연습해보세요."
}
```

> 수령 후 `GET /api/v1/reports/{reportId}`의 `parentMission`도 동일 값으로 채워집니다.

---

## 5. 회원 (Member)

Base path: `/api/v1/members` · **JWT 필수**

### 5-1. 내 프로필 조회

**GET** `/api/v1/members/me`

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | 회원 ID |
| `email` | string | 이메일 |
| `nickname` | string \| null | 아동 닉네임 |
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
  "email": "parent@example.com",
  "nickname": "민준이",
  "profileImageUrl": "https://...",
  "grade": 4,
  "interests": ["ANIMAL", "SPACE"],
  "items": [{ "itemType": "GIFT_2", "acquiredAt": "2026-06-01T12:00:00" }],
  "representativeItemType": "GIFT_2",
  "onboardingCompleted": true
}
```

---

### 5-2. 대표 아이템 설정

**PATCH** `/api/v1/members/me/representative-item`

> **아이템 체계 안내**
> - **온보딩 선물** (`GIFT_1`~`GIFT_4`): [3-5 선물 선택](#3-5-선물-아이템-선택-onb-08)으로 획득 → `GET /members/me`의 `items`에 표시 → **이 API**로 대표 설정
> - **상점 코스메틱** (모자·안경·옷·배경): [7. 상점 (Shop)](#7-상점-shop)에서 풀 차감 **구매** 후 착용. 대표 아이템 API와 **별개**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemType` | ItemType | O | 대표로 설정할 아이템 (`GIFT_1`~`GIFT_4`만 가능) |

```json
{ "itemType": "GIFT_1" }
```

**Response** `200 OK`

```json
{ "representativeItemType": "GIFT_1" }
```

---

## 6. 홈 (Home)

### 6-1. 홈 화면 조회

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
| `equippedItems` | EquippedItemsResponse | 착용 중인 전체 의상 (`itemCode`) |

`EquippedItemsResponse` 필드:

| Field | Type | Description |
|-------|------|-------------|
| `outfitCode` | string \| null | 착용 중 의상 코드 (예: `GIFT_3`, `OUTFIT_DEFAULT`) |

`TodoResponse` 필드:

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | 할 일 ID |
| `type` | TodoType | `CHAT` \| `LEARNING` \| `EMOTION` \| `CUSTOM` |
| `title` | string | 할 일 제목 |
| `status` | TodoStatus | `PENDING` \| `COMPLETED` |
| `source` | TodoSource | `SYSTEM` \| `AGENT` |
| `learningType` | LearningType \| null | `MATH` \| `LANGUAGE` \| null(일요일) |
| `assignedDate` | string | 할당 날짜 (`yyyy-MM-dd`) |

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
      "source": "SYSTEM", "learningType": null, "assignedDate": "2026-06-01" },
    { "id": 2, "type": "LEARNING", "title": "오늘의 문제 3개 풀기 📚", "status": "PENDING",
      "source": "SYSTEM", "learningType": "MATH", "assignedDate": "2026-06-01" },
    { "id": 3, "type": "EMOTION", "title": "친구 기분 알아보기 💌", "status": "COMPLETED",
      "source": "SYSTEM", "learningType": null, "assignedDate": "2026-06-01" }
  ],
  "equippedItems": {
    "outfitCode": "GIFT_3"
  }
}
```

---

## 7. 상점 (Shop)

Base path: `/api/v1/shop` · **JWT 필수** · 기능명세 **HOM-09**

레오 **전체 의상** 프리셋을 풀로 구매·착용합니다. FE 연동 ID는 **`itemCode`** (고정 문자열)를 사용합니다.

| 구분 | 획득 방법 | 보유 판정 | 착용 |
|------|-----------|-----------|------|
| 기본 의상 `OUTFIT_DEFAULT` | 항상 보유 (0풀) | `owned: true` | `PATCH .../equip` |
| 온보딩 선물 `GIFT_1`~`GIFT_4` | `POST /onboarding/gift` | 상점 목록에서도 `owned: true` | 동일 |
| 그 외 의상 | `POST /shop/items/{itemCode}/purchase` | 구매 후 `owned: true` | 동일 |

`ShopItemCategory`: `OUTFIT` (전체 의상만 노출)

### 의상 카탈로그 (`itemCode` · 가격)

| itemCode | 이름 | 풀 | 온보딩 선물 |
|----------|------|-----|-------------|
| `OUTFIT_DEFAULT` | 기본 | 0 | |
| `GIFT_3` | 꽃다발 | 5 | O |
| `GIFT_1` | 책 | 5 | O |
| `GIFT_4` | 선글라스 | 10 | O |
| `GIFT_2` | 리본 | 10 | O |
| `GIFT_6` | 과학자 가운 | 15 | |
| `GIFT_7` | 가수 | 15 | |
| `GIFT_5` | 공룡 의상 | 15 | |
| `OUTFIT_STUDYING` | 공부중 | 20 | |

온보딩 매핑: `GIFT_1`=책, `GIFT_2`=리본, `GIFT_3`=꽃다발, `GIFT_4`=선글라스

> 대표 아이템(나의 보물)은 `PATCH /members/me/representative-item` (`ItemType`) — 상점 착용과 별도.

---

### 7-1. 상점 아이템 목록 (HOM-09-01/03)

**GET** `/api/v1/shop/items`

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `availablePool` | int | 현재 보유 풀 |
| `items` | array | 의상 목록 (`category` = `OUTFIT`) |

`ShopItemResponse`: `{ itemCode, name, category, price, imageKey, owned }`

```json
{
  "availablePool": 12,
  "items": [
    { "itemCode": "OUTFIT_DEFAULT", "name": "기본", "category": "OUTFIT", "price": 0, "imageKey": "outfit_default", "owned": true },
    { "itemCode": "GIFT_3", "name": "꽃다발", "category": "OUTFIT", "price": 5, "imageKey": "outfit_flower", "owned": true },
    { "itemCode": "GIFT_5", "name": "공룡 의상", "category": "OUTFIT", "price": 15, "imageKey": "outfit_dino", "owned": false }
  ]
}
```

---

### 7-2. 나의 서랍

**GET** `/api/v1/shop/my-items`

보유 의상(기본·온보딩·구매) + 착용 코드.

```json
{
  "items": [
    { "itemCode": "OUTFIT_DEFAULT", "name": "기본", "category": "OUTFIT", "imageKey": "outfit_default", "acquiredAt": null },
    { "itemCode": "GIFT_3", "name": "꽃다발", "category": "OUTFIT", "imageKey": "outfit_flower", "acquiredAt": null }
  ],
  "equipped": { "outfitCode": "GIFT_3" }
}
```

---

### 7-3. 아이템 구매

**POST** `/api/v1/shop/items/{itemCode}/purchase`

- `price` > 0 만 구매 가능 (`OUTFIT_DEFAULT` 구매 불가)
- 온보딩 선물·이미 구매한 `itemCode`는 거부

**Response**: `{ itemCode, itemName, category, imageKey, remainingPool, acquiredAt }`

---

### 7-4. 아이템 착용

**PATCH** `/api/v1/shop/items/{itemCode}/equip`

보유한 의상만 착용. 홈 `equippedItems.outfitCode`와 동기화.

```json
{ "equipped": { "outfitCode": "GIFT_5" } }
```

---

### 7-5. 착용 해제

**DELETE** `/api/v1/shop/items/equip`

```json
{ "equipped": { "outfitCode": null } }
```

---

## 8. 공부방 — 수학 (Learning Math)

Base path: `/api/v1/learning/math` · **JWT 필수**

### 8-1. 로드맵 조회

**GET** `/api/v1/learning/math/roadmap`

**Response** `200 OK`

```json
{
  "grade": 4,
  "lastStepId": 2,
  "steps": [
    { "stepId": 1, "stepNumber": 1, "stepTitle": "자연수의 덧셈", "concept": "덧셈",
      "status": "COMPLETED", "completedCycles": [1, 2, 3] },
    { "stepId": 2, "stepNumber": 2, "stepTitle": "자연수의 뺄셈", "concept": "뺄셈",
      "status": "IN_PROGRESS", "completedCycles": [1] }
  ]
}
```

`status`: `AVAILABLE` \| `IN_PROGRESS` \| `COMPLETED` \| `LOCKED`

---

### 8-2. 스텝 콘텐츠 조회

**GET** `/api/v1/learning/math/steps/{stepId}/content`

`answer`, `explanation` 필드는 서버에서 제거됩니다. 호출 시 `lastAccessedAt`이 갱신됩니다.

**Path Parameter**

| Name | Type | Description |
| --- | --- | --- |
| `stepId` | long | 스텝 ID |

**Response** `200 OK`

| Field | Type | Description |
| --- | --- | --- |
| `stepId` | long | 스텝 ID |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `grade` | int | 학년 |
| `cycles` | array\<JsonNode\> | 사이클 목록 (answer/explanation 제거) |

> `cycles` 내 JSON 키는 snake_case입니다 (`cycle_number`, `cycle_type` 등). 수학·국어 공통 canonical 키입니다. DB 시드에 `cycle`/`type`(국어)이 있어도 응답에서는 `cycle_number`/`cycle_type`으로 정규화됩니다.
>
> 각 `questions[]` 항목에는 0-based `questionIndex`가 추가됩니다 (camelCase).

```json
{
  "stepId": 2,
  "stepTitle": "자연수의 뺄셈",
  "concept": "뺄셈",
  "grade": 4,
  "cycles": [
    { "cycle_number": 1, "cycle_type": "concept", "slides": [] },
    { "cycle_number": 2, "cycle_type": "choice", "questions": [{ "questionIndex": 0 }] }
  ]
}
```

---

### 8-3. 진도 조회

**GET** `/api/v1/learning/math/progress`

```json
{
  "lastStepId": 2, "totalSteps": 12, "completedSteps": 1,
  "progress": [
    { "stepId": 1, "isStepCompleted": true, "completedCycles": [1, 2, 3], "lastAccessedAt": "2026-06-01T10:00:00" }
  ]
}
```

---

### 8-4. 답안 검증

**POST** `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate`

```json
{ "answer": "3" }
```

**Response** `200 OK`

```json
{ "correct": true, "explanation": null }
```

---

### 8-5. 사이클 완료 처리

**POST** `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/complete`

```json
{ "stepId": 2, "cycleNumber": 2, "completedCycles": [1, 2], "isStepCompleted": false }
```

---

### 8-6. 스텝 완료 처리

**POST** `/api/v1/learning/math/steps/{stepId}/complete`

```json
{ "stepId": 2, "isStepCompleted": true, "nextStepId": 3, "nextStepStatus": "AVAILABLE" }
```

---

## 9. 공부방 — 국어 (Learning Korean)

Base path: `/api/v1/learning/korean` · **JWT 필수**

8. 공부방 — 수학과 동일한 엔드포인트 구조입니다.

### 9-1. 로드맵 조회

**GET** `/api/v1/learning/korean/roadmap`

---

### 9-2. 스텝 콘텐츠 조회

**GET** `/api/v1/learning/korean/steps/{stepId}/content`

`answer` 필드는 서버에서 제거됩니다. 호출 시 `lastAccessedAt`이 갱신됩니다. 수학(8-2)과 동일하게 `cycles`는 `cycle_number`, `cycle_type` canonical 키로 정규화됩니다 (`cycle`/`type`은 응답에 포함되지 않음).

**Path Parameter**

| Name | Type | Description |
| --- | --- | --- |
| `stepId` | long | 스텝 ID |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `stepId` | long | 스텝 ID |
| `stepTitle` | string | 스텝 제목 |
| `concept` | string | 학습 개념 |
| `grade` | int | 학년 |
| `passage` | JsonNode | 지문 `{ title, text, image }` |
| `cycles` | array\<JsonNode\> | 사이클 목록 (answer 제거, 키 정규화) |

> `cycle_type` 값은 과목별로 다릅니다 (예: `word_card`, `fact_check`). 키 이름만 수학과 통일합니다.

---

### 9-3. 진도 조회

**GET** `/api/v1/learning/korean/progress`

---

### 9-4. 답안 검증

**POST** `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate`

```json
{ "answer": "외로움" }
```

---

### 9-5. 사이클 완료 처리

**POST** `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/complete`

---

### 9-6. 스텝 완료 처리

**POST** `/api/v1/learning/korean/steps/{stepId}/complete`

---

## 10. 할 일 (Todo)

### 10-1. 할 일 완료 처리

**PATCH** `/api/v1/todos/{todoId}/complete` · **JWT 필수**

- 단일 완료: +1풀 / 오늘 전부 완료 시: +3풀 보너스

**Response** `200 OK`

```json
{
  "todo": {
    "id": 2, "type": "LEARNING", "title": "오늘의 문제 3개 풀기 📚",
    "status": "COMPLETED", "source": "SYSTEM", "learningType": "MATH", "assignedDate": "2026-06-01"
  },
  "singleReward": { "earnedPool": 1, "availablePool": 13, "totalPoolEarned": 43, "levelBefore": 3, "levelAfter": 3 },
  "allCompleteBonus": null,
  "leveledUp": false
}
```

---

### 10-2. Agent 할 일 추가 (Leo 전용)

**POST** `/api/v1/todos` · **X-Internal-Service 인증**

하루 최대 5개(시스템 3 + Agent 2) 제한.

```json
{ "memberId": 1, "title": "수학 문제 한 문제 더 풀기" }
```

**Response** `200 OK` — `TodoResponse`

---

### 10-3. Agent 할 일 수정 (Leo 전용)

**PATCH** `/api/v1/todos/{todoId}` · **X-Internal-Service 인증**

> SYSTEM 할 일 수정 불가 — `400`

```json
{ "memberId": 1, "title": "수정된 할 일 제목" }
```

---

### 10-4. Agent 할 일 삭제 (Leo 전용)

**DELETE** `/api/v1/todos/{todoId}?memberId={memberId}` · **X-Internal-Service 인증**

> SYSTEM 할 일 삭제 불가 — `400`

**Response** `204 No Content`

---

## 11. 친구랑 — 감정 학습 (Mind)

Base path: `/api/v1/mind` · **JWT 필수**

> **저장소**: 완료된 세션만 Firestore `users/{memberId}/mindSessions/{setId}`에 저장됩니다.  
> **흐름**: `POST /scenario`(생성) → 아이가 step1~4 학습 → `POST /sessions`(저장·보상) → `GET /sessions`(히스토리) / `GET /sessions/{sessionId}`(다시보기)  
> **경로 파라미터 `sessionId`**: 시나리오 응답의 `setId`와 동일한 값입니다.  
> **레거시**: `POST /api/v1/emotion/scenarios`는 생성·저장·보상을 한 번에 처리합니다. 신규 앱은 `/api/v1/mind/**` 사용을 권장합니다.  
> **Firestore 오류**: 저장·히스토리 조회 실패 시 `503 Service Unavailable` (빈 목록으로 숨기지 않음). 운영 설정은 [doc/mind/README.md](mind/README.md) 참고.

---

### 11-1. 감정 학습 시나리오 생성

**POST** `/api/v1/mind/scenario`

"이야기 보러 가기!" 클릭 시 호출합니다. AI가 4단계 학습 세트를 생성하고 step3 만화 이미지를 Firebase Storage에 업로드한 뒤 URL을 반환합니다. **이 시점에는 Firestore에 저장하지 않습니다.**

> AI 호출 + Storage 업로드로 응답이 **10~30초** 걸릴 수 있습니다.

**Request Body**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `emotion` | string | X | 감정 지정. 미지정 시 AI가 선택 (`기쁨` / `속상함` / `부끄러움` / `화남` / `실망` / `고마움`) |

```json
{ "emotion": "속상함" }
```

**Response** `200 OK`

| Field | Type | Description |
| --- | --- | --- |
| `setId` | string | 세션 고유 ID (이후 저장·조회에 사용) |
| `emotion` | string | 감정명 |
| `situation` | string | 상황 설명 |
| `learnedExpression` | string | 배울 표현 |
| `isFallback` | boolean | AI 실패 시 폴백 시나리오 여부 |
| `steps` | object | step1~step4 학습 콘텐츠 |

`steps` 구조:

| Step | 주요 필드 |
| --- | --- |
| `step1` | `title`, `expression`, `emotion`, `bodySensation`, `situationExample`, `imageUrl`, `nextButtonText` |
| `step2` | `title`, `visualClue`, `question`, `choices[]`, `imageUrl`, `retryMessage`, `nextButtonText` |
| `step3` | `title`, `comic[]` (`cut`, `text`, `imageUrl`), `question`, `choices[]`, `retryMessage`, `nextButtonText` |
| `step4` | `title`, `leoIntro`, `question`, `choices[]` (`type` 포함), `retryMessage`, `successMessage`, `reward`, `completeButtonText` |

`choices[]` (step2·step3): `{ "id", "text", "isCorrect", "feedback" }`  
`choices[]` (step4): `{ "id", "text", "type", "isCorrect", "feedback" }`  
`reward`: `{ "type": "POOL", "amount": 3 }` (예시)

```json
{
  "setId": "set_20260603_abc123",
  "emotion": "속상함",
  "situation": "친구가 장난 댓글을 달았어요",
  "learnedExpression": "속상해",
  "isFallback": false,
  "steps": {
    "step1": {
      "title": "오늘의 표현",
      "expression": "속상해",
      "emotion": "속상함",
      "bodySensation": "가슴이 먹먹해요",
      "situationExample": "친구가 나를 놀릴 때",
      "imageUrl": "https://storage.googleapis.com/.../step1.png",
      "nextButtonText": "다음"
    },
    "step2": { "title": "...", "choices": [{ "id": "a", "text": "속상한 표정", "isCorrect": true, "feedback": "..." }], "..." : "..." },
    "step3": { "title": "...", "comic": [{ "cut": 1, "text": "...", "imageUrl": "https://..." }], "..." : "..." },
    "step4": { "title": "...", "reward": { "type": "POOL", "amount": 3 }, "..." : "..." }
  }
}
```

---

### 11-2. 학습 완료 세션 저장

**POST** `/api/v1/mind/sessions`

아이가 **step4까지 완료**했을 때 FE가 `POST /scenario`로 받은 세션 전체를 전송합니다. Firestore에 저장하고 감정 학습 보상(풀)을 지급합니다. **히스토리 목록·다시보기는 이 API 호출 이후에만 가능**합니다.

**Request Body**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `setId` | string | O | `POST /scenario` 응답의 `setId` |
| `emotion` | string | O | 감정명 |
| `situation` | string | O | 상황 설명 |
| `learnedExpression` | string | O | 배운 표현 |
| `isFallback` | boolean | X | 폴백 시나리오 여부 (기본 `false`) |
| `steps` | object | O | step1~step4 전체 (`11-1` 응답과 동일 구조) |

**Response** `200 OK`

| Field | Type | Description |
| --- | --- | --- |
| `setId` | string | 저장된 세션 ID |
| `rewardAmount` | int | 지급된 풀 수 (실패 시 `0`, 비치명적) |

```json
{ "setId": "set_20260603_abc123", "rewardAmount": 3 }
```

서버가 `completedAt`(ISO-8601 UTC)을 자동 기록합니다.

**Response** `503 Service Unavailable` — Firestore 저장 실패(인증·네트워크·권한 등)

---

### 11-3. 학습 히스토리 목록 조회

**GET** `/api/v1/mind/sessions`

완료·저장된 세션 목록을 **최신순**(`completedAt` DESC)으로 반환합니다.

> 기본 최대 **20건** 조회 (서버 내부 limit). 쿼리 파라미터는 현재 미지원.

**Response** `200 OK`

| Field | Type | Description |
| --- | --- | --- |
| `sessions` | array | 세션 요약 목록 |
| `totalCount` | int | `sessions` 길이 |

`sessions[]` 항목:

| Field | Type | Description |
| --- | --- | --- |
| `setId` | string | 세션 ID (`GET /sessions/{sessionId}`의 `sessionId`와 동일) |
| `emotion` | string | 감정명 |
| `learnedExpression` | string | 배운 표현 |
| `completedAt` | string | 완료 시각 (ISO-8601) |
| `isFallback` | boolean | 폴백 시나리오 여부 |

```json
{
  "sessions": [
    {
      "setId": "set_20260603_abc123",
      "emotion": "속상함",
      "learnedExpression": "속상해",
      "completedAt": "2026-06-03T02:15:30.123456789Z",
      "isFallback": false
    }
  ],
  "totalCount": 1
}
```

세션이 없으면 `{ "sessions": [], "totalCount": 0 }` 입니다. (Firestore 장애와 구분: 장애 시 `503`)

**Response** `503 Service Unavailable` — Firestore 목록 조회 실패

---

### 11-4. 학습 세션 상세 조회 (다시보기)

**GET** `/api/v1/mind/sessions/{sessionId}`

완료된 세션을 다시 볼 때 사용합니다. step1~step4 전체 콘텐츠를 반환합니다.

**Path Parameter**

| Name | Type | Description |
| --- | --- | --- |
| `sessionId` | string | `setId` (예: `set_20260603_abc123`) |

**Response** `200 OK`

| Field | Type | Description |
| --- | --- | --- |
| `setId` | string | 세션 ID |
| `emotion` | string | 감정명 |
| `situation` | string | 상황 설명 |
| `learnedExpression` | string | 배운 표현 |
| `isFallback` | boolean | 폴백 시나리오 여부 |
| `completedAt` | string | 완료 시각 (ISO-8601) |
| `steps` | object | step1~step4 (`11-1`과 동일 구조) |

```json
{
  "setId": "set_20260603_abc123",
  "emotion": "속상함",
  "situation": "친구가 장난 댓글을 달았어요",
  "learnedExpression": "속상해",
  "isFallback": false,
  "completedAt": "2026-06-03T02:15:30.123456789Z",
  "steps": {
    "step1": { "title": "...", "expression": "속상해", "imageUrl": "https://...", "nextButtonText": "다음" },
    "step2": { "title": "...", "choices": [], "imageUrl": "https://...", "nextButtonText": "다음" },
    "step3": { "title": "...", "comic": [{ "cut": 1, "text": "...", "imageUrl": "https://..." }], "choices": [], "nextButtonText": "다음" },
    "step4": { "title": "...", "leoIntro": "...", "reward": { "type": "POOL", "amount": 3 }, "completeButtonText": "완료" }
  }
}
```

**Response** `404 Not Found` — 해당 회원의 `sessionId` 문서가 없을 때

```json
{ "message": "세션을 찾을 수 없습니다. sessionId=..." }
```

**Response** `503 Service Unavailable` — Firestore 조회 실패(문서 없음과 구분)

---

## 12. 채팅 (Chat)

### 12-1. 채팅 메시지 전송

**POST** `/api/v1/chat/messages` · **JWT 필수**

> sessionId가 신규이면 세션을 자동 생성하고, 사용자·Leo 메시지를 DB에 저장합니다.

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | string | O | 채팅 세션 ID (FE가 UUID 생성) |
| `message` | string | O | 사용자 메시지 |
| `nickname` | string | O | 아동 닉네임 |
| `grade` | int | O | 학년 (3~6) |
| `interests` | array\<Interest\> | O | 관심사 목록 (1~3개) |

**Response** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | string | 채팅 세션 ID |
| `reply` | string \| null | Leo의 텍스트 응답 |
| `cta` | object \| null | 앱 내 이동 힌트. 예) `{ "type": "NAVIGATE", "target": "MATH_STUDY", "stepId": 3 }` |
| `todosCreated` | array\<long\> \| null | Leo가 이번 응답에서 생성한 Todo ID 목록 |

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "reply": "수학 공부 같이 해보자!",
  "cta": { "type": "NAVIGATE", "target": "MATH_STUDY", "stepId": 3 },
  "todosCreated": [42]
}
```

---

## 13. 내부 전용 API (Internal — Leo)

> **인증**: `X-Internal-Service: <INTERNAL_SERVICE_TOKEN>` 헤더 필수. JWT 불필요.

### 13-1. 회원 프로필 조회

**GET** `/api/v1/members/{memberId}/profile`

```json
{ "memberId": 1, "nickname": "민준이", "grade": 4, "interests": ["ANIMAL", "SPACE"] }
```

---

### 13-2. 수학 스텝 전체 목록

**GET** `/api/v1/learning/math/steps?memberId={memberId}`

```json
{
  "grade": 4, "totalSteps": 12, "completedSteps": 2,
  "steps": [
    { "stepId": 1, "stepNumber": 1, "stepTitle": "자연수의 덧셈", "concept": "덧셈",
      "status": "COMPLETED", "completedCycles": [1, 2, 3] }
  ]
}
```

---

### 13-3. 국어 스텝 전체 목록

**GET** `/api/v1/learning/korean/steps?memberId={memberId}`

12-2와 동일 구조.

---

### 13-4. 통합 학습 진도

**GET** `/api/v1/members/{memberId}/learning-progress`

```json
{
  "memberId": 1,
  "math": { "grade": 4, "totalSteps": 12, "completedSteps": 2, "steps": ["..."] },
  "korean": { "grade": 4, "totalSteps": 10, "completedSteps": 1, "steps": ["..."] }
}
```

---

### 13-5. 채팅 세션 메시지 목록

**GET** `/api/v1/chat/sessions/{sessionId}/messages`

```json
{
  "sessionId": "550e8400-...", "memberId": 1,
  "messages": [
    { "id": 1, "role": "USER", "content": "수학 도와줘", "createdAt": "2026-06-01T10:00:00" },
    { "id": 2, "role": "ASSISTANT", "content": "좋아, 같이 해보자!", "createdAt": "2026-06-01T10:00:05" }
  ]
}
```

---

### 13-6. 기간별 채팅 메시지 조회

**GET** `/api/v1/chat/messages?memberId={memberId}&from={from}&to={to}`

`from`, `to`: ISO-8601 datetime (예: `2026-05-25T00:00:00`)

```json
{
  "memberId": 1, "from": "2026-05-25T00:00:00", "to": "2026-05-31T23:59:59",
  "messages": [
    { "id": 1, "sessionId": "550e8400-...", "role": "USER", "content": "안녕", "createdAt": "2026-05-28T09:00:00" }
  ]
}
```

---

### 13-7. 채팅 세션 수정

**PATCH** `/api/v1/chat/sessions/{sessionId}`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | X | 세션 제목 |
| `status` | SessionStatus | X | `ACTIVE` \| `CLOSED` |

```json
{ "title": "수학 이야기", "status": "ACTIVE" }
```

**Response** `200 OK`

```json
{
  "sessionId": "550e8400-...", "memberId": 1,
  "title": "수학 이야기", "status": "ACTIVE", "updatedAt": "2026-06-01T10:05:00"
}
```

---

### 13-8. 수학 concept 목록 (LRN_13)

**GET** `/api/v1/learning/math/concepts?memberId={memberId}`

```json
{
  "grade": 4,
  "concepts": [
    { "concept": "덧셈", "stepId": 1, "stepNumber": 1, "stepTitle": "자연수의 덧셈" }
  ]
}
```

---

### 13-9. 수학 concept별 lesson 상태 (LRN_14/15/16)

**GET** `/api/v1/learning/math/concepts/lesson-status?memberId={memberId}&concept={concept}`

| 값 | Leo 동작 |
|----|----------|
| `AVAILABLE` / `IN_PROGRESS` / `COMPLETED` | `stepId`로 이동 |
| `LOCKED` | `currentAvailableStepId`로 이동 유도 |
| `NOT_FOUND` | 안내 메시지 출력 |

```json
{
  "concept": "뺄셈", "lessonStatus": "IN_PROGRESS",
  "stepId": 2, "stepTitle": "자연수의 뺄셈",
  "currentAvailableStepId": null, "currentAvailableStepTitle": null
}
```

---

### 13-10. 현재 국어 lesson (LRN_32/33)

**GET** `/api/v1/learning/korean/lessons/current?memberId={memberId}`

우선순위: `IN_PROGRESS` → `AVAILABLE` → 첫 번째 스텝

```json
{ "stepId": 5, "stepTitle": "낱말 익히기", "concept": "어휘", "lessonStatus": "IN_PROGRESS" }
```

---

### 13-11. 학습 리포트 집계 (AI → BE)

**GET** `/api/v1/report/learning-summary` · **X-Internal-Service 인증**

주간 성장 리포트 생성 전, 해당 주간의 학습·할 일 집계를 조회합니다.

> **주간 범위**: `week_start` / `week_end`는 **AI가 정한 기간**을 사용합니다.  
> BE는 `weekly-report`·`weekly-safety-report` 콜백의 날짜를 그대로 저장하며, 별도로 주를 재계산하지 않습니다.  
> 이 API도 AI가 정한 `from`·`to`(보통 콜백과 동일한 `week_start`·`week_end`)를 쿼리로 넘기면, 응답 `weekStart`·`weekEnd`에 **실제 집계에 쓴 inclusive 범위**(`yyyy-MM-dd`)를 돌려줍니다.

#### Query

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `memberId` | long | O | 회원 ID |
| `from` | string (date) | O | 집계 시작일 (`yyyy-MM-dd`, inclusive) |
| `to` | string (date) | O | 집계 종료일 (`yyyy-MM-dd`, inclusive) |

- `from`이 `to`보다 늦으면 **400**

#### Response `200 OK`

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

### 13-12. 주간 배치 트리거 (BE → AI)

> **BE 내부 동작 — 외부 노출 없음**  
> 매주 **월요일 01:00 KST**에 온보딩 완료 회원 1인당 1건씩 AI에 호출.  
> 집계 주간은 **직전 완료 주(월~일)** (`week_start`=그 주 월요일, `week_end`=그 주 일요일).

#### 13-12-1. 주간 안전 보고서

**AI 엔드포인트**: `POST /api/v1/ai/batch/weekly-safety`

콜백: `POST /api/v1/weekly-safety-report`

#### 13-12-2. 주간 성장 리포트 (부모용)

**AI 엔드포인트**: `POST /api/v1/ai/report/weekly`

콜백: `POST /api/v1/weekly-report`

#### Request (공통, snake_case)

```json
{ "member_id": 1, "week_start": "2026-05-26", "week_end": "2026-06-01" }
```

> `AI_SERVICE_ENABLED=true`일 때만 실제 호출. 로컬 기본값은 `false`.

---

### 13-13. 주간 안전 보고서 수신 (AI → BE 콜백)

**POST** `/api/v1/weekly-safety-report` · **X-Internal-Service 인증**

챗 안전 신호 전용. 성장 리포트(`weekly-report`)와 **별도** 배치·저장.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `member_id` | long | O | 보고서 대상 회원 ID |
| `week_start` | string (date) | O | 주 시작일 (`yyyy-MM-dd`) |
| `week_end` | string (date) | O | 주 종료일 |
| `safety_signal` | string | O | `GREEN` \| `YELLOW` \| `RED` |
| `score` | int | O | 안전 점수 (0~3 GREEN, 4~7 YELLOW, 8+ RED) |
| `reason_summary` | string | X | Gemini 생성 한두 문장 요약 |

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

**Response** `200 OK`

```json
{ "received": true }
```

---

### 13-14. 주간 성장 리포트 수신 (AI → BE 콜백)

**POST** `/api/v1/weekly-report` · **X-Internal-Service 인증**

AI가 `POST /api/v1/ai/report/weekly` 등으로 생성한 **4섹션 JSON**을 BE에 저장합니다.  
`parent_mission`은 weekly 생성 시 함께 포함되며, 부모의 `미션 받기` 클릭 시 reveal 됩니다.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `member_id` | long | O | 리포트 대상 회원 ID |
| `week_start` | string (date) | O | 주 시작일 |
| `week_end` | string (date) | O | 주 종료일 |
| `sections` | string | O | 4섹션 JSON **문자열** (아래 구조) |

`sections` JSON 구조 (AI 생성, snake_case):

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

**Request 예시**

```json
{
  "member_id": 1,
  "week_start": "2026-05-26",
  "week_end": "2026-06-01",
  "sections": "{\"growth_summary\":\"...\",\"math\":{\"well_done\":\"...\",\"struggled\":\"...\"},\"korean\":{\"well_done\":\"...\",\"struggled\":\"...\"},\"parent_mission\":{\"praise\":\"...\",\"activity\":\"...\"}}"
}
```

**Response** `200 OK`

```json
{ "received": true }
```

> 동일 `member_id` + `week_start` 재수신 시 `sections` 업데이트 및 `missionRevealed` 초기화.

---

## 공통 에러 응답

| Status | 설명 |
|--------|------|
| `400 Bad Request` | 요청 파라미터/바디 유효성 오류 |
| `401 Unauthorized` | 토큰 누락 또는 만료 |
| `403 Forbidden` | 권한 없음 |
| `404 Not Found` | 존재하지 않는 리소스 |
| `500 Internal Server Error` | 서버 오류 |

```json
{ "error": "Unauthorized" }
```

---

*최종 수정: 2026-06-03*
