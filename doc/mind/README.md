# Mind 도메인 — 친구랑 감정 학습

## 구현 범위

| 엔드포인트 | 메서드 | 기능명세 | 설명 |
|---|---|---|---|
| `/api/v1/mind/scenario` | POST | EMO-02, EMO-04 | 감정 학습 시나리오 생성 + step3 이미지 업로드 |
| `/api/v1/mind/sessions` | POST | EMO-23 | 학습 완료 세션 저장 + 보상 지급 |
| `/api/v1/mind/sessions` | GET | EMO-03 | 학습 히스토리 목록 조회 |
| `/api/v1/mind/sessions/{sessionId}` | GET | EMO-27 | 특정 세션 상세 조회(다시보기) |

## 핵심 설계 결정

### 시나리오 생성과 세션 저장 분리
- **기존 `/api/v1/emotion/scenarios`**: 시나리오 생성 → Firestore 저장 → 보상 지급 한 번에 처리
- **신규 `/api/v1/mind/`**: 생성과 저장을 분리
  - `POST /scenario`: AI 호출 + 이미지 업로드만 담당
  - `POST /sessions`: 아이가 step4까지 완료했을 때 FE가 호출 → Firestore 저장 + 보상 지급

→ **완료된 세션만** `users/{userId}/mindSessions`에 쌓인다.

### 중복 표현 방지 로직
`getLearnedExpressions()`는 Firestore에 저장된 세션의 `learnedExpression` 필드를 읽는다.
완료 세션만 Firestore에 저장되므로, 아이가 중간에 그만둔 표현은 "학습한 표현"으로 카운트되지 않는다.

## DB 스키마 (Firestore)

```
users/{memberId}/mindSessions/{setId}
├── setId             String   AI가 생성한 고유 ID
├── emotion           String   감정명 (기쁨 / 속상함 / 부끄러움 / 화남 / 실망 / 고마움)
├── situation         String   상황 설명
├── learnedExpression String   배운 표현
├── isFallback        Boolean  폴백 시나리오 여부
├── completedAt       String   완료 시각 (ISO-8601 UTC)
├── step1             Map      오늘의 표현 배우기
├── step2             Map      어떤 기분일까요?
├── step3             Map      3컷 만화 (imageUrl 저장, image_b64 미저장)
└── step4             Map      이렇게 말하고 싶어!
```

API 명세 → [api-spec.md §11](../api-spec.md#11-친구랑--감정-학습-mind)

## 운영·트러블슈팅

### 필수 설정

| 항목 | 설명 |
|------|------|
| `firebase-service-account.json` | Firebase Admin SDK 서비스 계정 (Git 커밋 금지) |
| 기본 경로 | `src/main/resources/firebase-service-account.json` |
| 환경변수 | `FIREBASE_CONFIG_PATH` (파일 경로 또는 `classpath:...`) |
| Storage 버킷 | `FIREBASE_STORAGE_BUCKET` (기본 `bifriends-5df72.firebasestorage.app`) |
| Firestore DB ID | `FIRESTORE_DATABASE_ID` (기본 `(default)`. 콘솔에서 이름 지정 DB를 만들었다면 그 ID, 예: `bifriends`) |

### Docker 로컬 실행

`docker-compose.yml`이 서비스 계정을 컨테이너에 마운트합니다.

```text
./src/main/resources/firebase-service-account.json
  → /app/config/firebase-service-account.json
FIREBASE_CONFIG_PATH=/app/config/firebase-service-account.json
```

파일이 없으면 앱 기동 시 Firebase 초기화가 실패하거나, 기동 후 connectivity check warn이 남습니다.

### 히스토리가 비어 있을 때

| 확인 | 기대 |
|------|------|
| step4 완료 후 `POST /api/v1/mind/sessions` 호출 | 200 + 로그 `[Firestore] mindSession 저장 완료` |
| Firebase Console → `users/{memberId}/mindSessions` | 문서 존재 |
| `GET /api/v1/mind/sessions` | `totalCount >= 1` |

`POST /scenario`만 호출하고 `POST /sessions`를 생략하면 히스토리는 **항상 0건**입니다 (정상 동작).

### API 오류 구분

| HTTP | 의미 |
|------|------|
| `200` + `sessions: []` | 저장된 완료 세션 없음 |
| `503` | Firestore 연동/조회·저장 실패 (인증, 네트워크, 권한 등) |
| `404` | `GET /sessions/{sessionId}` — 해당 세션 문서 없음 |

조회 실패 시 빈 배열을 반환하지 않습니다. FE는 `503`을 스토리지 오류로 처리하세요.

### Firestore `completedAt` 인덱스

목록 API는 `orderBy(completedAt DESC)`를 사용합니다. 인덱스가 없으면 서버가 **메모리 정렬 fallback**으로 동작합니다(warn 로그).

콘솔에서 인덱스 생성을 권장합니다: 컬렉션 `mindSessions`, 필드 `completedAt` 내림차순.

### 기동 시 연결 점검

`FirestoreStartupHealthChecker`가 기동 직후 1회 Firestore read를 시도합니다.

- 성공: `[Firestore] connectivity check OK`
- 실패: `[Firestore] connectivity check failed` (앱 기동은 계속)
