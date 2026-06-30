# DB 선택: PostgreSQL 17

[[기술적-의사결정]] · [[DB-이원화-Firestore]]

---

## 선택지 비교

| 항목 | MySQL 8 | MongoDB | PostgreSQL 17 |
|------|---------|---------|--------------|
| ACID 트랜잭션 | 지원 (InnoDB) | 멀티 도큐먼트 트랜잭션 4.0+ (제한적) | 완전 지원 |
| 복합 UNIQUE 제약 | 지원 | 지원 (샤드 환경 제한) | 지원 |
| JSON 저장·쿼리 | JSON 타입 (인덱스 제한적) | 네이티브 | JSONB (GIN 인덱스, 쿼리 완전 지원) |
| 내림차순 인덱스 | 미지원 (8.0.19 이전 에뮬레이션) | 지원 | DDL 수준 직접 지정 |
| 집계 쿼리 | 지원 | Aggregation Pipeline (별도 학습) | SQL 표준, CTE, Window Function |
| 오픈소스 라이선스 | GPL | SSPL | PostgreSQL License (제한 없음) |

---

## 채택한 핵심 이유 3가지

### ① 풀(포인트) 시스템의 원자성

`UserStats.earnPool()`과 `RewardHistory` 삽입은 **반드시 하나의 트랜잭션**에 묶여야 합니다.
AI 채팅 정답 완료 시 풀을 지급하고 이력을 남기는 과정이 중간에 실패하면 포인트 불일치가 생깁니다.

MongoDB의 멀티 도큐먼트 트랜잭션은 레플리카셋 환경에서만 지원되고 오버헤드가 크기 때문에,
이 요건을 가장 자연스럽게 충족하는 PostgreSQL을 선택했습니다.

### ② 복합 UNIQUE 제약이 비즈니스 규칙 자체

```sql
CONSTRAINT uk_weekly_report_member_week  UNIQUE (member_id, week_start)
CONSTRAINT uq_user_math_progress         UNIQUE (member_id, math_step_id)
CONSTRAINT uq_math_step_grade_number     UNIQUE (grade, step_number)
```

"한 아동은 같은 주에 리포트 하나", "같은 단계 진행 중복 불가" 같은 규칙을 **DB가 보장**합니다.
애플리케이션만 믿으면 레이스 컨디션에서 중복이 생길 수 있습니다.
PostgreSQL의 DDL 레벨 제약이 이를 막아줍니다.

### ③ 관계형 집계와 JSON 혼용

학습 콘텐츠(`math_step.content_json`)와 AI 리포트(`weekly_report.sections_json`)는
구조가 변동적이라 JSON으로 저장합니다.
반면 학습 시도 집계, 주간 통계, 연속 출석은 관계형 쿼리가 필요합니다.
PostgreSQL은 이 둘을 **한 DB에서 처리**할 수 있습니다.

MySQL도 JSON 타입을 지원하지만, 내부 바이너리 파싱(JSONB)이 없어 쿼리 성능이 떨어집니다.

---

## 인덱싱 전략

```sql
-- 날짜 범위 조회 + 최신순 정렬을 한 인덱스로
CREATE INDEX idx_weekly_report_member ON weekly_report (member_id, week_start DESC);

-- AI 주간 배치: member_id + 과목 + 기간 필터
CREATE INDEX idx_learning_attempt_member_subject
    ON learning_attempt (member_id, subject, solved_at);

-- ChatMessage 비정규화: session JOIN 없이 기간 스캔
CREATE INDEX idx_chat_messages_member_created ON chat_messages (member_id, created_at);
```

### ChatMessage.memberId 비정규화

`chat_messages`에 `member_id`를 중복 저장한 것은 의도적인 결정입니다.

AI가 주간 리포트 생성을 위해 특정 기간의 채팅 이력을 조회할 때
`chat_sessions JOIN chat_messages` 없이 단일 테이블 범위 스캔이 가능합니다.
정규화 위반이지만, 읽기 성능과 쿼리 단순성을 위해 트레이드오프를 의식적으로 선택했습니다.

---

## Flyway 스키마 버전 관리

```
V1__init_schema.sql   → 전체 19개 테이블 초기 스키마
V2__seed_shop.sql     → 상점 아이템 시드 데이터
V3__seed_learning.sql → 학년별 학습 콘텐츠 시드
```

마이그레이션을 코드로 관리함으로써 팀원 간 스키마 동기화 문제를 없앴고,
배포 시 자동 적용되어 운영 환경 수동 DDL 실행 위험을 제거했습니다.

---

## 트레이드오프 · 향후 고려

| 항목 | 현재 | 향후 |
|------|------|------|
| `sections_json` 타입 | `TEXT` | JSONB로 전환 시 JSON 필드 쿼리 가능 |
| 단일 DB | PostgreSQL 단독 | 읽기 부하 증가 시 Read Replica 고려 |
| Firestore 혼용 | 친구랑 세션만 | [[DB-이원화-Firestore]] 참고 |
