# DB 이원화: PostgreSQL + Firestore

[[기술적-의사결정]] · [[PostgreSQL-선택]]

---

## 왜 두 가지 DB를 쓰는가

감정 학습(친구랑) 세션 데이터는 성격이 다릅니다.

- 시나리오 1세트당 **이미지 URL + 감정 표현 + 선택 이력**을 묶어 저장
- AI가 동적으로 생성하는 비정형 JSON
- 세션 간 관계 조회(JOIN)가 필요 없음 — 회원별 독립 컬렉션

이 데이터를 PostgreSQL에 넣으면 가변 필드를 JSONB로 처리하거나
매 시나리오 업데이트마다 스키마 변경이 필요합니다.
Firestore의 도큐먼트 모델이 자연스럽게 맞습니다.

---

## 저장소 분리 기준

| 기준 | PostgreSQL | Firestore |
|------|-----------|-----------|
| 다른 테이블과 JOIN 필요 | ✅ | ❌ |
| 집계·통계 쿼리 | ✅ | ❌ |
| 스키마 고정 | ✅ | — |
| 가변 JSON, 이미지 URL 묶음 | — | ✅ |
| 단건 조회 위주 | — | ✅ |
| ACID 트랜잭션 필요 | ✅ | ❌ |

### 데이터별 저장소

| 데이터 | 저장소 | 이유 |
|--------|--------|------|
| 채팅 메시지, 세션 | PostgreSQL | 기간별 집계, AI 리포트 JOIN |
| 학습 시도, 진행률 | PostgreSQL | 주간 통계, 레벨업 연산 |
| 주간 리포트, 안전 신호 | PostgreSQL | 부모 리포트 조회 |
| 상점, 보상 이력 | PostgreSQL | 포인트 트랜잭션 |
| 친구랑 완료 세션 | Firestore | 비정형 JSON, JOIN 불필요 |
| `learnedExpressions` | Firestore | 세션에서 파생, 빠른 단건 조회 |

---

## Firestore 경로 구조

```
users/{memberId}/mindSessions/{setId}
users/{memberId}.learnedExpressions   ← 배열 필드 (중복 시나리오 방지)
```

---

## 중간 이탈 방지 설계

```
POST /api/v1/mind/scenario   → AI에서 시나리오 생성 (Firestore 저장 X)
POST /api/v1/mind/sessions   → 완료 시에만 Firestore 저장
```

시나리오를 받아 중간에 이탈해도 DB 오염이 없습니다.
완료 시에만 저장하므로 미완 데이터가 쌓이는 문제를 원천 차단했습니다.

---

## Firestore 장애 격리

```kotlin
fun verifyConnectivity() {
    try {
        db.collection("_connectivity").document("probe").get().get()
    } catch (e: Throwable) {
        log.warn("[Firestore] connectivity check failed — ...")
        // 예외를 삼킴: 앱 기동은 계속
    }
}
```

Firestore 연결 실패가 앱 전체 기동을 막아선 안 됩니다.
PostgreSQL 기반 핵심 기능(채팅, 학습, 리포트)은 Firestore와 무관하게 동작해야 합니다.
스타트업 헬스체크에서 경고만 남기고 기동을 계속하도록 했습니다.

---

## 트레이드오프

| 항목 | 내용 |
|------|------|
| 운영 복잡도 | DB가 두 개 → 모니터링 포인트 증가 |
| 트랜잭션 경계 | PostgreSQL ↔ Firestore 간 원자성 보장 불가 |
| 비용 | Firestore 읽기/쓰기 과금 (소규모에서는 무시 가능) |
| 친구랑 세션 집계 | SQL로 불가 — 필요 시 PostgreSQL에 요약 컬럼 추가 고려 |
