# 보상 규칙 설계 문서

> 작성일: 2026-05-06  
> 대상 모듈: `domain/home`  
> 관련 기능명세: Functional Specification — 단위 시스템: 홈 (HOM-08-01)

---

## 1. 배경 및 목적

BiFriends는 느린학습자(경계선 지능) 아동을 대상으로 하는 앱이다.  
아이들이 학습 활동(퀴즈 풀기, 감정 시나리오, 챗봇 대화)을 꾸준히 이어가도록 **풀(Pool)** 이라는 보상 화폐와 **레벨 시스템**을 도입했다.

보상 규칙 설계의 핵심 목표는 두 가지다.

- **동기 유지**: 매일 접속하고 학습할수록 보상이 쌓인다는 것을 아이가 느낄 수 있어야 한다.
- **규칙의 일관성**: 보상 지급 방식이 코드 곳곳에 흩어지지 않고 한 곳에서 관리되어야 한다.

---

## 2. 풀(Pool) 시스템 개요

### 2.1 풀을 두 가지로 분리한 이유

풀은 단순히 하나의 숫자로 관리하지 않고, 역할에 따라 두 개의 필드로 분리한다.

| 필드 | 역할 | 증가 | 감소 |
|---|---|---|---|
| `totalPoolEarned` | 누적 획득 풀 (레벨 계산 기준) | 보상 획득 시 | **절대 감소하지 않음** |
| `availablePool` | 현재 사용 가능한 풀 (상점 소비용) | 보상 획득 시 | 상점 구매 시 |

**분리하지 않았을 때의 문제**

풀을 하나로 관리하면, 상점에서 아이템을 구매할 때마다 레벨이 떨어지는 현상이 발생한다.

```
획득: +10풀 → pool = 10  (Lv2 달성!)
획득: +15풀 → pool = 25  (Lv3 달성!)
상점 구매: -24풀 → pool = 1 → 레벨이 Lv1로 하락 ❌
```

레벨은 "아이가 얼마나 열심히 학습했는가"를 나타내는 **학습 진행 기록**이다.  
상점에서 아이템을 구매했다고 학습 기록이 사라지는 것은 이 앱의 교육 철학과 맞지 않는다.

**분리한 후의 동작**

```
획득: +10풀 → totalPoolEarned = 10, availablePool = 10  (Lv2 달성!)
획득: +15풀 → totalPoolEarned = 25, availablePool = 25  (Lv3 달성!)
상점 구매: -24풀 → totalPoolEarned = 25 (그대로), availablePool = 1

→ 레벨은 Lv3 유지 ✅
```

`totalPoolEarned`는 건드리지 않으므로 레벨은 절대 내려가지 않는다.  
`availablePool`은 지갑처럼 쓰고 채우며 순환한다.

---

## 3. 레벨 시스템 설계

### 3.1 레벨업 요구 풀 (선형 증가)

기능명세서 기준으로 레벨업에 필요한 풀은 선형으로 증가한다.

```
Lv N → N+1 에 필요한 풀 = 10 + (N-1) × 5
```

| 레벨업 구간 | 필요 풀 | 누적 필요 풀 |
|---|---|---|
| Lv1 → Lv2 | 10 | 10 |
| Lv2 → Lv3 | 15 | 25 |
| Lv3 → Lv4 | 20 | 45 |
| Lv4 → Lv5 | 25 | 70 |
| Lv5 → Lv6 | 30 | 100 |
| Lv6 → Lv7 | 35 | 135 |
| 이후 | +5씩 증가 | — |

### 3.2 누적 풀로 레벨을 역산하는 공식

레벨 N에 도달하기 위한 누적 풀을 수식으로 정리하면:

```
cumulativePoolForLevel(N) = 5 × (N-1) × (N+2) / 2
```

검증:
- Lv2: 5 × 1 × 4 / 2 = **10** ✅  
- Lv3: 5 × 2 × 5 / 2 = **25** ✅  
- Lv4: 5 × 3 × 6 / 2 = **45** ✅  

이 공식은 `LevelPolicy.kt`의 `cumulativePoolForLevel()` 함수로 구현되어 있다.  
`calculateLevel(totalPoolEarned)`은 이 공식을 이용해 누적 풀에서 현재 레벨을 역산한다.

**경계값 검증 결과**

```
totalPoolEarned =  0 ~ 9   → Lv1
totalPoolEarned = 10 ~ 24  → Lv2
totalPoolEarned = 25 ~ 44  → Lv3
totalPoolEarned = 45 ~ 69  → Lv4
totalPoolEarned = 70 ~ 99  → Lv5
```

### 3.3 구현 위치: `LevelPolicy.kt`

레벨 계산 로직은 JPA 엔티티나 서비스가 아닌 **순수 `object`** 로 분리했다.

```kotlin
object LevelPolicy {
    fun poolRequiredForLevelUp(fromLevel: Int): Int = 10 + (fromLevel - 1) * 5
    fun cumulativePoolForLevel(level: Int): Int = 5 * (level - 1) * (level + 2) / 2
    fun calculateLevel(totalPoolEarned: Int): Int { ... }
    fun progressInCurrentLevel(totalPoolEarned: Int): Int { ... }
    fun poolNeededForNextLevel(totalPoolEarned: Int): Int { ... }
}
```

**이렇게 분리한 세 가지 이유:**

**① DB나 Spring Context 없이도 테스트할 수 있어야 한다**

레벨 계산 로직이 `UserStatsService` 안에 섞여 있으면, "25풀이면 Lv3이 맞나?" 라는 단순한 수식 검증을 위해서도 Spring 서버를 띄우고 DB 연결까지 해야 테스트할 수 있다. 레벨 계산은 DB와 아무 상관 없는 순수한 수식인데 그 무게를 떠안는 건 낭비다.

`LevelPolicy`를 독립 `object`로 분리하면 Spring/DB 없이 바로 테스트할 수 있다.

```kotlin
// Spring 없이 그냥 실행되는 단위 테스트
class LevelPolicyTest {
    @Test
    fun `25풀이면 Lv3이다`() {
        assertThat(LevelPolicy.calculateLevel(25)).isEqualTo(3)
    }

    @Test
    fun `24풀은 아직 Lv2다`() {
        assertThat(LevelPolicy.calculateLevel(24)).isEqualTo(2)
    }
}
```

**② `object`로 만들면 인스턴스 생성 없이 바로 호출할 수 있다**

Kotlin의 `object`는 Java의 `static`과 비슷하다. 인스턴스를 따로 만들 필요 없이 이름으로 바로 접근할 수 있다.

```kotlin
// object → 인스턴스 생성 없이 바로 호출
LevelPolicy.calculateLevel(25)

// class였다면 → 쓸 때마다 인스턴스를 만들어야 함
val policy = LevelPolicy()
policy.calculateLevel(25)
```

레벨 계산 함수들은 내부 상태(데이터)를 가질 필요가 없는 순수 함수들의 모음이므로 `object`가 딱 맞는 형태다.

**③ 레벨 공식이 바뀌어도 이 파일 하나만 수정하면 전체에 반영된다**

기획이 바뀌어서 "레벨업 요구 풀을 선형이 아니라 지수로 바꿔주세요" 라는 요청이 오면, `LevelPolicy.kt` 한 파일의 한 줄만 고치면 된다.

```kotlin
// LevelPolicy.kt 한 파일만 수정
fun poolRequiredForLevelUp(fromLevel: Int): Int {
    // 기존: return 10 + (fromLevel - 1) * 5  (선형)
    return 10 * fromLevel                       // 변경: 지수로
}
```

이 파일 하나를 고치면 `UserStats.earnPool()`, `UserStatsService`, `TodoService` 등 이 함수를 쓰는 모든 곳에 자동으로 반영된다. 반대로 레벨 계산 로직이 각 서비스에 복붙되어 흩어져 있었다면, 모든 서비스 파일을 다 찾아서 고쳐야 하고 한 군데라도 빠뜨리면 버그가 된다.

---

## 4. 보상 규칙 상세

### 4.1 보상 출처별 지급량 (`RewardPolicy.kt`)

| 출처 (`RewardSource`) | 설명 | 지급 풀 |
|---|---|---|
| `ATTENDANCE` | 연속 출석 | streak 일수에 따라 차등 (아래 참고) |
| `LEARNING_CORRECT` | 문제 1개 정답 | +1풀 |
| `LEARNING_SET_COMPLETE` | 3문제 세트 완료 보너스 | +2풀 (정답 보상과 별개) |
| `EMOTION` | 마음 시나리오 1개 완료 | +3풀 |
| `TODO_SINGLE` | 할 일 1개 완료 | +1풀 |
| `TODO_ALL_COMPLETE` | 오늘의 할 일 전체 완료 보너스 | +3풀 |

> 배움 탭 1세트 최대 총 획득량: 정답 3개(3풀) + 세트 보너스(2풀) = **5풀**

### 4.2 연속 출석 보상 (streak 기반 차등)

```
1일:     +3풀
2일:     +4풀
3일:     +5풀
4일 이상: +5풀 (고정)
```

매일 00:00 KST 기준으로 초기화되며, 하루라도 빠지면 streak은 1일차로 리셋된다.

### 4.3 일일 기대 보상량

```
출석:           3 ~ 5풀
배움 1세션:     약 5풀
마음 1회:       약 3풀
할 일 완료:     약 4 ~ 5풀 (개별 1풀 × 3 + 전체 완료 보너스 3풀)
─────────────────────────────────
일일 합계:      약 15 ~ 18풀
```

---

## 5. 소프트웨어 설계 고려 사항

### 5.1 도메인 로직의 엔티티 캡슐화 (Rich Domain Model)

보상 관련 비즈니스 로직은 `UserStats` 엔티티 내부 메서드로 캡슐화했다.

**잘못된 방식 — 서비스가 필드를 직접 수정 (Anemic Model)**

```kotlin
// UserStatsService
fun earnPool(memberId: Long, amount: Int) {
    stats.totalPoolEarned += amount   // 필드 직접 수정
    stats.availablePool += amount     // 필드 직접 수정
    val newLevel = LevelPolicy.calculateLevel(stats.totalPoolEarned)
    stats.level = newLevel            // 필드 직접 수정
}
```

이 방식은 같은 로직이 `TodoService`, `LearningService`, `EmotionService` 등 여러 곳에 **복붙**된다.  
나중에 "풀 획득 시 알림을 보내야 해요" 같은 요구사항이 생기면 모든 서비스를 다 찾아서 고쳐야 하고, 한 군데라도 빠뜨리면 버그가 된다.

**현재 방식 — 엔티티가 규칙을 스스로 알고 있음 (Rich Domain Model)**

```kotlin
// UserStats 엔티티 — "어떻게 처리할지" 책임짐
fun earnPool(amount: Int): Boolean {
    totalPoolEarned += amount
    availablePool += amount
    val newLevel = LevelPolicy.calculateLevel(totalPoolEarned)
    val leveledUp = newLevel > level
    level = newLevel
    return leveledUp
}

// TodoService — "얼마를 줄지"만 결정, 내부 처리는 모름
fun completeTodo(todoId: Long) {
    stats.earnPool(RewardPolicy.TODO_SINGLE)  // 한 줄로 끝
}

// LearningService — 동일하게 한 줄로 끝
fun completeQuestion(questionId: Long) {
    stats.earnPool(RewardPolicy.LEARNING_CORRECT)
}
```

**서비스는 "언제, 얼마를" 결정하고, 엔티티는 "어떻게 처리할지"를 책임진다.**  
레벨업 로직이 `UserStats.earnPool()` 한 곳에만 있으므로, 규칙이 바뀌어도 수정 지점이 단 하나다.

### 5.2 멱등성 보장 (`recordAttendance()`)

**멱등성(Idempotency)** 이란 "같은 연산을 여러 번 실행해도 결과가 달라지지 않는 성질"이다.

앱 특성상 사용자는 홈 화면에 하루에 여러 번 진입한다.  
진입할 때마다 출석 보상을 지급하면 하루에 수십 번의 보상을 받을 수 있게 된다.

```kotlin
fun recordAttendance(today: LocalDate): Int? {
    if (lastLoginDate == today) return null  // ← 이 한 줄이 멱등성 보장
    ...
    return RewardPolicy.attendancePool(streakDays)
}
```

`lastLoginDate == today`이면 `null`을 반환하고, 서비스에서는 `null`이면 `earnPool()`을 호출하지 않는다.

```
1번째 홈 진입: lastLoginDate = null    → streak 갱신, 3풀 지급, lastLoginDate = today
2번째 홈 진입: lastLoginDate == today  → null 반환, 보상 없음
N번째 홈 진입: lastLoginDate == today  → null 반환, 보상 없음
```

**현재 방식의 한계와 MVP 판단:**  
단일 서버에서는 완벽하게 동작한다. 단, 서버가 여러 대로 스케일 아웃된 환경에서는 두 요청이 동시에 들어올 경우 `lastLoginDate`를 동시에 읽어 둘 다 미처리로 판단할 수 있다. 이 경우 `SELECT FOR UPDATE` 락이나 DB `UNIQUE` 제약이 추가로 필요하다. MVP 단계에서는 현재 구조로 충분하다고 판단했다.

### 5.3 보상 이력의 Append-Only 설계 (`RewardHistory`)

`RewardHistory` 테이블은 수정하지 않는 **append-only** 로그다.

```kotlin
@Entity
class RewardHistory(
    val source: RewardSource,          // 어떤 행동으로
    val amount: Int,                   // 얼마를
    val refId: Long? = null,           // 관련 엔티티 ID (todo.id 등)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

이렇게 설계한 이유는 세 가지다.

- **감사(Audit) 로그**: "누가 언제 어떤 행동으로 풀을 받았는가"를 추적할 수 있다. 유저 문의("내 풀이 왜 이것밖에 없어요?") 대응 시 이력을 바로 조회할 수 있다.
- **데이터 무결성**: 실수로 잘못 지급된 경우에도 기존 레코드를 수정하지 않고 보정 레코드를 추가하는 방식으로 처리한다.
- **향후 분석**: 어떤 활동이 가장 많이 이루어지는지, 어떤 보상이 동기 부여에 효과적인지 데이터로 확인할 수 있다.

---

## 6. `domain/home` 패키지 구조 설계 이유

```
domain/home/
├── model/
│   ├── LevelPolicy.kt       # 레벨 계산 순수 도메인 로직
│   ├── RewardPolicy.kt      # 보상 지급량 정책 상수
│   ├── RewardSource.kt      # 보상 출처 enum
│   ├── UserStats.kt         # 엔티티 (도메인 메서드 포함)
│   └── RewardHistory.kt     # 보상 이력 엔티티 (append-only)
├── repository/
│   ├── UserStatsRepository.kt
│   └── RewardHistoryRepository.kt
├── service/
│   └── UserStatsService.kt
└── dto/
    └── UserStatsDtos.kt
```

### 6.1 왜 `domain/home/` 으로 묶었나

`UserStats`와 `RewardHistory`는 홈 화면 진입 시 처리되는 개념들이다. 이 데이터들은 홈의 상단 영역(레벨 표시)과 출석 보상, 할 일 완료 보상과 직결된다. `domain/member/`에 넣으면 member 도메인이 보상 규칙까지 알게 되어 책임이 과도하게 커진다. 기능 단위로 묶어 응집도를 높이기 위해 `domain/home/`으로 분리했다.

### 6.2 `model/` 안에 Policy 객체를 함께 둔 이유

`LevelPolicy`, `RewardPolicy`는 JPA 엔티티가 아닌 순수 Kotlin `object`다. 그럼에도 `model/`에 넣은 이유는 이것들이 **도메인 규칙 그 자체**이기 때문이다.

DB나 Spring과 무관하게 "이 도메인이 어떻게 동작하는가"를 정의하는 객체이므로, 인프라 계층(`infrastructure/`)이나 서비스 계층이 아닌 모델 계층에 위치하는 것이 자연스럽다. 또한 `UserStats` 엔티티가 `LevelPolicy`를 직접 참조하므로 같은 패키지에 두는 것이 의존 방향을 명확하게 한다.

### 6.3 `service/`의 역할 경계

`UserStatsService`는 다음 두 가지만 담당한다.

- **오케스트레이션**: `UserStats`, `RewardHistory`, `MemberRepository`를 조합해서 트랜잭션 단위로 묶는다.
- **결정권**: 어떤 상황에서 얼마의 보상을 지급할지 결정한다.

비즈니스 규칙 자체(레벨업 계산, streak 갱신, 풀 차감 검증)는 서비스가 직접 구현하지 않고 엔티티와 Policy 객체에 위임한다.

### 6.4 `dto/`를 별도 분리한 이유

API 응답 형태(DTO)와 내부 도메인 모델(Entity)을 분리하는 이유는 두 가지다.

- **JPA 안전성**: 엔티티를 그대로 직렬화하면, 지연 로딩 중인 연관 관계가 JSON 변환 시점에 예기치 않게 쿼리를 발생시키거나 `LazyInitializationException`을 던질 수 있다.
- **결합도 감소**: API 스펙과 DB 스펙이 강하게 결합되면, 테이블 컬럼 하나를 바꿀 때 API 응답 형식도 함께 바뀐다. DTO를 분리하면 둘을 독립적으로 변경할 수 있다.

`UserStatsResponse.from(stats)`처럼 변환 책임을 DTO 내부의 `companion object`에 두어 서비스가 변환 로직을 알 필요 없도록 했다.

---

## 7. 향후 고려 사항

| 항목 | 현재 상태 | 확장 시 대응 방안 |
|---|---|---|
| 출석 멱등성 | `lastLoginDate` 필드 비교 | `SELECT FOR UPDATE` 또는 분산 락 |
| 할 일 자동 생성 | 미구현 | `@Scheduled` → 사용자 규모에 따라 Spring Batch |
| 레벨업 알림 | 미구현 | `earnPool()` 반환값(`Boolean`) 활용하여 푸시 알림 트리거 |
| 레벨 상한 | 없음 | 필요 시 `LevelPolicy`에 `MAX_LEVEL` 상수 추가 |
| 풀 만료 정책 | 없음 | `availablePool`에 만료일 컬럼 추가 고려 |