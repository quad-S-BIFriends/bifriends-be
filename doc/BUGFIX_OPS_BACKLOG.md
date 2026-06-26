# 버그·운영 수정 백로그 (2순위)

백엔드 코드 리뷰 및 운영 이슈 분석에서 도출한 **수정 예정 항목**을 정리한 문서입니다.  
구현 전 설계·우선순위·배포 시 주의사항을 함께 기록합니다.

> **범위:** `bifriends-be` 중심. `bifriends-ai` 최적화·emotion/mind API 통합·부모 PIN 보안 등은 별도 백로그.

---

## 요약 (우선순위)

| 순위 | 이슈 | 지금 터지나? | 수정 난이도 | 권장 |
|------|------|-------------|------------|------|
| **1** | Firestore `database-id` 설정 갭 | prod 친구랑/Firestore 연동 | 매우 낮음 | **먼저** |
| **2** | `ChatService` 트랜잭션 경계 | 동시 채팅 증가 시 커넥션 풀 고갈 | 낮음 | 두 번째 |
| **3** | `ReportService` N+1 | 리포트 주 수 적으면 미미 | 낮음 | 여유 있을 때 |
| **4** | `getLearnedExpressions` 전체 스캔 | 세션 수 적으면 OK | 중~높음 | 이슈 등록 후 중기 |

---

## 1. Firestore `database-id` 설정 갭

### 현상

- Firebase 콘솔·로컬 개발은 **이름 지정 DB** `bifriends`를 사용
- BE는 기본값 `(default)` DB에 연결 시도 가능
- 관련 로그 예: `The database (default) does not exist for project bifriends-5df72`

### 원인

`FirestoreService`는 `firebase.firestore.database-id`를 읽습니다.

```kotlin
// infrastructure/firebase/FirestoreService.kt
@Value("\${firebase.firestore.database-id:(default)}") private val databaseId: String
```

| 위치 | `FIRESTORE_DATABASE_ID` / yaml | 실제 BE에 전달 |
|------|-------------------------------|----------------|
| 로컬 `docker-compose.yml` | `FIRESTORE_DATABASE_ID: bifriends` ✅ | **❌** `application.yml`에 매핑 없음 |
| `deploy/docker-compose.yml` | 없음 | **❌** `(default)` 사용 |
| `application.yml` | `firebase.firestore.database-id` 항목 없음 | 기본 `(default)` |

Spring Boot env 자동 바인딩:

- `FIREBASE_FIRESTORE_DATABASE_ID` → `firebase.firestore.database-id` ✅
- `FIRESTORE_DATABASE_ID` → **자동 매핑 안 됨** ❌

로컬 compose에 `FIRESTORE_DATABASE_ID`를 넣어도, yaml 매핑이 없으면 **효과 없음**.

### 영향 범위

Firestore를 쓰는 기능 전반 (PostgreSQL과 별도 저장소):

| 기능 | 호출 경로 |
|------|-----------|
| 친구랑 세션 저장/목록/상세 | `MindSessionService` |
| 시나리오 중복 방지 | `MindScenarioService`, `EmotionScenarioService` |
| 주간 리포트 학습 요약 | `ReportService.getLearningSummary` |

### 권장 수정

**① `src/main/resources/application.yml`**

```yaml
firebase:
  firestore:
    database-id: ${FIRESTORE_DATABASE_ID:(default)}
```

**② `deploy/docker-compose.yml` — `bifriends-be.environment`**

```yaml
FIRESTORE_DATABASE_ID: ${FIRESTORE_DATABASE_ID:-bifriends}
```

**③ `deploy/.env.example`**

```bash
FIRESTORE_DATABASE_ID=bifriends
```

(로컬 `docker-compose.yml`은 이미 `FIRESTORE_DATABASE_ID`를 넘기고 있으므로 ①만 추가해도 로컬은 동작)

### 배포 시 주의 (VM)

- CI는 `main` push 시 **이미지만** 재빌드. VM의 `/app/docker-compose.yml`, `/app/.env`는 **자동 갱신되지 않음**
- 배포 후 수동 반영:
  1. `/app/.env`에 `FIRESTORE_DATABASE_ID=bifriends` 추가
  2. compose env 반영
  3. `docker compose up -d bifriends-be`

### 검증

```bash
sudo docker logs bifriends-be 2>&1 | grep -i firestore
```

기대 로그:

- `[Firestore] named database 사용 — databaseId=bifriends`
- `[Firestore] connectivity check OK — databaseId=bifriends`

---

## 2. `ChatService` — AI HTTP 호출이 `@Transactional` 안에 있음

### 현상

`sendMessage()` 한 메서드 안에서 DB 저장 → AI HTTP → DB 저장이 **하나의 트랜잭션**으로 묶여 있음.

```kotlin
// domain/chat/service/ChatService.kt
@Transactional
fun sendMessage(memberId: Long, request: ChatMessageRequest): ChatMessageResponse {
    // 1. 세션 조회/생성
    // 2. user 메시지 save
    // 3. aiChatClient.sendChat(...)   ← 외부 HTTP
    // 4. assistant 메시지 save
}
```

AI 클라이언트 read timeout: **최대 3분**

```kotlin
// infrastructure/ai/AiClientConfig.kt
setReadTimeout(Duration.ofMinutes(3))
```

### 왜 문제인가

`@Transactional`이 켜지면 **DB 커넥션을 트랜잭션 종료까지 점유**합니다.

```
[요청] → 커넥션 획득
  → user INSERT
  → AI HTTP 대기 (수 초 ~ 180초)   ← 이 동안 커넥션 점유
  → assistant INSERT
[응답] → 커넥션 반환
```

| 상황 | 결과 |
|------|------|
| 동시 채팅 N건, AI 응답 지연 | 커넥션 N개가 AI 대기 동안 묶임 |
| Hikari 기본 pool ~10 | 홈·학습·로그인 등 다른 API도 커넥션 대기 → 지연/타임아웃 |
| AI 호출 실패 | **user 메시지까지 롤백** (일관성은 좋으나 UX·패턴 모두 불리) |

외부 I/O(AI, HTTP, 메시지 큐)는 트랜잭션 **밖**에서 호출하는 것이 원칙.

### 권장 수정

**3단계로 분리:**

```
① @Transactional  saveUserMessage()      → commit, 커넥션 반환
② (트랜잭션 없음) aiChatClient.sendChat()
③ @Transactional  saveAssistantMessage() → commit
```

구현 옵션:

- `sendMessage`에서 private `@Transactional` 메서드 2개 호출 (같은 클래스면 프록시 이슈 → `self` 주입 또는 `TransactionTemplate`)
- 또는 `ChatMessagePersistenceService` 등으로 DB 단계만 분리

### 트레이드오프

| | 현재 | 분리 후 |
|---|------|---------|
| AI 실패 | user 메시지도 롤백 | **user 메시지는 DB에 남음** |
| AI 성공 후 assistant 저장 실패 | 전체 롤백 | FE는 reply 수신, DB에 assistant 없음 (드묾) |
| 커넥션 | AI 대기 전체 구간 점유 | **짧게만 사용** |

채팅 UX상 “내 말은 남고 AI만 실패”가 더 자연스러운 경우가 많음.

### 수정 대상 파일

- `domain/chat/service/ChatService.kt` (필수)
- (선택) 단위/통합 테스트 — AI mock으로 단계별 DB 상태 검증

### 검증

- 동시 채팅 부하 테스트 시 Hikari `threads awaiting connection` 증가 여부
- AI 500 시 user 메시지 DB 잔존 여부 (의도한 동작인지 확인)
- **정량 비교 테스트·STAR 결과:** `doc/chat/hikari_pool_test.md`

---

## 3. `ReportService` — 리포트 목록 N+1

### 현상

`getReports()`가 주간 리포트마다 `findSafetySignal()`을 호출하고, 그 안에서 **row마다 SELECT 1회**.

```kotlin
// domain/report/service/ReportService.kt
fun getReports(memberId: Long): ReportListResponse {
    val reports = weeklyReportRepository
        .findAllByMemberIdOrderByWeekStartDesc(memberId)
        .map { report ->
            ReportSummaryItem(
                ...
                safetySignal = findSafetySignal(memberId, report.weekStart),  // N회
            )
        }
}

private fun findSafetySignal(memberId: Long, weekStart: LocalDate): SafetySignal {
    return weeklySafetyReportRepository.findByMemberIdAndWeekStart(memberId, weekStart)
        ?.safetySignal ?: SafetySignal.GREEN
}
```

**쿼리 수:** `1 + N` (N = 해당 회원의 주간 리포트 개수)

| 리포트 수 | 쿼리 |
|-----------|------|
| 4주 | 5 |
| 52주 (1년) | 53 |

`getReportDetail()`의 `findChatSafety`는 **리포트 1건** 조회라 N+1 아님.

### 권장 수정

한 번에 safety report를 가져와 메모리에서 lookup:

```kotlin
fun getReports(memberId: Long): ReportListResponse {
    val weeklyReports = weeklyReportRepository
        .findAllByMemberIdOrderByWeekStartDesc(memberId)
    val safetyByWeek = weeklySafetyReportRepository
        .findAllByMemberIdOrderByWeekStartDesc(memberId)  // 기존 메서드 활용 또는 추가
        .associateBy { it.weekStart }

    val reports = weeklyReports.map { report ->
        ReportSummaryItem(
            ...
            safetySignal = safetyByWeek[report.weekStart]?.safetySignal
                ?: SafetySignal.GREEN,
        )
    }
    return ReportListResponse(reports = reports)
}
```

또는 `findAllByMemberIdAndWeekStartIn(memberId, weekStarts)` repository 메서드 추가.

### 수정 대상 파일

- `domain/report/service/ReportService.kt`
- (필요 시) `domain/report/repository/WeeklySafetyReportRepository.kt`

### 검증

- Hibernate SQL 로그 또는 테스트에서 `getReports` 호출 시 쿼리 수 2회 이하 확인

---

## 4. `getLearnedExpressions` — Firestore 전체 컬렉션 스캔

### 현상

시나리오 중복 방지를 위해 회원의 `mindSessions` **서브컬렉션 전체**를 읽음.

```kotlin
// infrastructure/firebase/FirestoreService.kt
fun getLearnedExpressions(memberId: Long): List<String> {
    return try {
        mindSessionsCollection(memberId)
            .get()   // 전체 문서 fetch
            ...
            .mapNotNull { it.getString("learnedExpression") }
    } catch (e: Exception) {
        emptyList()   // 실패 시 조용히 빈 목록 → 중복 방지 무력화
    }
}
```

### 호출처

| 서비스 | 시점 |
|--------|------|
| `MindScenarioService` | 친구랑 시나리오 생성 요청마다 |
| `EmotionScenarioService` | 감정 시나리오 생성 |
| `ReportService` | 주간 리포트 `learningSummary` 생성 |

### 문제

| 항목 | 설명 |
|------|------|
| 비용 | 세션 100개면 요청마다 100문서 읽기 |
| 실패 처리 | `emptyList()` → 같은 `learnedExpression` 재생성 가능 |
| 확장성 | 세션 증가 시 선형 비용 |

### 권장 방향 (중기)

단기(세션 수 적음): 모니터링만  
중기 옵션:

1. Firestore `users/{memberId}` 문서에 `learnedExpressions: string[]` 유지 (세션 저장 시 append)
2. PostgreSQL에 learned expression sync 테이블
3. Firestore 쿼리로 `learnedExpression` 필드만 projection (전체 문서보다는 나음, 인덱스 검토 필요)

### 수정 대상 파일

- `infrastructure/firebase/FirestoreService.kt`
- `domain/mind/service/MindScenarioService.kt`
- `domain/emotion/service/EmotionScenarioService.kt`
- (스키마 변경 시) 세션 저장 로직 `saveMindSession` 호출부

---

## 권장 작업 순서

1. **Firestore `database-id`** — yaml + deploy compose + `.env.example` + VM 수동 반영
2. **`ChatService` 트랜잭션 분리** — 코드 변경, 배포는 이미지 재빌드만
3. **`ReportService` N+1** — 코드 변경
4. **`getLearnedExpressions`** — 요구사항·데이터 모델 합의 후 별도 태스크

---

## 관련 파일 경로

| 목적 | 경로 |
|------|------|
| 채팅 서비스 | `src/main/kotlin/.../domain/chat/service/ChatService.kt` |
| AI 클라이언트 | `src/main/kotlin/.../infrastructure/ai/AiClientConfig.kt` |
| Firestore | `src/main/kotlin/.../infrastructure/firebase/FirestoreService.kt` |
| 리포트 | `src/main/kotlin/.../domain/report/service/ReportService.kt` |
| 앱 설정 | `src/main/resources/application.yml` |
| 로컬 compose | `docker-compose.yml` |
| 배포 compose (레포) | `deploy/docker-compose.yml` |
| 배포 env 예시 | `deploy/.env.example` |
| **운영 VM compose** | `/app/docker-compose.yml` (레포와 별도, 수동 동기화) |

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-06-10 | 초안 작성 (코드 리뷰·운영 딥다이브 기반) |
