# LearnedExpressions 리팩터 (#4, #9)

> 친구랑 시나리오 중복 방지를 위해 `mindSessions` **전체 스캔**하던 구조를 user 문서 **요약 배열**로 변경.

| | |
|---|---|
| **이슈** | [#4](https://github.com/quad-S-BIFriends/bifriends-be/issues/4), [#9](https://github.com/quad-S-BIFriends/bifriends-be/issues/9) |
| **구분** | 확장성 |

[[트러블슈팅-및-이슈]]

---

## STAR

### Situation

- 시나리오 생성마다 `getLearnedExpressions()`가 `mindSessions` **서브컬렉션 전체** 읽기
- 세션 수 증가 시 Firestore 읽기 비용·지연 **선형 증가**

### Task

- 중복 방지에 필요한 “이미 배운 표현” 목록을 **O(1) read**에 가깝게 조회
- 기존 세션 데이터와 호환 유지

### Action

1. `users/{memberId}` 문서에 `learnedExpressions` 배열 필드 추가
2. 세션 완료 저장 시 `arrayUnion`으로 요약 갱신
3. 레거시 데이터 **backfill** (기존 세션에서 한 번 채움)
4. 조회 경로를 서브컬렉션 스캔 → user 문서 1 read로 변경

### Result

- 시나리오 1회당 Firestore read: **N문서 → 1문서**
- 주간 리포트 학습 요약 등 동일 헬퍼 사용 경로도 개선

---

## Before → After

| Before | After |
|--------|-------|
| `mindSessions` 전체 collection read | `users/{id}.learnedExpressions` 1 read |
| 실패 시 `emptyList()` → 중복 방지 무력화 | 요약 필드 기반 안정 조회 |

---

## 관련 파일

- `FirestoreService.kt` — `getLearnedExpressions`, backfill
- `MindScenarioService.kt`, `MindSessionService.kt`
