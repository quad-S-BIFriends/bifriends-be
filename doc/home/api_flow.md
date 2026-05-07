# 홈 화면 API 연동 흐름

> 대상: 홈 화면을 구현하는 클라이언트(Flutter) 개발자  
> 목적: 홈 화면에서 발생하는 사용자 인터랙션별 API 호출 패턴 정의

---

## 전체 흐름 개요

홈 화면에서 발생하는 API 호출은 크게 세 가지 시나리오로 나뉜다.

```
① 앱 오픈 / 홈 탭 진입         → GET /api/v1/home
② 할 일 클릭 (탭 이동 타입)     → 클라이언트 라우팅만, API 없음
                                   복귀 시 ① 재실행
③ 할 일 완료 처리               → PATCH /api/v1/todos/{id}/complete
```

---

## ① 앱 오픈 / 홈 탭 진입

### 언제 호출하나

앱을 열 때, 또는 바텀 내비게이션의 홈 탭을 탭할 때마다 호출한다.  
다른 탭(배움, 마음, 챗봇)에서 홈으로 돌아올 때도 호출한다.

### 요청

```
GET /api/v1/home
Authorization: Bearer {accessToken}
```

### 응답

```json
{
  "member": {
    "nickname": "서윤"
  },
  "greeting": {
    "type": "STREAK",
    "streakDays": 4,
    "message": "우와 서윤! 진짜 잘하고 있다 😆"
  },
  "stats": {
    "level": 3,
    "availablePool": 25,
    "totalPoolEarned": 120,
    "streakDays": 4,
    "currentLevelProgress": 5,
    "totalPoolForCurrentLevelUp": 20,
    "poolNeededForNextLevel": 15
  },
  "attendance": {
    "isFirstAttendanceToday": true,
    "streakDays": 4,
    "reward": {
      "earnedPool": 5,
      "availablePool": 25,
      "totalPoolEarned": 120,
      "levelBefore": 3,
      "levelAfter": 3,
      "leveledUp": false
    }
  },
  "todos": [
    {
      "id": 101,
      "type": "CHAT",
      "title": "레오랑 이야기하기 🗣️",
      "status": "PENDING",
      "estimatedTimeSec": 60,
      "source": "SYSTEM",
      "learningType": null,
      "assignedDate": "2026-05-07"
    },
    {
      "id": 102,
      "type": "LEARNING",
      "title": "오늘의 문제 3개 풀기 📚",
      "status": "PENDING",
      "estimatedTimeSec": 180,
      "source": "SYSTEM",
      "learningType": "MATH",
      "assignedDate": "2026-05-07"
    },
    {
      "id": 103,
      "type": "EMOTION",
      "title": "친구 기분 알아보기 💌",
      "status": "COMPLETED",
      "estimatedTimeSec": 120,
      "source": "SYSTEM",
      "learningType": null,
      "assignedDate": "2026-05-07"
    }
  ]
}
```

### 클라이언트 처리

| 응답 필드 | 화면 영역 | 설명 |
|---|---|---|
| `greeting.message` | 인사 영역 | Leo 인사 메시지 텍스트 |
| `greeting.streakDays` | 상태 영역 | 연속 접속 일수 표시 |
| `stats.level` | 상단 영역 | 현재 레벨 표시 |
| `stats.availablePool` | 상태 영역 | 보유 풀 개수 표시 |
| `stats.currentLevelProgress` | 프로그레스 바 | 현재 레벨 내 진행 풀 |
| `stats.totalPoolForCurrentLevelUp` | 프로그레스 바 | 레벨업 기준 총 풀 |
| `todos[]` | 할 일 리스트 | 각 항목의 `status`로 체크 여부 표시 |
| `attendance.reward` | (애니메이션) | `isFirstAttendanceToday == true`이면 보상 획득 애니메이션 표시 |

### 출석 처리 멱등성

같은 날 여러 번 호출해도 출석 보상은 하루에 한 번만 지급된다.  
재방문 시 `attendance.isFirstAttendanceToday == false`, `attendance.reward == null`이 반환된다.

```
1번째 홈 진입: isFirstAttendanceToday = true,  reward = { earnedPool: 5, ... }
2번째 홈 진입: isFirstAttendanceToday = false, reward = null
N번째 홈 진입: isFirstAttendanceToday = false, reward = null
```

### 인사 메시지 유형별 동작

| `greeting.type` | 표시 조건 | 예시 메시지 |
|---|---|---|
| `FIRST_LOGIN` | 앱 최초 접속 | "안녕, 서윤! 만나서 반가워 🙌" |
| `COMEBACK_SHORT` | 2~3일 만에 복귀 | "와 서윤! 또 만나서 좋다 😆" |
| `COMEBACK_LONG` | 4일 이상 만에 복귀 | "우와 서윤! 오랜만이다 😆" |
| `STREAK` | 연속 출석 중 | streak 일수에 따라 메시지 변화 |

---

## ② 할 일 클릭 — 탭 이동 타입 (CHAT / LEARNING / EMOTION)

### 동작

할 일의 `type`에 따라 해당 탭으로 이동한다. **이 시점에 API 호출은 없다.**

| `type` | `learningType` | 이동 화면 |
|---|---|---|
| `CHAT` | — | 챗봇 탭 |
| `LEARNING` | `MATH` | 수학 공부방 |
| `LEARNING` | `LANGUAGE` | 국어 공부방 |
| `LEARNING` | `null` (일요일) | 전체 공부방 목록 |
| `EMOTION` | — | 마음 탭 |

### 홈으로 복귀 시

해당 탭에서의 활동(퀴즈 풀기, 마음 시나리오 완료 등)이 끝나고 홈으로 돌아올 때 `GET /api/v1/home`을 **재호출**한다.

재호출 이유: 배움/마음 탭에서 풀 보상이 발생했을 수 있고, 할 일 상태(`COMPLETED`)도 서버에서 변경됐을 수 있다. 홈 재진입 시 최신 상태를 받아야 프로그레스 바와 체크 상태가 정확하게 표시된다.

```
배움 탭 진입 → 퀴즈 완료 (서버에서 풀 지급, todo COMPLETED 처리)
             → 홈으로 복귀
             → GET /api/v1/home 재호출
             → 갱신된 stats.availablePool, todos[].status = COMPLETED 수신
```

---

## ③ 할 일 직접 완료 처리

> ⚠️ 현재 컨트롤러 레이어 미구현. 서비스 로직(`TodoService.completeTodo()`)은 준비되어 있음.

### 언제 호출하나

`CUSTOM` 타입 할 일처럼 탭 이동 없이 홈 화면에서 직접 완료 버튼을 탭할 때 호출한다.

### 요청

```
PATCH /api/v1/todos/{id}/complete
Authorization: Bearer {accessToken}
```

### 응답

```json
{
  "todo": {
    "id": 104,
    "type": "CUSTOM",
    "title": "오늘 일기 쓰기",
    "status": "COMPLETED",
    ...
  },
  "singleReward": {
    "earnedPool": 1,
    "availablePool": 26,
    "totalPoolEarned": 121,
    "levelBefore": 3,
    "levelAfter": 3,
    "leveledUp": false
  },
  "allCompleteBonus": {
    "earnedPool": 3,
    "availablePool": 29,
    "totalPoolEarned": 124,
    "levelBefore": 3,
    "levelAfter": 4,
    "leveledUp": true
  },
  "leveledUp": true
}
```

### 응답 필드 설명

| 필드 | 타입 | 설명 |
|---|---|---|
| `todo` | TodoResponse | 완료 처리된 할 일 |
| `singleReward` | RewardResult | 단일 완료 보상 (+1풀) |
| `allCompleteBonus` | RewardResult? | 오늘 모든 할 일 완료 시 보너스 (+3풀). 아직 남은 할 일이 있으면 `null` |
| `leveledUp` | Boolean | `singleReward` 또는 `allCompleteBonus`에서 레벨업 발생 여부 |

### 클라이언트 처리

```
PATCH 성공 응답 수신
  → todos 목록에서 해당 항목 status = COMPLETED 로 갱신
  → stats.availablePool = allCompleteBonus?.availablePool ?? singleReward.availablePool 로 갱신
  → leveledUp == true 이면 레벨업 애니메이션 표시
  → allCompleteBonus != null 이면 전체 완료 축하 애니메이션 표시
```

> **프로그레스 바 갱신**: 현재 `PATCH` 응답에 `currentLevelProgress`, `poolNeededForNextLevel`이 포함되지 않는다. 완료 후 정확한 프로그레스 바 갱신이 필요하면 `GET /api/v1/home`을 재호출하거나, `TodoCompleteResult`에 `UserStatsResponse`를 추가하는 방향으로 확장한다.

---

## 전체 흐름 요약

```
앱 오픈
  └─ GET /api/v1/home ──────────────────────────────────────────────┐
       ├─ 인사 메시지 + 레벨/풀/streak + 할 일 목록 수신             │
       └─ 출석 체크 (오늘 첫 방문이면 streak 갱신 + 보상 지급)       │
                                                                    │
할 일 탭 클릭 (CHAT/LEARNING/EMOTION)                               │
  └─ 클라이언트 라우팅 → 해당 탭 화면                                │
       └─ 탭에서 활동 후 홈 복귀                                     │
            └─ GET /api/v1/home 재호출 ──────────────────────────── ┘

할 일 완료 버튼 탭 (CUSTOM 등)
  └─ PATCH /api/v1/todos/{id}/complete
       ├─ 응답으로 보상/레벨업 정보 수신
       └─ 클라이언트 로컬 상태 갱신 (or 홈 재조회)
```
