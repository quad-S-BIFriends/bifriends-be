# domain/study — 공부방 수학·국어 구현 요약

## 구현 범위

| 항목 | 내용 |
|------|------|
| 신규 도메인 | `domain/study/` |
| 신규 Entity | `MathStep`, `UserMathProgress`, `KoreanStep`, `UserKoreanProgress` |
| 신규 API | 수학 6개 + 국어 6개 (`/api/v1/learning/**`) |
| 기능명세 | LRN_MATH_01~33, LRN_KOR_00~48 |
| MVP 범위 | 초3~초6, 학년당 3스텝, 스텝당 5사이클 (수학 12스텝 + 국어 12스텝) |

## API 목록

### 수학
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/learning/math/roadmap` | 로드맵 조회 |
| GET | `/api/v1/learning/math/steps/{stepId}/content` | 콘텐츠 조회 (answer 제거) |
| GET | `/api/v1/learning/math/progress` | 진도 조회 |
| POST | `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 답안 검증 |
| POST | `/api/v1/learning/math/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 |
| POST | `/api/v1/learning/math/steps/{stepId}/complete` | 스텝 완료 |

### 국어
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/learning/korean/roadmap` | 로드맵 조회 |
| GET | `/api/v1/learning/korean/steps/{stepId}/content` | 콘텐츠 조회 (passage 포함, answer 제거) |
| GET | `/api/v1/learning/korean/progress` | 진도 조회 |
| POST | `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 답안 검증 (Cycle 2~5) |
| POST | `/api/v1/learning/korean/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 |
| POST | `/api/v1/learning/korean/steps/{stepId}/complete` | 스텝 완료 |

## 핵심 설계 결정

- **정답 서버 검증**: `answer`/`explanation` 필드는 DB에만 존재, 콘텐츠 API 응답에서 제거
- **사이클 JSON 키**: 수학·국어 시드 모두 `cycle_number`, `cycle_type` (API 응답도 동일; 국어 legacy `cycle`/`type`은 sanitizer가 정규화)
- **콘텐츠 저장**: `content_json TEXT` + `JsonNodeConverter` (hypersistence 의존성 없이)
- **사이클 완료 추적**: `@ElementCollection` → `user_math_progress_cycles` 테이블, `MutableSet<Int>` 으로 중복 방지
- **중도 종료**: 사이클 단위로만 완료 저장, 사이클 내 문항 상태는 클라이언트 관리 (LRN_MATH_30)

## DB 스키마

```sql
CREATE TABLE math_step (
    id           BIGSERIAL    PRIMARY KEY,
    grade        INTEGER      NOT NULL,
    step_number  INTEGER      NOT NULL,
    step_title   VARCHAR(100) NOT NULL,
    concept      VARCHAR(200) NOT NULL,
    content_json TEXT         NOT NULL,
    CONSTRAINT uq_math_step_grade_number UNIQUE (grade, step_number)
);

CREATE TABLE user_math_progress (
    id                BIGSERIAL PRIMARY KEY,
    member_id         BIGINT    NOT NULL REFERENCES members(id),
    math_step_id      BIGINT    NOT NULL REFERENCES math_step(id),
    is_step_completed BOOLEAN   NOT NULL DEFAULT FALSE,
    last_accessed_at  TIMESTAMP,
    CONSTRAINT uq_user_math_progress UNIQUE (member_id, math_step_id)
);

CREATE TABLE user_math_progress_cycles (
    progress_id  BIGINT  NOT NULL REFERENCES user_math_progress(id),
    cycle_number INTEGER NOT NULL
);
```

## 패키지 구조

```
domain/study/
├── controller/
│   ├── StudyMathController.kt
│   └── StudyKoreanController.kt
├── service/
│   ├── StudyMathService.kt
│   └── StudyKoreanService.kt
├── dto/
│   ├── StudyMathDtos.kt
│   └── StudyKoreanDtos.kt
├── model/
│   ├── MathStep.kt
│   ├── UserMathProgress.kt
│   ├── KoreanStep.kt
│   ├── UserKoreanProgress.kt
│   └── StepStatus.kt          ← 수학·국어 공용
└── repository/
    ├── MathStepRepository.kt
    ├── UserMathProgressRepository.kt
    ├── KoreanStepRepository.kt
    └── UserKoreanProgressRepository.kt

infrastructure/converter/JsonNodeConverter.kt
resources/db/math_seed.sql     ← 수학 12스텝
resources/db/korean_seed.sql   ← 국어 12스텝
```
