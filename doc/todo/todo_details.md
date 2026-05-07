# 오늘의 할 일 (Todo) 상세 설계 문서

> 작성일: 2026-05-07  
> 대상 모듈: `domain/home`  
> 관련 기능명세: Functional Specification — 단위 시스템: 홈 (HOM-08-08, HOM-08-09)

---

## 1. 배경 및 목적

BiFriends는 느린학습자(경계선 지능) 아동을 대상으로 하는 앱이다.  
아이들이 매일 앱을 열었을 때 "오늘 내가 할 일"이 명확하게 보여야 한다. 막연한 학습 유도가 아니라, 구체적인 세 가지 행동(챗봇 대화, 퀴즈 풀기, 감정 시나리오)을 오늘의 할 일로 제시하여 아이와 보호자가 학습 루틴을 만들 수 있도록 돕는 것이 이 기능의 목적이다.

기능명세서 기준으로 할 일 시스템에서 백엔드가 담당하는 핵심 요구사항은 두 가지다.

- **HOM-08-08**: 매일 자동으로 할 일 3개를 생성하고 홈 화면에 노출한다.
- **HOM-08-09**: 할 일 완료 시 풀 보상을 지급하고, 전체 완료 시 보너스를 추가 지급한다.

---

## 2. 데이터 설계: 삭제 없이 날짜로 구분 (B안)

### 2.1 기능명세서 원안 (A안)과의 차이

기능명세서 원안(A안)은 매일 00:00에 기존 할 일을 **삭제하고 새로 생성**하는 방식이었다.

```
A안: 매일 00:00
  1. 기존 todos WHERE assigned_date < today → DELETE
  2. 새 todos 3개 → INSERT
```

이 방식을 채택하지 않은 이유는 **과거 완료 이력이 사라지기 때문**이다.  
아이가 지난주에 퀴즈를 몇 개 풀었는지, 어떤 날 모든 할 일을 완료했는지 — 이 데이터는 향후 성장일기, 학습 통계 기능에서 반드시 필요하다. 삭제 방식을 쓰면 그 이력을 영구히 잃게 된다.

### 2.2 채택한 방식 (B안): `assignedDate` 컬럼으로 날짜 구분

할 일을 삭제하지 않고 `assignedDate: LocalDate` 컬럼을 두어 날짜 기준으로 조회한다.

```
B안: 매일 00:00
  → todos에 오늘 날짜로 3개 INSERT (기존 데이터는 건드리지 않음)

홈 화면 조회:
  SELECT * FROM todos WHERE member_id = ? AND assigned_date = '오늘'

성장일기 조회:
  SELECT * FROM todos WHERE member_id = ? AND assigned_date BETWEEN '시작' AND '끝'
```

**B안의 장점**

- 과거 완료 이력이 그대로 남아, 성장일기·학습 통계 기능에 재활용 가능하다.
- 날짜별 조회 패턴이 단순하고 인덱스를 타기 쉽다.
- 스케줄러가 실패하거나 중복 실행되어도 데이터 정합성이 무너지지 않는다.

**B안의 트레이드오프**

- 데이터가 누적되므로, 서비스 규모가 커지면 `todos` 테이블의 행 수가 빠르게 늘어난다.  
  (회원 수 × 일수 × 3 = 1만 명 × 365일 × 3개 → 약 1천만 행/년)
- MVP 단계에서는 인덱스(`member_id, assigned_date`)로 충분히 대응 가능하다. 데이터가 폭증하면 파티셔닝 또는 아카이브 정책을 검토하면 된다.

### 2.3 DB 테이블 설계

```sql
CREATE TABLE todos (
    id                BIGSERIAL PRIMARY KEY,
    member_id         BIGINT NOT NULL REFERENCES members(id),
    type              VARCHAR(20) NOT NULL,        -- CHAT | LEARNING | EMOTION | CUSTOM
    title             VARCHAR(255) NOT NULL,
    estimated_time_sec INT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source            VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    learning_type     VARCHAR(20),                 -- MATH | LANGUAGE | null
    assigned_date     DATE NOT NULL,               -- KST 기준 날짜
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_todos_member_date ON todos(member_id, assigned_date);
```

`idx_todos_member_date` 복합 인덱스를 두어, 홈 화면 진입 시 가장 빈번하게 발생하는 `member_id + assigned_date` 조회를 커버 인덱스로 처리한다.

---

## 3. 할 일 유형 설계

### 3.1 할 일 유형 (`TodoType`)

하루 3개의 기본 할 일은 각각 앱의 핵심 기능 탭에 대응한다.

| 유형 | 기본 제목 | 예상 시간 | 연결 탭 |
|---|---|---|---|
| `CHAT` | 레오랑 이야기하기 🗣️ | 60초 | 챗봇 탭 |
| `LEARNING` | 오늘의 문제 3개 풀기 📚 | 180초 | 배움 탭 |
| `EMOTION` | 친구 기분 알아보기 💌 | 120초 | 마음 탭 |
| `CUSTOM` | (Agent가 지정) | — | — |

`CUSTOM`은 AI Agent가 아이의 상황에 맞게 추가하는 할 일 유형이다. 기본 3개는 항상 `SYSTEM` 출처로 생성되며, `CUSTOM`은 항상 `AGENT` 출처다.

### 3.2 생성 출처 (`TodoSource`)

```
SYSTEM → 스케줄러 자동 생성 (매일 3개 고정)
AGENT  → AI Agent 추가 생성 (최대 2개까지 추가 가능)
```

출처를 구분하는 가장 중요한 이유는 **수정·삭제 보호**다. 스케줄러가 생성한 기본 할 일(`SYSTEM`)은 아이와 부모가 앱에 진입할 때마다 항상 동일하게 보여야 한다. Agent가 실수로 기본 할 일을 수정하거나 삭제하는 일이 없어야 한다.

### 3.3 요일별 학습 과목 분기 (`LearningTypePolicy`)

`LEARNING` 할 일은 요일에 따라 연결되는 공부방이 달라진다.

```
월·수·금 → MATH     (생각하는 힘, 수학 공부방)
화·목·토 → LANGUAGE (말하는 힘, 국어 공부방)
일       → null     (전체 공부방 목록으로 이동, 자유 선택)
```

이 정책은 `LearningTypePolicy.kt`에 순수 `object`로 분리했다. 단위 테스트에서 Spring/DB 없이 `LearningTypePolicy.forDate(LocalDate.of(2026, 5, 4))` 형태로 바로 호출해 검증할 수 있다.

---

## 4. 스케줄러 설계

### 4.1 실행 시점

```kotlin
@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
fun generateDailyTodos()
```

매일 00:00 KST에 전체 회원에 대해 오늘 날짜의 할 일 3개를 생성한다. KST를 명시적으로 지정한 이유는 서버 시간대(UTC)와 사용자 기준(KST)의 불일치를 방지하기 위해서다. 서버가 UTC로 운영되더라도 아이 기준 "오늘"인 자정 KST에 실행된다.

### 4.2 멱등성 보장

스케줄러는 같은 날 여러 번 실행되더라도 할 일이 두 번 생성되지 않아야 한다. 서버 재시작, 배포 중 중복 실행, 또는 운영 중 수동 재실행 상황을 모두 안전하게 처리해야 한다.

**진입 시 체크 방식 (채택)**

```kotlin
fun generateDailyTodos(member: Member, today: LocalDate) {
    if (todoRepository.existsByMemberIdAndAssignedDate(member.id, today)) return
    // ...할 일 생성
}
```

`generateDailyTodos()` 첫 줄에서 오늘 날짜 할 일이 이미 존재하는지 확인하고, 있으면 즉시 반환한다. 이 한 줄이 스케줄러 중복 실행을 완전히 막는다.

이 방식이 작동하는 이유는 B안(날짜 컬럼 방식) 덕분이다. A안(삭제 후 재생성)이었다면 "삭제 후 재생성 vs 이미 오늘 것 있음" 사이의 경합 상태가 발생할 수 있었다.

### 4.3 개별 회원 에러 격리

```kotlin
members.forEach { member ->
    try {
        todoService.generateDailyTodos(member, today)
    } catch (e: Exception) {
        log.error("[TodoScheduler] 할 일 생성 실패 — memberId=${member.id}", e)
    }
}
```

한 회원의 할 일 생성이 실패하더라도 다른 회원 처리는 계속 진행된다. `try-catch`를 회원 단위로 두어, 한 명의 예외가 전체 스케줄러 실행을 중단시키지 않도록 했다.

### 4.4 MVP 한계와 확장 방안

현재는 `memberRepository.findAll()`로 전체 회원을 한 번에 조회해서 순차 처리한다. 회원이 수천 명을 넘으면 메모리 부족과 처리 시간 초과 위험이 있다.

```
현재 (MVP):  findAll() → forEach 순차 처리
확장 시:     Spring Batch Chunk 처리 (페이징, 실패 청크 재시도)
```

### 4.5 신규 가입 시 즉시 생성 (크로스 도메인 이벤트)

스케줄러는 매일 00:00 KST에 실행된다. 그 이후에 가입한 신규 회원은 다음 날 자정까지 오늘의 할 일이 없다. 이를 보완하기 위해 회원 가입 시점에 즉시 오늘의 할 일을 생성한다.

**문제**

```
00:00  스케줄러 실행 → 기존 회원 전체 할 일 생성
09:30  신규 회원 가입 → 오늘 할 일 없음 (다음날 00:00까지 대기)
```

**해결: Spring ApplicationEvent**

`MemberService`는 신규 회원 저장 후 `MemberRegisteredEvent`를 발행한다.  
`MemberRegistrationEventListener`(home 도메인)가 이벤트를 수신해 `generateDailyTodos()`를 호출한다.

```
[트랜잭션 1] MemberService.findOrCreateMember()
  → memberRepository.save(member)
  → eventPublisher.publishEvent(MemberRegisteredEvent(member))
  → 트랜잭션 1 COMMIT

[트랜잭션 2] MemberRegistrationEventListener.onMemberRegistered()  ← AFTER_COMMIT
  → todoService.generateDailyTodos(member, today)
```

`AFTER_COMMIT` 페이즈를 사용하는 이유는 todo 생성 실패가 회원가입 롤백으로 이어지는 것을 막기 위해서다.  
`generateDailyTodos()`는 멱등 처리가 되어 있으므로 이벤트 중복 발행에도 안전하다.

**관련 파일**

```
domain/member/event/MemberRegisteredEvent.kt         # 이벤트 클래스
domain/member/service/MemberService.kt               # 이벤트 발행
domain/home/service/MemberRegistrationEventListener.kt # 이벤트 수신 및 할 일 생성
```

> 크로스 도메인 이벤트 전체 목록 → [doc/events/README.md](../events/README.md)

---

## 5. 소프트웨어 설계 고려 사항

### 5.1 SYSTEM 할 일 보호 (엔티티 도메인 메서드)

SYSTEM 출처 할 일의 수정·삭제 금지 규칙은 서비스 레이어가 아닌 **엔티티 메서드 수준**에서 강제한다.

**서비스에서 검증하는 방식 (채택하지 않음)**

```kotlin
// TodoService
fun updateAgentTodo(todoId: Long, title: String) {
    val todo = todoRepository.findById(todoId).get()
    if (todo.source == TodoSource.SYSTEM) throw IllegalStateException("수정 불가")
    todo.title = title  // 직접 필드 수정
}
```

이 방식은 `deleteAgentTodo`, `createAgentTodo` 등 다른 서비스 메서드에도 같은 검증 코드를 반복 작성해야 한다. 한 곳이라도 빠뜨리면 SYSTEM 할 일이 무방비로 수정된다.

**엔티티 도메인 메서드에서 강제하는 방식 (채택)**

```kotlin
// Todo 엔티티
fun updateTitle(newTitle: String) {
    check(source == TodoSource.AGENT) { "시스템 할 일은 수정할 수 없습니다. id=$id" }
    title = newTitle
    updatedAt = LocalDateTime.now()
}
```

```kotlin
// TodoService — 검증 로직 없이 엔티티 메서드만 호출
fun updateAgentTodo(memberId: Long, todoId: Long, request: AgentTodoUpdateRequest): TodoResponse {
    val todo = findTodoOfMember(memberId, todoId)
    todo.updateTitle(request.title)  // 엔티티 내부에서 SYSTEM 여부 검증
    return TodoResponse.from(todo)
}
```

`updateTitle()`을 호출하는 모든 경로에서 자동으로 SYSTEM 보호가 적용된다. 서비스 메서드를 아무리 많이 추가해도 이 규칙을 깜빡할 수 없다.

### 5.2 완료 보너스를 단일 트랜잭션에서 처리

전체 완료 보너스 지급 방식으로 두 가지 선택지를 검토했다.

**선택지 A: 별도 배치 또는 스케줄러로 자정에 체크**

자정에 "오늘 할 일이 모두 완료된 회원"을 스캔해서 보너스를 지급하는 방식이다. 이 방식은 지급 타이밍이 지연되고, 배치 실패 시 보너스를 못 받는 회원이 생길 수 있다.

**선택지 B: 완료 처리 시점에 즉시 체크 (채택)**

```kotlin
@Transactional
fun completeTodo(memberId: Long, todoId: Long): TodoCompleteResult {
    todo.complete()                          // 1. 완료 처리

    userStatsService.earnReward(             // 2. 단일 완료 보상 (+1풀)
        source = RewardSource.TODO_SINGLE,
        amount = RewardPolicy.TODO_SINGLE,
    )

    val remainingPending = todoRepository.countByMemberIdAndAssignedDateAndStatus(
        memberId, today, TodoStatus.PENDING
    )
    if (remainingPending == 0) {             // 3. 남은 PENDING이 0이면 보너스
        userStatsService.earnReward(
            source = RewardSource.TODO_ALL_COMPLETE,
            amount = RewardPolicy.TODO_ALL_COMPLETE,
        )
    }
}
```

마지막 할 일이 완료되는 시점에 `PENDING` 개수를 세어 0이면 즉시 보너스를 지급한다. 단일 완료 보상과 전체 완료 보너스가 하나의 트랜잭션 안에서 처리되므로, 둘 중 하나가 실패하면 전체가 롤백된다. 아이 입장에서는 마지막 할 일을 완료하는 순간 바로 보너스 풀이 지급되므로 즉각적인 피드백을 받을 수 있다.

### 5.3 하루 최대 5개 제한 (`MAX_TODOS_PER_DAY`)

기능명세 HOM-08-09는 하루 할 일을 최대 5개로 제한한다 (SYSTEM 3 + AGENT 2).  
이 상수는 `TodoService.companion object`에 정의하고, Agent 추가 시 DB 조회 결과와 비교한다.

```kotlin
companion object {
    const val MAX_TODOS_PER_DAY = 5
    const val SYSTEM_TODOS_PER_DAY = 3
}

fun createAgentTodo(...) {
    val currentCount = todoRepository.countByMemberIdAndAssignedDate(memberId, today)
    check(currentCount < MAX_TODOS_PER_DAY) { "하루 최대 할 일 개수($MAX_TODOS_PER_DAY)를 초과했습니다." }
}
```

숫자 `5`를 코드 전체에 하드코딩하지 않고 상수로 빼둔 이유는, 기획이 바뀌어 최대 개수가 달라질 때 이 상수 하나만 수정하면 전체에 반영되기 때문이다.

### 5.4 KST 기준 날짜 처리

할 일은 "오늘"이라는 개념이 핵심이다. 서버가 UTC로 운영되면 서울 기준 00:30에 생성된 할 일이 어제 날짜로 기록되는 문제가 발생할 수 있다.

```kotlin
private val KST = ZoneId.of("Asia/Seoul")

val today = LocalDate.now(KST)   // UTC가 아닌 KST 기준 오늘 날짜
```

`LocalDate.now(KST)`를 사용해 항상 서울 기준 오늘 날짜를 명시적으로 가져온다. `@Scheduled`의 `zone = "Asia/Seoul"`과 쌍을 이루어, 스케줄러 실행 타이밍과 날짜 기록이 항상 동일한 시간대 기준으로 동작한다.

---

## 6. `domain/home` 패키지 구조 설계 이유

```
domain/home/
├── model/
│   ├── Todo.kt                          # 핵심 엔티티 (도메인 메서드 포함)
│   ├── TodoType.kt                      # CHAT / LEARNING / EMOTION / CUSTOM
│   ├── TodoStatus.kt                    # PENDING / COMPLETED
│   ├── TodoSource.kt                    # SYSTEM / AGENT
│   ├── LearningType.kt                  # MATH / LANGUAGE
│   └── LearningTypePolicy.kt           # 요일별 과목 분기 정책 (순수 object)
├── repository/
│   └── TodoRepository.kt               # 날짜·상태 기반 조회 메서드
├── service/
│   ├── TodoService.kt                  # 생성·완료·Agent CRUD 비즈니스 로직
│   ├── TodoScheduler.kt                # 매일 00:00 KST 자동 실행
│   └── MemberRegistrationEventListener.kt  # 신규 가입 시 즉시 할 일 생성
└── dto/
    └── TodoDtos.kt                     # TodoResponse, TodoCompleteResult 등

domain/member/
└── event/
    └── MemberRegisteredEvent.kt        # 신규 가입 이벤트 (home 도메인이 수신)
```

### 6.1 Todo를 `domain/home/`에 둔 이유

할 일은 홈 화면에서만 노출되는 개념이다. `domain/todo/`로 독립 분리하는 방법도 있지만, 현재 할 일은 홈의 레벨 정보·인사 메시지와 함께 홈 화면 진입 시 한 번에 조회된다. 홈이라는 맥락에서 강하게 결합되어 있으므로 `domain/home/`에 함께 두는 것이 자연스럽다. 향후 할 일이 다른 탭에서도 독립적으로 관리되는 기능이 생기면 그 시점에 분리를 검토하면 된다.

### 6.2 `LearningTypePolicy`를 `model/`에 둔 이유

`LearningTypePolicy`는 JPA 엔티티가 아닌 순수 Kotlin `object`다. 그럼에도 `model/`에 넣은 이유는 이것이 **도메인 규칙 그 자체**이기 때문이다. "월요일에는 수학 공부방으로 간다"는 규칙은 DB나 Spring과 무관하게 이 도메인이 어떻게 동작하는지를 정의한다. `TodoService`가 생성 시 `LearningTypePolicy.forDate(today)`를 직접 참조하므로, 같은 패키지에 두는 것이 의존 방향을 명확하게 한다.

### 6.3 `TodoScheduler`를 `service/`에 둔 이유

스케줄러는 `TodoService.generateDailyTodos()`를 호출하는 진입점일 뿐이다. 비즈니스 로직은 `TodoService`에 있고, `TodoScheduler`는 그것을 자동으로 트리거하는 인프라 역할이다. `@Component`이지만 비즈니스 로직과 물리적으로 가까운 `service/`에 두어, "이 스케줄러가 어떤 서비스를 구동하는가"를 패키지 구조만 봐도 알 수 있도록 했다.

### 6.4 `dto/`를 분리한 이유

`Todo` 엔티티를 API 응답으로 직접 반환하지 않고 `TodoResponse` DTO로 변환하는 이유는 두 가지다.

- **JPA 안전성**: 엔티티를 그대로 직렬화하면 `member` 연관 관계가 지연 로딩 중 JSON 변환 시점에 `LazyInitializationException`을 던질 수 있다. DTO에는 필요한 필드만 담아 이 문제를 원천 차단한다.
- **결합도 감소**: DB 스키마와 API 스펙이 강하게 결합되면 테이블 컬럼 하나를 바꿀 때 API 응답도 함께 바뀐다. DTO를 두면 둘을 독립적으로 변경할 수 있다.

`TodoResponse.from(todo)`처럼 변환 책임을 DTO의 `companion object`에 두어, 서비스가 변환 로직을 직접 알 필요 없도록 했다.

---

## 7. 향후 고려 사항

| 항목 | 현재 상태 | 확장 시 대응 방안 |
|---|---|---|
| 스케줄러 처리 방식 | `findAll()` 순차 처리 | Spring Batch Chunk 처리 (페이징, 실패 청크 재시도) |
| 전체 완료 보너스 중복 지급 | 단일 서버에서는 안전 | `SELECT FOR UPDATE` 또는 DB `UNIQUE` 제약으로 분산 환경 대응 |
| todos 테이블 적재량 | 날짜 무제한 누적 | 파티셔닝(assigned_date 기준) 또는 오래된 데이터 아카이브 |
| 할 일 알림 | 미구현 | 완료 시 `TodoCompleteResult.leveledUp` 활용하여 푸시 알림 트리거 |
| 공부방 연결 라우팅 | `learningType` 필드 반환 | 클라이언트에서 `learningType` 값으로 탭 이동 처리 |
| 일요일 자유 선택 | `learningType = null` 반환 | 클라이언트에서 null이면 전체 공부방 목록으로 이동 |
| Agent 할 일 개수 제한 | 5개 초과 시 예외 | 기획 변경 시 `MAX_TODOS_PER_DAY` 상수 하나만 수정 |
