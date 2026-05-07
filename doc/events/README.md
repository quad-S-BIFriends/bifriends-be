# 크로스 도메인 이벤트 레지스트리

> 이 파일은 도메인 간 Spring ApplicationEvent 흐름을 한눈에 파악하기 위한 단일 진입점이다.
> 새 이벤트를 추가할 때 반드시 이 표에 행을 추가하고, 발행·수신 도메인의 `_details.md`에도 섹션을 추가한다.

---

## 이벤트 목록

| 이벤트 클래스 | 발행 도메인 | 수신 도메인 | 트랜잭션 페이즈 | 발행 시점 | 비즈니스 이유 |
|---|---|---|---|---|---|
| `MemberRegisteredEvent` | `member` | `home` | `AFTER_COMMIT` | 신규 회원 DB 저장 완료 후 | 가입 당일 스케줄러를 이미 지나쳤어도 오늘의 할 일을 즉시 생성 |

---

## 이벤트 상세

### `MemberRegisteredEvent`

**목적**  
신규 가입 회원이 당일 자정 스케줄러 실행 이후에 가입했을 경우, 오늘의 할 일 3개가 생성되지 않는 문제를 해결한다.

**발행**

```
domain/member/event/MemberRegisteredEvent.kt   # 이벤트 클래스 (data class, Member 포함)
domain/member/service/MemberService.kt         # 신규 회원 저장 후 publishEvent() 호출
```

```kotlin
// MemberService.findOrCreateMember() — orElseGet 블록
val member = memberRepository.save(Member(...))
eventPublisher.publishEvent(MemberRegisteredEvent(member))
```

**수신**

```
domain/home/service/MemberRegistrationEventListener.kt
```

```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun onMemberRegistered(event: MemberRegisteredEvent) {
    todoService.generateDailyTodos(event.member, LocalDate.now(KST))
}
```

**`AFTER_COMMIT`을 선택한 이유**

| 페이즈 | 동작 | 결과 |
|---|---|---|
| `AFTER_COMMIT` (채택) | 회원 저장 트랜잭션이 커밋된 뒤 별도 트랜잭션으로 리스너 실행 | todo 생성 실패해도 회원가입은 이미 완료 → fail-safe |
| `BEFORE_COMMIT` (기본값) | 같은 트랜잭션 내 실행 | todo 생성 실패 시 회원가입까지 롤백 → 바람직하지 않음 |

todo는 스케줄러가 매일 00:00에 멱등하게 재생성하므로, 즉시 생성에 실패해도 치명적이지 않다. 반면 회원가입이 롤백되면 사용자는 앱에 진입하지 못한다. 두 작업의 중요도 차이가 `AFTER_COMMIT` 선택의 근거다.

**멱등성**  
`generateDailyTodos()`는 이미 오늘 할 일이 존재하면 즉시 반환한다. 이벤트가 중복 발행되더라도 할 일이 두 번 생성되지 않는다.

**관련 문서**  
- [할 일 상세 설계 문서 — §4.5 신규 가입 시 즉시 생성](../todo/todo_details.md)

---

## 새 이벤트 추가 방법

1. **이벤트 클래스 생성**: 발행 도메인의 `event/` 패키지에 `data class`로 생성
2. **발행 코드 추가**: 발행 서비스에서 `ApplicationEventPublisher.publishEvent()` 호출
3. **리스너 생성**: 수신 도메인의 `service/` 패키지에 `@Component` + `@TransactionalEventListener` 작성
4. **이 파일 업데이트**: 위 이벤트 목록 표에 행 추가
5. **관련 `_details.md` 업데이트**: 발행·수신 도메인 각각의 상세 문서에 이벤트 섹션 추가
