# 공부방 — 수학 학습 백엔드 PRD

> **기능명세 출처**: LRN_MATH_01 ~ LRN_MATH_33  
> **MVP 범위**: 초3~초6, 학년당 3스텝, 스텝당 5사이클 (총 12스텝)  
> **이 문서의 목적**: Claude Code가 이 문서만 읽고 구현을 완료할 수 있도록 작성된 기술 명세서

---

## 1. 구현 범위 요약

| 항목 | 내용 |
|------|------|
| 신규 도메인 | `domain/study/` |
| 신규 Entity | `MathStep`, `UserMathProgress` |
| 신규 API | 6개 (아래 명세 참고) |
| 콘텐츠 저장 방식 | PostgreSQL JSONB (`math_step.content_json`) |
| 초기 데이터 | `src/main/resources/db/math_seed.sql` — 12개 스텝 INSERT |
| 정답 검증 | 서버에서 수행 (콘텐츠 응답 시 `answer`, `explanation` 필드 제거) |
| 힌트 상태 | 클라이언트 관리 (서버 저장 안 함) |
| 사이클 중도 종료 | 저장 안 함 — 사이클 단위로만 완료 저장 (LRN_MATH_30) |
| SecurityConfig | `/api/v1/study/**` 인증 필요 경로 추가 |

---

## 2. 도메인 규칙 (비즈니스 로직)

### 스텝 잠금 규칙 (LRN_MATH_03, 04)
- 학년 내 스텝은 `step_number` 순서로 순차 진행
- `step_number = 1`은 항상 진행 가능 (잠금 없음)
- `step_number = N`은 `step_number = N-1`이 완료되어야 잠금 해제
- 완료한 스텝은 재진입(복습) 가능 (LRN_MATH_05)

### 사이클 완료 규칙 (LRN_MATH_18, 25, 27)
- 사이클 내 3문제 모두 정답 처리 시 해당 사이클 완료
- 사이클 5 완료 → 스텝 완료
- 진행 중 앱 종료 시 해당 사이클은 처음부터 재시작 (서버는 미완료 사이클 상태를 저장하지 않음)

### 학년 결정 규칙 (LRN_MATH_33)
- 로드맵 조회 시 `members.grade` 컬럼 기준으로 해당 학년의 스텝만 반환
- `grade`가 null이면 403 또는 빈 목록 반환

### 마지막 위치 (LRN_MATH_06)
- `user_math_progress.last_accessed_at`이 가장 최근인 미완료 스텝을 "마지막 위치"로 반환
- 미완료 스텝이 없으면 (전부 완료) 마지막 완료 스텝 반환

---

## 3. DB 스키마

```sql
-- 스텝 메타 + 콘텐츠 원본 (정답 포함)
CREATE TABLE math_step (
    id           BIGSERIAL    PRIMARY KEY,
    grade        INTEGER      NOT NULL,          -- 3, 4, 5, 6
    step_number  INTEGER      NOT NULL,          -- 학년 내 순서 1, 2, 3
    step_title   VARCHAR(100) NOT NULL,
    concept      VARCHAR(200) NOT NULL,
    content_json JSONB        NOT NULL,          -- 전체 사이클 데이터 (정답 포함 원본)
    CONSTRAINT uq_math_step_grade_number UNIQUE (grade, step_number)
);

-- 사용자별 수학 진도
CREATE TABLE user_math_progress (
    id                 BIGSERIAL PRIMARY KEY,
    member_id          BIGINT    NOT NULL REFERENCES members(id),
    math_step_id       BIGINT    NOT NULL REFERENCES math_step(id),
    completed_cycles   INTEGER[] NOT NULL DEFAULT '{}',  -- 완료된 사이클 번호 배열 ex) {1,2,3}
    is_step_completed  BOOLEAN   NOT NULL DEFAULT FALSE,
    last_accessed_at   TIMESTAMP,
    CONSTRAINT uq_user_math_progress UNIQUE (member_id, math_step_id)
);

CREATE INDEX idx_user_math_progress_member ON user_math_progress(member_id);
```

---

## 4. API 명세

### 공통
- Base path: `/api/v1/study/math`
- 모든 API: `Authorization: Bearer <JWT>` 필수
- 오류 응답: 기존 `ErrorResponse` 형식 준수

---

### 4-1. 로드맵 조회

```
GET /api/v1/study/math/roadmap
```

**설명**: 로그인 사용자의 학년에 해당하는 스텝 목록과 각 스텝 상태를 반환합니다.

**응답 예시**:
```json
{
  "grade": 3,
  "lastStepId": 2,
  "steps": [
    {
      "stepId": 1,
      "stepNumber": 1,
      "stepTitle": "마트에서 장보기",
      "concept": "받아올림/내림 없는 세 자리 수 덧셈과 뺄셈",
      "status": "COMPLETED",
      "completedCycles": [1, 2, 3, 4, 5]
    },
    {
      "stepId": 2,
      "stepNumber": 2,
      "stepTitle": "용돈 관리하기",
      "concept": "받아올림/내림 1번 있는 세 자리 수 덧셈과 뺄셈",
      "status": "IN_PROGRESS",
      "completedCycles": [1, 2]
    },
    {
      "stepId": 3,
      "stepNumber": 3,
      "stepTitle": "생일 파티 준비하기",
      "concept": "받아올림/내림 2번 있는 세 자리 수 덧셈과 뺄셈",
      "status": "LOCKED",
      "completedCycles": []
    }
  ]
}
```

**상태값 정의**:
- `COMPLETED`: `is_step_completed = true`
- `IN_PROGRESS`: `user_math_progress` 행이 존재하고 미완료
- `AVAILABLE`: `user_math_progress` 행 없음 + 잠금 조건 미해당 (첫 스텝 또는 이전 스텝 완료)
- `LOCKED`: 이전 스텝 미완료

**`lastStepId`**: `last_accessed_at` 최신 스텝의 id. 없으면 첫 번째 스텝 id.

---

### 4-2. 스텝 콘텐츠 조회

```
GET /api/v1/study/math/steps/{stepId}/content
```

**설명**: 특정 스텝의 전체 사이클·문제·힌트를 반환합니다. **`answer`, `explanation` 필드는 응답에서 제거합니다.**

**Path Variable**: `stepId` — `math_step.id`

**응답 예시 (grade 3~5 단순 텍스트 타입)**:
```json
{
  "stepId": 1,
  "stepTitle": "마트에서 장보기",
  "concept": "받아올림/내림 없는 세 자리 수 덧셈과 뺄셈",
  "grade": 3,
  "cycles": [
    {
      "cycleNumber": 1,
      "cycleType": "concept",
      "slides": [
        {
          "image": "step1_concept_01.png",
          "text": "마트에서 사과 230원, 우유 150원을 샀어요. 같이 더해볼까요?",
          "confirmButtonText": "확인"
        }
      ]
    },
    {
      "cycleNumber": 2,
      "cycleType": "choice",
      "difficulty": 1,
      "questions": [
        {
          "questionIndex": 0,
          "image": "step1_c2_q1.png",
          "text": "마트에서 빵 310원, 주스 240원을 샀어요. 모두 얼마일까요?",
          "options": ["450원", "550원", "650원"],
          "hint": [
            ["310 + 240을 같이 해봐요!"],
            ["일의 자리부터요. 0 + 0 = 0, 십의 자리 1 + 4 = ?"],
            ["거의 다 왔어요! 310 + 240 = 5□0"]
          ]
        }
      ]
    }
  ]
}
```

**응답 예시 (grade 6 — 분수 렌더링 타입, text가 배열)**:
```json
{
  "cycleNumber": 2,
  "cycleType": "choice",
  "questions": [
    {
      "questionIndex": 0,
      "image": "g6_step1_c2_q1.png",
      "text": [
        { "type": "text", "value": "소금 2kg을 3봉지에 똑같이 나눠 담으면 한 봉지에 몇 kg씩 담을 수 있을까요?" }
      ],
      "options": [
        { "display": "fraction", "numerator": 1, "denominator": 3, "unit": "kg" },
        { "display": "fraction", "numerator": 2, "denominator": 3, "unit": "kg" },
        { "display": "fraction", "numerator": 3, "denominator": 2, "unit": "kg" }
      ],
      "hint": [...]
    }
  ]
}
```

> **구현 주의**: `content_json`을 그대로 역직렬화하지 말고, 응답 DTO에서 `answer`와 `explanation` 키를 명시적으로 제외한다. JSONB를 `JsonNode`로 읽어 해당 필드를 제거한 뒤 반환하거나, 별도 응답 DTO로 매핑한다.

---

### 4-3. 진도 조회

```
GET /api/v1/study/math/progress
```

**설명**: 현재 사용자의 전체 진도와 마지막 접근 스텝을 반환합니다. 앱 재진입 시 로드맵 위치 복원에 사용됩니다 (LRN_MATH_06).

**응답 예시**:
```json
{
  "lastStepId": 2,
  "totalSteps": 3,
  "completedSteps": 1,
  "progress": [
    {
      "stepId": 1,
      "isStepCompleted": true,
      "completedCycles": [1, 2, 3, 4, 5],
      "lastAccessedAt": "2026-05-26T10:30:00"
    },
    {
      "stepId": 2,
      "isStepCompleted": false,
      "completedCycles": [1, 2],
      "lastAccessedAt": "2026-05-26T11:00:00"
    }
  ]
}
```

---

### 4-4. 답안 검증

```
POST /api/v1/study/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate
```

**설명**: 사용자 답안을 서버에서 검증합니다. `answer` 필드는 절대 클라이언트에 노출되지 않습니다.

**Path Variables**:
- `stepId`: `math_step.id`
- `cycleNumber`: 1~5
- `questionIndex`: 0~2 (0-based)

**요청 Body — choice 타입**:
```json
{ "answer": "550원" }
```

**요청 Body — short_answer 타입**:
```json
{ "answer": "557" }
```

**요청 Body — grade 6 분수 타입 (answer가 객체인 경우)**:
```json
{ "answer": { "numerator": 2, "denominator": 3 } }
```

**응답 — 정답**:
```json
{
  "correct": true,
  "explanation": "잘했어요! 310 + 240 = 550원이에요."
}
```

**응답 — 오답**:
```json
{
  "correct": false
}
```

> **정답 비교 로직**:
> - `content_json` → `cycles[cycleNumber-1]` → `questions[questionIndex]` → `answer` 값과 요청 `answer` 비교
> - choice/short_answer: 문자열 동등 비교 (`==`, 공백 trim)
> - grade 6 분수형: numerator, denominator 각각 비교
> - `explanation`은 정답일 때만 `content_json`에서 읽어서 포함

---

### 4-5. 사이클 완료 처리

```
POST /api/v1/study/math/steps/{stepId}/cycles/{cycleNumber}/complete
```

**설명**: 해당 사이클의 3문제를 모두 정답 처리한 후 클라이언트가 호출합니다. 중복 호출 시 멱등 처리합니다.

**요청 Body**: 없음

**응답**:
```json
{
  "stepId": 1,
  "cycleNumber": 2,
  "completedCycles": [1, 2],
  "isStepCompleted": false
}
```

**서비스 로직**:
1. `user_math_progress` 조회 또는 신규 생성 (`findOrCreate`)
2. `completed_cycles` 배열에 `cycleNumber` 추가 (중복 무시)
3. `last_accessed_at` 갱신
4. 저장 후 현재 완료 목록 반환

---

### 4-6. 스텝 완료 처리

```
POST /api/v1/study/math/steps/{stepId}/complete
```

**설명**: 사이클 5 완료 후 클라이언트가 호출합니다. `is_step_completed = true`로 변경합니다.

**요청 Body**: 없음

**응답**:
```json
{
  "stepId": 1,
  "isStepCompleted": true,
  "nextStepId": 2,
  "nextStepStatus": "AVAILABLE"
}
```

**서비스 로직**:
1. `user_math_progress.is_step_completed = true` 업데이트
2. 같은 학년 내 다음 `step_number` 스텝 조회
3. 다음 스텝의 상태 계산 후 반환 (없으면 `nextStepId: null`)

---

## 5. 패키지 구조

```
domain/study/
├── controller/
│   └── StudyMathController.kt
├── service/
│   └── StudyMathService.kt
├── dto/
│   └── StudyMathDtos.kt         -- 모든 Request/Response DTO 정의
├── model/
│   ├── MathStep.kt              -- @Entity, data class 사용 금지
│   ├── UserMathProgress.kt      -- @Entity, data class 사용 금지
│   └── StepStatus.kt            -- enum: COMPLETED, IN_PROGRESS, AVAILABLE, LOCKED
└── repository/
    ├── MathStepRepository.kt
    └── UserMathProgressRepository.kt
```

---

## 6. Entity 명세

### MathStep.kt
```kotlin
@Entity
@Table(name = "math_step")
class MathStep(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val grade: Int,

    @Column(nullable = false)
    val stepNumber: Int,

    @Column(nullable = false)
    val stepTitle: String,

    @Column(nullable = false)
    val concept: String,

    @Type(JsonType::class)           // hypersistence-utils 또는 직접 구현
    @Column(columnDefinition = "jsonb", nullable = false)
    val contentJson: JsonNode,       // com.fasterxml.jackson.databind.JsonNode
)
```

> **JSONB 매핑**: `hypersistence-utils` 의존성 추가 또는 `@Convert`로 직접 구현.
> `build.gradle.kts`에 `implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")` 추가.

### UserMathProgress.kt
```kotlin
@Entity
@Table(name = "user_math_progress")
class UserMathProgress(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "math_step_id", nullable = false)
    val mathStep: MathStep,

    @Column(columnDefinition = "integer[]")
    var completedCycles: Array<Int> = emptyArray(),

    @Column(nullable = false)
    var isStepCompleted: Boolean = false,

    var lastAccessedAt: LocalDateTime? = null,
)
```

---

## 7. Repository 명세

### MathStepRepository.kt
```kotlin
interface MathStepRepository : JpaRepository<MathStep, Long> {
    fun findByGradeOrderByStepNumber(grade: Int): List<MathStep>
    fun findByGradeAndStepNumber(grade: Int, stepNumber: Int): MathStep?
}
```

### UserMathProgressRepository.kt
```kotlin
interface UserMathProgressRepository : JpaRepository<UserMathProgress, Long> {
    fun findByMemberIdAndMathStepId(memberId: Long, mathStepId: Long): UserMathProgress?
    fun findByMemberIdAndMathStepIdIn(memberId: Long, mathStepIds: List<Long>): List<UserMathProgress>
    fun findTopByMemberIdOrderByLastAccessedAtDesc(memberId: Long): UserMathProgress?
}
```

---

## 8. SecurityConfig 수정

`SecurityConfig.kt`의 `authorizeHttpRequests` 블록에 아래 경로를 추가합니다:

```kotlin
.requestMatchers("/api/v1/study/**").authenticated()
```

---

## 9. 초기 데이터 시딩

`src/main/resources/db/math_seed.sql` 파일을 생성하고, 12개 스텝의 JSON을 INSERT합니다.

```sql
INSERT INTO math_step (grade, step_number, step_title, concept, content_json) VALUES
(3, 1, '마트에서 장보기', '받아올림/내림 없는 세 자리 수 덧셈과 뺄셈', '{...}'::jsonb),
(3, 2, '용돈 관리하기', '받아올림/내림 1번 있는 세 자리 수 덧셈과 뺄셈', '{...}'::jsonb),
(3, 3, '생일 파티 준비하기', '받아올림/내림 2번 있는 세 자리 수 덧셈과 뺄셈', '{...}'::jsonb),
(4, 1, '저금통 열기', '만, 다섯 자리 수 읽기와 쓰기', '{...}'::jsonb),
(4, 2, '저금통 돈 불리기', '뛰어세기', '{...}'::jsonb),
(4, 3, '누가 더 많이 모았을까?', '수의 크기 비교', '{...}'::jsonb),
(5, 1, '용돈 기입장 쓰기', '덧셈과 뺄셈이 섞여있는 식', '{...}'::jsonb),
(5, 2, '급식 도우미', '곱셈과 나눗셈이 섞여있는 식', '{...}'::jsonb),
(5, 3, '생일 선물 고르기', '덧셈, 뺄셈, 곱셈이 섞여있는 식', '{...}'::jsonb),
(6, 1, '재료 나눠 담기 (1)', '자연수 ÷ 자연수의 몫을 분수로 나타내기', '{...}'::jsonb),
(6, 2, '재료 나눠 담기 (2)', '분수 ÷ 자연수', '{...}'::jsonb),
(6, 3, '재료 나눠 담기 (3)', '분수 ÷ 자연수를 분수의 곱셈으로 나타내기', '{...}'::jsonb);
```

> **`{...}` 자리에 실제 JSON을 채워 넣어야 합니다.**
> `application.yml`에 `spring.sql.init.data-locations: classpath:db/math_seed.sql` 및
> `spring.sql.init.mode: always`를 추가하거나, 최초 1회 수동 실행합니다.
> 중복 실행 방지를 위해 `INSERT INTO ... ON CONFLICT DO NOTHING` 사용을 권장합니다.

### JSON 원본 위치
각 스텝의 전체 JSON은 아래 변수명으로 참조하십시오 (이 PRD에 첨부된 원본 JSON 그대로 사용):
- 초3: `step_1_grade3_json`, `step_2_grade3_json`, `step_3_grade3_json`
- 초4: `step_1_grade4_json`, `step_2_grade4_json`, `step_3_grade4_json`
- 초5: `step_1_grade5_json`, `step_2_grade5_json`, `step_3_grade5_json`
- 초6: `step_1_grade6_json`, `step_2_grade6_json`, `step_3_grade6_json`

---

## 10. 구현 순서 (Claude Code 작업 순서)

아래 순서대로 구현하십시오. 각 단계 완료 후 다음으로 넘어갑니다.

```
Step 1. build.gradle.kts
        └── hypersistence-utils 의존성 추가

Step 2. DB 스키마
        └── math_step, user_math_progress 테이블 생성
            (ddl-auto: update이므로 Entity 생성 시 자동 반영)

Step 3. Entity 작성
        ├── MathStep.kt
        └── UserMathProgress.kt

Step 4. Repository 작성
        ├── MathStepRepository.kt
        └── UserMathProgressRepository.kt

Step 5. DTO 작성
        └── StudyMathDtos.kt
            (RoadmapResponse, StepContentResponse, ProgressResponse,
             ValidateAnswerRequest, ValidateAnswerResponse,
             CycleCompleteResponse, StepCompleteResponse)

Step 6. Service 작성
        └── StudyMathService.kt
            - getRoadmap(memberId): 로드맵 + 상태 계산
            - getStepContent(stepId): contentJson에서 answer/explanation 제거
            - getProgress(memberId): 진도 + lastStepId
            - validateAnswer(memberId, stepId, cycleNumber, questionIndex, answer): 정답 검증
            - completeCycle(memberId, stepId, cycleNumber): 사이클 완료
            - completeStep(memberId, stepId): 스텝 완료

Step 7. Controller 작성
        └── StudyMathController.kt
            @RequestMapping("/api/v1/study/math")

Step 8. SecurityConfig.kt 수정
        └── /api/v1/study/** authenticated() 추가

Step 9. 시딩 데이터 작성
        └── src/main/resources/db/math_seed.sql
            12개 스텝 JSON INSERT (ON CONFLICT DO NOTHING)

Step 10. 빌드 & 테스트
         └── ./gradlew bootRun
             curl http://localhost:8080/api/v1/study/math/roadmap (JWT 포함)
```

---

## 11. 설계 결정 사항

### 정답 서버 검증 (채택)
클라이언트에 `answer` 필드를 내려주지 않고 서버에서 검증. 정답지 노출 방지 및 학습 데이터 신뢰도 확보.

### 콘텐츠 JSONB 저장 (채택)
JSON 파일을 resources에 두는 것보다 JSONB로 저장하면 재배포 없이 콘텐츠 수정 가능. `answer` 필드 제거는 응답 DTO 변환 시점에 수행 (`JsonNode` 순회 또는 별도 필터링).

### 사이클 단위 저장 (채택)
LRN_MATH_30 명세 준수. 사이클 내 문제별 진도는 저장하지 않음. 클라이언트가 사이클 완료 시점에 한 번만 서버에 알림.

### 학년별 독립 스텝 번호 (채택)
`UNIQUE(grade, step_number)`. 글로벌 step_id 대신 학년 내 순서 관리. 추후 학년별 독립 커리큘럼 추가 용이.

---

## 12. 향후 고려 사항 (MVP 이후)

| 항목 | 현재 MVP 한계 | 확장 방안 |
|------|-------------|----------|
| 과목 확장 | 수학만 존재 | `domain/study/` 하위에 `language/` 등 추가, `math_step` → `study_step`으로 일반화 |
| 보상 연동 | 스텝 완료 시 풀 획득 연출만 클라이언트 처리 | `StepCompletedEvent` 발행 → 보상 도메인에서 처리 |
| 학습 통계 | 저장 안 함 | 사이클 완료 시간, 오답 횟수 등 별도 로그 테이블 추가 |
| 콘텐츠 관리 | SQL 직접 수정 | 관리자 API 또는 CMS 연동 |
| 정답 오류 횟수 제한 | 무제한 재시도 | 시도 횟수 저장 후 일정 횟수 초과 시 자동 정답 처리 옵션 |
