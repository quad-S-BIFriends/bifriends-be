# 홈 - 오늘의 할 일 (Todo) 구현 요약

> 관련 기능명세: Functional Specification — 단위 시스템: 홈 (HOM-08-08, HOM-08-09)  
> 관련 문서: [상세 설계 문서](./todo_details.md)

---

## 구현 범위

| 항목 | 파일 | 설명 |
|---|---|---|
| 할 일 엔티티 | `model/Todo.kt` | assignedDate 기반 이력 보존 설계 |
| 할 일 유형 | `model/TodoType.kt` | CHAT / LEARNING / EMOTION / CUSTOM |
| 완료 상태 | `model/TodoStatus.kt` | PENDING / COMPLETED |
| 생성 출처 | `model/TodoSource.kt` | SYSTEM / AGENT |
| 학습 과목 | `model/LearningType.kt` | MATH / LANGUAGE |
| 요일별 과목 분기 | `model/LearningTypePolicy.kt` | 월·수·금→수학, 화·목·토→국어, 일→자유 |
| 저장소 | `repository/TodoRepository.kt` | 날짜·상태 기반 조회 메서드 포함 |
| 서비스 | `service/TodoService.kt` | 생성·완료·Agent CRUD |
| 스케줄러 | `service/TodoScheduler.kt` | 매일 00:00 KST 자동 실행 |
| 가입 즉시 생성 | `service/MemberRegistrationEventListener.kt` | 신규 가입 시 당일 할 일 즉시 생성 ([이벤트 상세](../events/README.md)) |
| 스케줄링 설정 | `global/config/SchedulingConfig.kt` | @EnableScheduling 활성화 |

---

## 핵심 설계 결정

### 삭제 없이 날짜 컬럼으로 구분 (B안 채택)

기능명세서 원안은 매일 기존 할 일을 삭제 후 재생성하는 방식이었으나, `assignedDate: LocalDate` 컬럼을 두고 오늘 날짜 기준으로 조회하는 방식으로 변경했다.

```
홈 화면 조회: WHERE member_id = ? AND assigned_date = '오늘'
성장일기 조회: WHERE member_id = ? AND assigned_date BETWEEN '시작' AND '끝'
```

과거 완료 이력이 그대로 남아, 추후 성장일기·학습 통계 기능에 재활용 가능하다.

### 스케줄러 멱등성

스케줄러가 중복 실행되더라도 할 일이 두 번 생성되지 않도록 진입 시 체크한다.

```kotlin
if (todoRepository.existsByMemberIdAndAssignedDate(member.id, today)) return
```

### SYSTEM 할 일 보호

`source = SYSTEM`인 할 일은 엔티티 메서드 수준에서 수정·삭제를 차단한다.

```kotlin
fun updateTitle(newTitle: String) {
    check(source == TodoSource.AGENT) { "시스템 할 일은 수정할 수 없습니다." }
}
```

### 전체 완료 보너스 처리

단일 완료(+1풀)와 전체 완료 보너스(+3풀)를 하나의 트랜잭션에서 처리한다.  
별도 배치 없이 `completeTodo()` 호출 시점에 `PENDING` 개수가 0인지 체크해서 즉시 지급한다.

---

## DB 테이블: `todos`

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

---

## 다음 단계

- [ ] 홈 단일 조회 API (`GET /api/v1/home`) — 레벨 정보 + 인사 메시지 + 할 일 목록
- [ ] 할 일 완료 API (`PATCH /api/v1/todos/{id}/complete`)
- [ ] Agent용 Todo CRUD API
