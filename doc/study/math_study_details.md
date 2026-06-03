# 공부방 수학 학습 — 상세 설계 문서

> 기능명세: LRN_MATH_01 ~ LRN_MATH_33  
> 관련 PRD: `doc/study/math_prd.md`

---

## 1. 배경 및 목적

느린학습자(경계선 지능) 아동은 학습 집중력이 짧고 반복 학습에 대한 피로도가 높습니다. BiFriends 수학 학습은 이를 고려해 짧은 사이클(개념 → 선택형 → 단답형) 구조를 설계하고, 서버가 정답을 관리함으로써 클라이언트 조작을 방지합니다.

MVP에서는 초3~초6, 학년당 3스텝, 스텝당 5사이클(총 15문제)을 제공합니다. 목적은 콘텐츠 양 확장이 아닌 **학습 구조의 유효성 검증**입니다.

---

## 2. 설계 결정

### 2-1. 정답 저장 위치: 서버 vs 클라이언트

| 구분 | 서버 검증 (채택) | 클라이언트 포함 |
|------|----------------|----------------|
| 보안 | 정답 노출 없음 ✅ | APK 분석으로 노출 가능 ❌ |
| 네트워크 | 문제당 1회 요청 필요 | 오프라인 가능 |
| 구현 복잡도 | API 1개 추가 | 없음 |
| 채택 이유 | 보안이 단순함보다 우선 |  |

`content_json`의 `answer`와 `explanation` 필드는 DB에만 존재하며, 콘텐츠 조회 API(`GET /steps/{stepId}/content`) 응답에서 `sanitizeCycleNode()`가 명시적으로 제거합니다.

### 2-2. 콘텐츠 JSON 저장: JSONB vs TEXT + Converter

| 구분 | `@Convert` + TEXT (채택) | hypersistence JSONB |
|------|--------------------------|---------------------|
| 의존성 | 없음 ✅ | `io.hypersistence:hypersistence-utils-hibernate-63` 추가 필요 |
| 쿼리 | JSON 경로 쿼리 불가 | `->>`  등 PostgreSQL JSON 연산자 사용 가능 |
| 채택 이유 | MVP 단계에서 JSON 필드 기반 쿼리 불필요, 의존성 최소화 우선 |

향후 특정 콘텐츠 필드로 검색하거나 필터링이 필요하면 `columnDefinition = "jsonb"`로 변경하고 hypersistence-utils를 추가합니다.

### 2-3. 완료 사이클 추적: `@ElementCollection` vs 별도 Entity

`completedCycles`는 단순 정수 집합으로, 복잡한 쿼리나 연관 관계가 없습니다. `@ElementCollection`으로 `user_math_progress_cycles` 테이블을 자동 생성하며, `MutableSet<Int>` 타입이 중복 추가를 자연스럽게 방지합니다(멱등 보장).

### 2-4. 힌트 상태: 클라이언트 관리

힌트 단계(몇 번 틀렸는가)는 사이클 내 임시 UI 상태입니다. LRN_MATH_30에 따라 사이클 중도 종료 시 처음부터 재시작하므로, 서버에 저장할 실익이 없습니다. 힌트 배열은 `content_json`에 포함되어 클라이언트가 직접 관리합니다.

---

## 3. 소프트웨어 설계 고려 사항

### 3-1. `lastAccessedAt` 갱신 시점

| 시점 | 이유 |
|------|------|
| `getStepContent()` 호출 시 | 스텝 진입 즉시 갱신 → 앱 재진입 시 마지막 위치 정확히 복원 (LRN_MATH_06) |
| `completeCycle()` 호출 시 | 사이클 완료 시점도 최신 접근으로 간주 |
| `completeStep()` 호출 시 | 스텝 완료 시점도 최신 접근으로 간주 |

`getStepContent()`가 `@Transactional` Write 트랜잭션을 가지는 이유는 `lastAccessedAt` 갱신 때문입니다. 클래스 레벨은 `readOnly = true`이므로 메서드에 `@Transactional`을 명시하여 Write 트랜잭션으로 오버라이드합니다.

### 3-2. 스텝 잠금 규칙

```
step_number=1 → 항상 AVAILABLE
step_number=N → 이전 step(N-1)의 isStepCompleted=true 이면 AVAILABLE, 아니면 LOCKED
```

잠금 계산은 쿼리가 아닌 애플리케이션 레이어에서 수행합니다. 학년당 스텝 수가 3개로 고정이므로 루프 비용이 무시할 수준입니다.

### 3-3. content_json 키 케이스 (snake_case)

시드 데이터(`math_seed.sql`)는 `cycle_number`, `cycle_type`, `confirm_button_text` 등 snake_case 키를 사용합니다. 서버는 JsonNode를 그대로 반환하므로 클라이언트도 snake_case로 파싱해야 합니다.

> **API 정규화**: `LearningCycleContentSanitizer`가 응답 시 `cycle`→`cycle_number`, `type`→`cycle_type`(국어 시드)을 통일합니다. DB 시드는 변경하지 않습니다.

### 3-4. `findOrCreateProgress` 패턴

사이클 완료 / 스텝 완료 / 콘텐츠 조회 모두 `UserMathProgress` 행이 없을 수 있습니다. 첫 진입 시 자동 생성하여 `lastAccessedAt`만 갱신하는 패턴을 공통 헬퍼로 추출했습니다.

```kotlin
private fun findOrCreateProgress(member: Member, step: MathStep): UserMathProgress {
    return userMathProgressRepository.findByMemberIdAndMathStepId(member.id, step.id)
        ?: userMathProgressRepository.save(UserMathProgress(member = member, mathStep = step))
}
```

---

## 4. 패키지 구조 설계 이유

```
domain/study/          ← 수학 학습 전용 도메인
├── controller/        ← REST 엔드포인트만 담당
├── service/           ← 비즈니스 로직 (잠금 계산, 정답 비교, 진도 집계)
├── dto/               ← 모든 Request/Response DTO 한 파일(StudyMathDtos.kt)로 관리
├── model/             ← JPA Entity + StepStatus enum
└── repository/        ← Spring Data JPA

infrastructure/converter/
└── JsonNodeConverter.kt   ← 범용 JsonNode ↔ String 변환기 (study 도메인에 한정되지 않음)
```

`JsonNodeConverter`를 `study/` 내부가 아닌 `infrastructure/converter/`에 배치한 이유는, 향후 다른 도메인에서도 JsonNode를 JPA Column으로 저장할 수 있기 때문입니다.

---

## 5. 향후 고려 사항

| 항목 | MVP 한계 | 확장 방안 |
|------|---------|----------|
| 국어 등 타 과목 | 수학 전용 API | `domain/study/` 내에 과목별 service/controller 추가 또는 공통 추상화 |
| 콘텐츠 관리 도구 | 직접 SQL 편집 | 관리자 API(`/api/v1/admin/math-steps`) 추가 |
| content_json 필드 검색 | 불가 | `columnDefinition = "jsonb"` 전환 + hypersistence-utils |
| 오답 통계 | 미수집 | `answer_log` 테이블 추가 |
| 다국어 힌트 | 한국어 고정 | `content_json` 다국어 키 확장 |
| 이미지 CDN | 파일명만 저장 | CDN URL prefix를 `application.yml`로 관리 |
