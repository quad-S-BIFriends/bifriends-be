# 채팅 트랜잭션 분리 — Hikari 커넥션 풀 부하 테스트

> **목적:** `ChatService.sendMessage` 트랜잭션 경계 수정 전·후의 DB 커넥션 대기를 **정량 비교**하고, STAR(상황·과제·행동·결과) 형식으로 기록한다.  
> **관련 코드:** `ChatService.kt`, `ChatMessageWriteService.kt`, `ChatHikariPoolComparisonTest.kt`  
> **관련 백로그:** `doc/BUGFIX_OPS_BACKLOG.md` §2

---

## 1. 테스트를 진행하게 된 배경

### 1.1 체감 문제

채팅(Leo) 사용 시 **응답이 전반적으로 느리게 느껴지는 경우**가 있었다. 나뿐만 아니라 팀원들도 채팅의 응답속도가 느리다는 반응이 있었다. 체감 수치가 느린건지 아니면 구조적 혹은 코드 상의 문제가 있는지 확인하고 싶었다.

### 1.2 구조적 원인 분석

`ChatService.sendMessage()`가 클래스 단위 `@Transactional(readOnly = true)` 안에서 동작하면서, **외부 AI HTTP 호출(최대 3분 read timeout) 동안에도 DB 커넥션을 붙잡는** 패턴이었다.

```
[수정 전 — 개념도]

요청 시작 → DB 커넥션 획득
  → user 메시지 INSERT
  → AI HTTP 대기 (수 초 ~ 수십 초)   ← 이 구간 동안 커넥션 점유
  → assistant 메시지 INSERT
요청 종료 → 커넥션 반환
```

Spring `@Transactional`은 트랜잭션이 끝날 때까지 **같은 JDBC 커넥션**을 붙잡는다. 외부 I/O(AI 호출)를 트랜잭션 안에 두면 Hikari 풀의 슬롯이 오래 점유되고, 동시 요청이 늘면 **커넥션 대기(threads awaiting connection)** 가 발생한다.

### 1.3 코드 수정

DB 쓰기와 AI 호출을 분리했다.


| 단계  | 처리                                               | 트랜잭션                              |
| --- | ------------------------------------------------ | --------------------------------- |
| 1   | `ChatMessageWriteService.saveUserMessage()`      | 짧은 `@Transactional` → 커밋 후 커넥션 반환 |
| 2   | `AiChatClient.sendChat()`                        | 트랜잭션 없음 (`NOT_SUPPORTED`)         |
| 3   | `ChatMessageWriteService.saveAssistantMessage()` | 짧은 `@Transactional`               |


```kotlin
// ChatService.kt — 요약
@Transactional(propagation = Propagation.NOT_SUPPORTED)
fun sendMessage(...) {
    chatMessageWriteService.saveUserMessage(...)
    val aiResponse = aiChatClient.sendChat(...)
    aiResponse.reply?.let { chatMessageWriteService.saveAssistantMessage(...) }
}
```

### 1.4 정량 테스트가 필요했던 이유

- 체감 개선만으로는 **“왜 빨라졌는지”** 설명하기 어렵다.
- 운영 장애 직전에 터지는 유형(풀 고갈)이라, **동시 부하 조건**에서 수치로 남겨 두는 것이 안전하다.
- 발표·리뷰·회고에서 STAR 형식의 **재현 가능한 근거**가 필요했다.

---

## 2. 테스트 환경 설정과 배경 설명

### 2.1 무엇을 검증하고, 무엇은 검증하지 않는가


| 검증함                                                       | 검증하지 않음                                      |
| --------------------------------------------------------- | -------------------------------------------- |
| `ChatService` + JPA로 `chat_sessions` / `chat_messages` 저장 | HTTP `POST /api/v1/chat/messages` (컨트롤러·JWT) |
| Hikari `threadsAwaitingConnection` (커넥션 대기 스레드 수)         | 실제 bifriends-ai HTTP 응답 시간                   |
| 부하 중 다른 DB 작업 지연 (프로브 쿼리)                                 | PostgreSQL·Firestore                         |
| 수정 전 패턴 재현 (`LegacyChatTransactionSimulator`)             | E2E(앱 → API → AI → DB) 전 구간                  |


**HTTP를 빼는 이유:** 병목이 **DB 커넥션 점유 시간**이기 때문이다. 컨트롤러를 거치면 JWT·직렬화만 추가되고, **저장·트랜잭션 경계는 `ChatService`와 동일**하다.

`**ChatService`를 직접 호출하면:** 앱이 채팅 보낼 때 서버 내부에서 하는 일과 같이, 세션 생성·user/assistant 메시지 INSERT가 실행된다.

### 2.2 용어 설명


| 용어                            | 설명                                                                       |
| ----------------------------- | ------------------------------------------------------------------------ |
| **JDBC 커넥션**                  | 애플리케이션이 DB와 말할 때 쓰는 “전화선” 하나. 매 요청마다 새로 열면 느려서 풀에서 빌려 쓴다.                |
| **HikariCP**                  | Spring Boot 기본 커넥션 풀. 미리 연결을 만들어 두고 돌려 쓴다.                               |
| **커넥션 풀 (pool)**              | 동시에 DB에 붙을 수 있는 연결 **슬롯** 모음. `maximum-pool-size`가 상한.                   |
| **threadsAwaitingConnection** | 풀에 빈 슬롯이 없어 **커넥션을 받을 때까지 줄 서 있는** 스레드 수. 이 값이 크면 다른 API도 DB를 못 써서 느려진다. |
| **@Transactional**            | 메서드(또는 클래스) 단위로 DB 트랜잭션을 열고, 끝날 때까지 커넥션을 붙잡는다.                           |
| **NOT_SUPPORTED**             | 기존 트랜잭션에 참여하지 않음. AI 호출 구간에 쓰면 **DB 커넥션을 잡지 않은 채** 대기할 수 있다.             |
| **H2**                        | 테스트용 인메모리 DB. Postgres를 띄우지 않고 JPA 저장 로직을 검증할 때 쓴다. 테스트 종료 시 데이터 삭제.     |
| **DB 프로브**                    | 부하 중 50ms마다 `SELECT 1`을 실행해, **다른 API가 DB를 쓰려 할 때** 얼마나 기다리는지 재는 보조 지표.  |


### 2.3 환경 설정 (`application-chat-pool-test.yml`)


| 설정                  | 값            | 이유                                        |
| ------------------- | ------------ | ----------------------------------------- |
| DB                  | H2 in-memory | Postgres 없이 CI·로컬에서 빠르게 실행                |
| `maximum-pool-size` | **5**        | 운영 값이 아님. **의도적으로 작게** 잡아 풀 고갈을 재현하기 쉽게 함 |
| 동시 요청 수             | **12**       | 풀(5)보다 많아 대기가 발생하도록                       |
| AI 지연 (mock)        | **2,000ms**  | 실제 AI 대기를 `Thread.sleep`으로 대체             |
| `AiChatClient`      | `@MockBean`  | 네트워크·AI 서버 없이 지연만 재현                      |
| Security            | 비활성          | 테스트 범위 밖                                  |


### 2.4 운영 환경과의 차이


| 항목       | 운영 (VM/Docker)                 | 이 테스트         |
| -------- | ------------------------------ | ------------- |
| DB       | PostgreSQL 17                  | H2 (메모리)      |
| 커넥션 풀 상한 | Hikari **기본값 약 10** (yaml 미설정) | **5** (실험용)   |
| AI       | bifriends-ai HTTP              | mock 2초 sleep |


운영도 풀 크기만 다를 뿠, **“AI 대기 중 커넥션 점유”** 메커니즘은 동일하다. 풀을 5로 줄인 것은 **현상을 더 크게 보이게 하는 실험 설계**이지, 운영 스펙을 그대로 옮긴 것이 아니다.

### 2.5 비교 라운드 (LEGACY vs FIXED)

> **Leo 채팅(톡톡)** `ChatService.sendMessage()` 경로만 검증합니다. 친구랑 감정 시나리오(`/mind`)와 무관합니다.

1. **LEGACY:** `LegacyChatTransactionSimulator` — 단일 `@Transactional` 안에서 user 저장 → `sleep(2s)` → assistant 저장 (수정 전 패턴 재현).
2. **FIXED:** 현재 `ChatService.sendMessage()` — TX 분리 + `NOT_SUPPORTED`.

### 2.6 실행 방법

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
cd bifriends-be
.\gradlew test --tests "com.bifriends.domain.chat.ChatHikariPoolComparisonTest"
```

- stdout / HTML 리포트에 `[STAR]` 블록 출력.
- 리포트: `build/reports/tests/test/classes/com.bifriends.domain.chat.ChatHikariPoolComparisonTest.html`
- Docker: `scripts/chat_hikari_pool_test.ps1` (첫 실행은 의존성 다운로드로 오래 걸릴 수 있음)

---

## 3. 테스트 결과

**실행 일자:** 2026-06-26  
**조건:** 동시 12건, AI mock 2,000ms, Hikari `maximum-pool-size=5`  
**로컬:** JDK 17 + Gradle toolchain JDK 21, 약 40초 소요

### 3.1 STAR 요약


|                         | LEGACY (수정 전 패턴) | FIXED (현재 코드) |
| ----------------------- | ---------------- | ------------- |
| 성공 / 실패                 | 12 / 0           | 12 / 0        |
| **채팅 12건 처리 총 소요 (ms)** | **6,225**        | **2,043**     |
| **max threadsAwaiting** | **8**            | **0**         |
| DB 프로브 max (ms)         | **4,214**        | **0**         |
| DB 프로브 P95 (ms)         | 0*               | 0*            |


 프로브 샘플 대부분이 1ms 미만이라 P95가 0으로 표시됨. **max**가 부하 중 최악 지연을 더 잘 반영한다.

### 3.2 원문 로그 (`[STAR]` 블록)

```
========== [STAR] 채팅 TX 분리 — Hikari 부하 비교 ==========
Situation : 동시 채팅 12건, AI 응답 2000ms, Hikari pool=5
Task      : AI HTTP 대기 중 DB 커넥션 점유 → 풀 고갈·다른 API 대기 감소 증명
Action    : sendMessage를 ① TX 분리(NOT_SUPPORTED) vs ② 레거시(단일 TX+sleep) 재현

--- LEGACY (수정 전 패턴) ---
  성공/실패        : 12/0
  총 소요(ms)       : 6225
  max threadsAwaiting : 8
  DB 프로브 P95(ms) : 0 (max 4214, n=33)

--- FIXED (현재 코드) ---
  성공/실패        : 12/0
  총 소요(ms)       : 2043
  max threadsAwaiting : 0
  DB 프로브 P95(ms) : 0 (max 0, n=34)

Result:
  threadsAwaiting 감소 : 8 → 0 (Δ 8)
  DB 프로브 max 개선   : 100.0% (max 4214ms → 0ms)
============================================================
```

### 3.3 DB에 저장된 내용 (개념)

각 라운드마다 동시 12회 `sendMessage` 호출 → 회당 **user + assistant** 메시지 2건:

- `chat_sessions`: 세션 키마다 1행 (최대 12행/라운드)
- `chat_messages`: 최대 24행/라운드

테스트 종료 시 H2 `create-drop`으로 테이블이 삭제된다.

---

## 4. 결과 해석

### 4.1 핵심 결론

**트랜잭션 분리 후, AI 대기 구간에 DB 커넥션을 붙잡지 않아 풀 대기가 사라졌다.**

- `threadsAwaiting` **8 → 0**: 동시 12채팅·풀 5 조건에서, 수정 전에는 최대 8개 스레드가 커넥션을 기다렸고, 수정 후에는 대기 없음.
- **채팅 12건 처리 총 소요** **약 6.2초 → 약 2.0초**: 레거시는 커넥션 부족으로 요청이 **줄을 서서** 순차적으로 진행된 것에 가깝고, 수정 후는 AI 2초만큼만 병렬로 기다리면 됨.

> **용어 주의:** 여기서 “라운드”는 친구랑 **감정 시나리오**가 아니라, **Leo 채팅 `sendMessage` 12건**을 한 번에 돌린 테스트 실행입니다.
- DB 프로브 max **4.2초 → 0ms**: 채팅 부하 중에도 다른 DB 작업(홈 조회 등)이 **거의 즉시** 실행 가능해짐을 시사.

### 4.2 체감 속도와의 관계


| 구간                    | 수정 영향                                       |
| --------------------- | ------------------------------------------- |
| **Leo 답장 자체 (AI 시간)** | 거의 없음 — AI HTTP 시간은 그대로                     |
| **채팅 저장 실패·롤백**       | user 메시지는 AI 실패 시에도 남을 수 있음 (의도된 UX 트레이드오프) |
| **동시 사용자·다른 탭 API**   | **큼** — 풀 대기가 줄어 홈·학습·로그인 등이 덜 막힘           |
| **채팅만 단독·저부하**        | 체감 차이 작을 수 있음                               |


즉, 이 수정은 **“Leo가 1초 빨리 말한다”**기보다 **“채팅 때문에 서버 DB가 숨 막히지 않는다”**에 가깝다.

### 4.3 한계 

1. **H2 ≠ PostgreSQL** — 저장 로직·JPA·Hikari 동작은 유사하나, 운영 DB와 100% 동일하진 않다.
2. **풀 5는 실험용** — 운영은 기본 약 10. 절대값(8)보다 **전후 차이(8 vs 0)** 가 메시지다.
3. **HTTP·JWT 미포함** — API 레이어 추가 지연은 측정하지 않았다.
4. **AI는 mock** — 실제 Gemini 지연·타임아웃 분포와 다를 수 있다.

### 4.4 STAR 스크립트 

- **Situation:** 채팅 사용 시 응답이 느리게 느껴지고, 동시 사용 시 다른 API도 느려지는 현상이 있었다.
- **Task:** 코드 구조를 분석해 DB 커넥션을 AI 대기 동안 점유하는지 확인하고, 수정 후 정량 검증이 필요했다.
- **Action:** `ChatMessageWriteService`로 TX 분리, `NOT_SUPPORTED`로 AI 구간 분리. H2·풀 5·동시 12·AI 2초 mock으로 전·후 비교 테스트 작성.
- **Result:** `threadsAwaiting` 8→0, 부하 중 DB 프로브 max 4.2s→0, **채팅 12건 처리** 약 6.2s→2.0s.

---

## 5. 관련 파일


| 파일                                               | 역할             |
| ------------------------------------------------ | -------------- |
| `domain/chat/service/ChatService.kt`             | 수정된 오케스트레이션    |
| `domain/chat/service/ChatMessageWriteService.kt` | DB 저장 전용 TX    |
| `test/.../ChatHikariPoolComparisonTest.kt`       | 비교 테스트         |
| `test/.../LegacyChatTransactionSimulator.kt`     | 수정 전 패턴 재현     |
| `test/resources/application-chat-pool-test.yml`  | H2·풀 5 설정      |
| `scripts/chat_hikari_pool_test.ps1`              | Docker 실행 (선택) |


---

## 변경 이력


| 날짜         | 내용                  |
| ---------- | ------------------- |
| 2026-06-26 | 초안 — 배경·환경·결과·해석 정리 |


