# 홈 화면 (Home) 상세 설계 문서

> 작성일: 2026-05-07  
> 대상 모듈: `domain/home`  
> 관련 기능명세: Functional Specification — 단위 시스템: 홈 (HOM-08-01 ~ HOM-08-09)

---

## 1. 배경 및 목적

BiFriends는 느린학습자(경계선 지능) 아동을 대상으로 하는 앱이다.  
홈 화면은 아이가 앱을 열었을 때 가장 먼저 마주하는 화면이다. 이 화면이 잘 작동해야 아이가 매일 앱을 열고 학습 루틴을 이어갈 수 있다.

홈 화면 백엔드의 핵심 역할은 세 가지다.

- **출석 체크**: 앱을 연 것만으로 streak이 쌓이고 보상이 지급된다.
- **인사 메시지**: 아이의 접속 패턴(첫 방문 / 복귀 / 연속 출석)에 맞는 메시지로 따뜻하게 맞이한다.
- **오늘의 할 일 노출**: 레벨/풀 현황과 할 일 목록을 한 번의 API 호출로 제공한다.

---

## 2. 설계 결정

### 2.1 단일 홈 API vs 분리된 API

홈 화면 구성 요소별로 API를 분리하는 방식을 검토했으나 단일 API를 채택했다.

**분리 방식 (검토)**

```
GET /api/v1/home/stats      레벨·풀·streak
GET /api/v1/home/greeting   인사 메시지
GET /api/v1/todos/today     오늘의 할 일
```

이 방식은 홈 화면을 렌더링하기 위해 클라이언트가 3번의 API를 병렬 호출해야 한다. Flutter 앱 구조에서는 각각 로딩 상태를 따로 관리해야 하고, 하나라도 실패하면 화면이 부분적으로 깨질 수 있다.

**단일 API 방식 (채택)**

```
GET /api/v1/home
  → 인사 메시지 + 레벨/풀/streak + 출석 결과 + 할 일 목록
```

홈 화면 진입 시 단 한 번의 요청으로 렌더링에 필요한 모든 데이터를 받는다. 클라이언트는 로딩 상태를 하나만 관리하면 된다. 출석 체크도 이 API 안에서 함께 처리되므로, 클라이언트가 별도로 출석 API를 호출할 필요 없다.

### 2.2 출석 기준: 로그인이 아닌 홈 화면 진입

BiFriends 앱은 한 번 로그인하면 JWT 토큰이 만료되지 않는 한 계속 로그인 상태를 유지한다. 즉, 아이는 매일 앱을 열 때 별도의 로그인 과정을 거치지 않는다.

출석 체크를 "로그인"에 연결하면 아이가 오늘 앱을 열었는데도 streak이 쌓이지 않는 문제가 발생한다.

따라서 **홈 화면 진입 = 출석**으로 정의한다. 매일 앱을 열고 홈 화면이 로드될 때마다 `GET /api/v1/home`이 호출되고, 이 안에서 `recordAttendance()`가 실행된다.

```
아이 행동:   앱 오픈 → 홈 화면 렌더링
API 호출:   GET /api/v1/home
서버 처리:  lastAttendanceDate == today? → 스킵 (멱등)
            lastAttendanceDate != today? → streak 갱신 + 보상 지급
```

`UserStats.lastAttendanceDate` 필드명도 이 결정을 반영한다. 처음에는 `lastLoginDate`로 지었으나, 실제 의미("마지막 홈 방문 날짜")와 맞지 않아 `lastAttendanceDate`로 변경했다.

### 2.3 인사 메시지 분기 설계

인사 메시지는 Notion 명세의 `selection_priority` 순서를 따른다.

**분기 기준 (출석 처리 전 `lastAttendanceDate` 기준)**

```
lastAttendanceDate == null  → FIRST_LOGIN   (앱 최초 접속)
gap 0~1일                   → STREAK        (연속 출석 중)
gap 2~3일                   → COMEBACK_SHORT
gap 4일 이상                → COMEBACK_LONG
```

gap = `ChronoUnit.DAYS.between(lastAttendanceDate, today)`

**STREAK 세부 bucket (출석 처리 후 `streakDays` 기준)**

| streakDays | bucket |
|---|---|
| 1 | streak_day_1 |
| 2~3 | streak_day_2_3 |
| 4~6 | streak_day_4_6 |
| 7+ | streak_day_7_plus |

**pre/post attendance 분리가 필요한 이유**

인사 유형은 출석 처리 **전** 상태로 결정해야 한다. 예를 들어 3일만에 복귀한 아이가 `GET /api/v1/home`을 호출하면:

```
recordAttendance() 전: lastAttendanceDate = 3일 전  → gap = 3 → COMEBACK_SHORT
recordAttendance() 후: lastAttendanceDate = today   → gap = 0 → STREAK (잘못된 결과!)
```

처리 전 값을 캡처(`lastAttendanceBefore`)한 뒤 출석을 처리하고, 인사 유형은 캡처된 값으로 결정한다.

STREAK bucket은 반대로 출석 처리 **후** `streakDays`를 써야 한다. 어제까지 3일 연속이었다면 오늘 출석 후 4일이 되어 `streak_day_4_6` bucket에 해당한다.

---

## 3. 소프트웨어 설계 고려 사항

### 3.1 GreetingPolicy object

인사 메시지 분기 로직과 메시지 풀을 `GreetingPolicy.kt`에 순수 `object`로 분리했다.

`LevelPolicy`와 같은 이유다. DB나 Spring Context 없이 단위 테스트에서 바로 호출할 수 있고, 기획이 바뀌어 메시지나 분기 조건이 달라져도 이 파일 하나만 수정하면 전체에 반영된다.

```kotlin
// Spring 없이 그냥 실행되는 단위 테스트
class GreetingPolicyTest {
    @Test
    fun `3일 공백은 COMEBACK_SHORT`() {
        val today = LocalDate.of(2026, 5, 7)
        val lastDate = today.minusDays(3)
        assertThat(GreetingPolicy.determineType(lastDate, today))
            .isEqualTo(GreetingType.COMEBACK_SHORT)
    }

    @Test
    fun `첫 방문은 FIRST_LOGIN`() {
        assertThat(GreetingPolicy.determineType(null, LocalDate.now()))
            .isEqualTo(GreetingType.FIRST_LOGIN)
    }
}
```

### 3.2 HomeService의 트랜잭션 처리

`HomeService.getHome()`은 `@Transactional`로 묶여 있다. 내부에서 호출하는 `userStatsService.getOrCreate()`, `userStatsService.recordAttendance()`, `todoService.getTodayTodos()`는 모두 기본 전파 레벨(`REQUIRED`)이므로 같은 트랜잭션에 합류한다.

`getOrCreate()`로 로드된 `UserStats` 엔티티는 JPA 1차 캐시에 보관된다. 이후 `recordAttendance()`가 내부에서 `getOrCreate()`를 다시 호출할 때 DB 조회 없이 같은 인스턴스가 반환된다. 따라서 `lastAttendanceBefore` 값 캡처와 실제 변경이 같은 인스턴스 위에서 순서대로 일어난다.

```kotlin
val statsBefore = userStatsService.getOrCreate(memberId)       // DB 조회 → 1차 캐시에 저장
val lastAttendanceBefore = statsBefore.lastAttendanceDate      // 변경 불가능한 값 복사

val attendanceResult = userStatsService.recordAttendance(memberId)
// recordAttendance 내부에서 getOrCreate() 재호출 → 1차 캐시에서 같은 인스턴스 반환
// statsBefore.lastAttendanceDate가 변경되어도 lastAttendanceBefore는 원래 값 유지
```

### 3.3 닉네임 fallback

`Member.nickname`은 온보딩 전에는 null이다. 온보딩 전에도 홈 화면 진입이 가능할 경우를 대비해 `nickname ?: name`으로 fallback 처리한다.

```kotlin
val nickname = member.nickname ?: member.name
```

---

## 4. 패키지 구조 설계 이유

```
domain/home/
├── controller/
│   └── HomeController.kt         # GET /api/v1/home 진입점
├── service/
│   └── HomeService.kt            # 오케스트레이터
├── model/
│   ├── GreetingType.kt           # 인사 유형 enum
│   └── GreetingPolicy.kt         # 인사 메시지 정책 (순수 object)
└── dto/
    └── HomeDtos.kt               # HomeResponse, MemberSummary, GreetingResponse
```

### 4.1 HomeService를 별도로 분리한 이유

홈 API는 `UserStatsService`, `TodoService`, `MemberRepository` 세 곳에서 데이터를 가져와야 한다. 이 오케스트레이션을 `UserStatsService`나 `TodoService` 중 한 곳에 넣으면 서비스 간 순환 의존이 발생한다. 홈 화면이라는 진입점 역할에 맞는 별도 서비스를 두어 각 서비스의 역할 경계를 유지했다.

### 4.2 GreetingPolicy를 model/에 둔 이유

`LevelPolicy`, `LearningTypePolicy`와 동일한 이유다. "어떤 상황에서 어떤 메시지를 보여주는가"는 DB나 Spring과 무관하게 이 도메인이 어떻게 동작하는지를 정의하는 **도메인 규칙**이다. 인프라 계층이나 서비스 계층이 아닌 모델 계층에 두는 것이 자연스럽다.

### 4.3 HomeDtos에 다른 패키지 DTO를 참조하는 이유

`HomeResponse`는 `UserStatsResponse`, `AttendanceResult`, `TodoResponse`를 필드로 사용한다. 이것들은 모두 같은 `dto` 패키지 내에 있어 import 없이 참조 가능하다. 각 DTO를 중복 정의하지 않고 재사용함으로써 응답 형식의 일관성을 유지한다.

---

## 5. 향후 고려 사항

| 항목 | 현재 상태 | 확장 시 대응 방안 |
|---|---|---|
| 할 일 완료 컨트롤러 | 미구현 (서비스 로직은 있음) | `TodoController` 작성 필요 |
| 완료 응답 stats 포함 | `RewardResult`에 부분 포함 | `TodoCompleteResult`에 `UserStatsResponse` 추가 검토 |
| 인사 메시지 다국어 | 한국어 고정 | Spring MessageSource로 전환 |
| 인사 메시지 실시간 수정 | 코드에 하드코딩 | DB 또는 CMS 기반 관리로 전환 |
| 홈 API 응답 캐싱 | 없음 | stats는 캐싱 가능, greeting/todos는 매번 새로 조회 필요 |
| 전역 예외 처리 | 미구현 | `@ControllerAdvice`로 IllegalStateException → 400, IllegalArgumentException → 404 처리 |
