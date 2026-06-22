# 프론트엔드 연동 가이드 (Flutter)

백엔드 API 서버와 연동하기 위해 프론트엔드 팀에게 전달하는 문서입니다.

---

## 기본 정보

| 항목 | 값 |
|---|---|
| API Base URL | `https://api.bifriends.study/api/v1` |
| Swagger UI | `https://api.bifriends.study/swagger-ui/index.html` |
| 헬스체크 | `https://api.bifriends.study/actuator/health` |

> Swagger에서 모든 엔드포인트의 요청/응답 스펙을 직접 확인할 수 있습니다.

---

## Firebase 클라이언트 설정

Google Sign-In을 위해 Firebase 프로젝트 연동이 필요합니다.

- Firebase 프로젝트: `bifriends-5df72`
- Android: `google-services.json` 추가 (Firebase 콘솔에서 다운로드)
- iOS: `GoogleService-Info.plist` 추가 (Firebase 콘솔에서 다운로드)
- Flutter 패키지: `firebase_auth`, `google_sign_in`

---

## 인증 방식

### 전체 흐름

```
1. Flutter: Firebase Google Sign-In 수행
2. Flutter: Firebase ID Token 획득 (user.getIdToken())
3. Flutter → 서버: POST /api/v1/members/auth/google
                    { "idToken": "<Firebase ID Token>" }
4. 서버 → Flutter: { accessToken, refreshToken, onboardingCompleted, ... }
5. 이후 모든 요청: Authorization 헤더에 accessToken 포함
```

### 로그인 API

**POST** `/api/v1/members/auth/google`

요청:
```json
{
  "idToken": "Firebase Google Sign-In에서 받은 ID Token"
}
```

응답:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "email": "user@example.com",
  "nickname": null,
  "profileImageUrl": "https://...",
  "onboardingCompleted": false
}
```

> `onboardingCompleted: false`이면 온보딩 화면으로 이동해야 합니다.

### JWT 토큰 사용

인증이 필요한 모든 요청에 헤더 추가:
```
Authorization: Bearer {accessToken}
```

| 토큰 | 만료 시간 |
|---|---|
| accessToken | 1시간 |
| refreshToken | 7일 |

### 로그아웃

**POST** `/api/v1/members/auth/logout`

서버는 Stateless이므로 Flutter에서 저장된 토큰을 삭제하면 됩니다.

---

## 에러 응답

| HTTP 상태 | 의미 |
|---|---|
| `401` | 인증 실패 (토큰 없음 / 만료) |
| `403` | 권한 없음 (부모 PIN 불일치 등) |
| `400` | 잘못된 요청 |

인증 실패 응답:
```json
{ "error": "Unauthorized" }
```

---

## 주요 API 목록

> 하단의 API 중 **"AI 내부"** 표시 항목은 서버 내부에서 AI(Leo)가 호출하는 전용 엔드포인트입니다.
> Flutter 클라이언트는 호출하지 않습니다.

---

### 회원

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/members/auth/google` | 구글 로그인 / 회원가입 | 불필요 |
| POST | `/members/auth/logout` | 로그아웃 | 불필요 |
| GET | `/members/me` | 내 프로필 조회 | 필요 |
| PATCH | `/members/me/settings` | 이름·학년·관심사 한 번에 저장 | 필요 |
| PATCH | `/members/me/representative-item` | 대표 아이템 변경 | 필요 |
| DELETE | `/members/me` | 회원 탈퇴 (부모 PIN 필요) | 필요 |

---

### 온보딩

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/onboarding/terms` | 약관 동의 (ONB-02) | 필요 |
| POST | `/onboarding/parent-password` | 부모 PIN 설정 (ONB-02-01) | 필요 |
| PATCH | `/onboarding/profile` | 이름·학년 입력 (ONB-04/06) | 필요 |
| PUT | `/onboarding/interests` | 관심사 선택 (ONB-07) | 필요 |
| POST | `/onboarding/gift` | 선물 아이템 선택 (ONB-08) | 필요 |
| PATCH | `/onboarding/permissions` | 알림·마이크 권한 설정 (ONB-10) | 필요 |
| POST | `/onboarding/complete` | 온보딩 완료 (ONB-11) | 필요 |

---

### 홈

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/home` | 홈 화면 전체 데이터 (인사 메시지, 레벨, 풀, streak, 할 일 목록) | 필요 |

> 앱 오픈 및 홈 탭 진입 시 호출. 출석 체크(streak 갱신·보상 지급)가 멱등 처리로 포함됩니다.

---

### 할 일 (Todo)

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| PATCH | `/todos/{todoId}/complete` | 할 일 완료 처리 (+1풀, 전체 완료 시 +3풀 보너스) | 필요 |

> 나머지 투두 CRUD(생성·수정·삭제)는 AI(Leo) 내부 전용 API입니다. Flutter에서 호출하지 않습니다.

---

### 학습 — 수학

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/learning/math/roadmap` | 수학 학습 로드맵 | 필요 |
| GET | `/learning/math/progress` | 수학 학습 진도 조회 | 필요 |
| GET | `/learning/math/steps/{stepId}/content` | 스텝 콘텐츠 조회 | 필요 |
| POST | `/learning/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 문제 답안 검증 | 필요 |
| POST | `/learning/math/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 처리 | 필요 |
| POST | `/learning/math/steps/{stepId}/complete` | 스텝 완료 처리 | 필요 |

---

### 학습 — 국어

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/learning/korean/roadmap` | 국어 학습 로드맵 | 필요 |
| GET | `/learning/korean/progress` | 국어 학습 진도 조회 | 필요 |
| GET | `/learning/korean/steps/{stepId}/content` | 스텝 콘텐츠 조회 (지문 포함, 정답 제외) | 필요 |
| POST | `/learning/korean/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 문제 답안 검증 (Cycle 2~5만 해당) | 필요 |
| POST | `/learning/korean/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 처리 | 필요 |
| POST | `/learning/korean/steps/{stepId}/complete` | 스텝 완료 처리 | 필요 |

---

### AI 채팅

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/chat/messages` | 메시지 전송 (AI 응답 포함) | 필요 |
| GET | `/chat/sessions` | 채팅 세션 목록 (최근 활동 순) | 필요 |
| GET | `/chat/sessions/{sessionId}` | 세션 상세 (메시지 전체 포함, 오래된 순) | 필요 |
| DELETE | `/chat/sessions/{sessionId}` | 세션 삭제 (메시지 포함 전체) | 필요 |

---

### 감정 학습 (친구랑 탭 — 감정 카드)

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/emotion/scenarios` | 감정 학습 세트 생성 (AI 호출, 10~30초 소요) | 필요 |

> AI 호출 + Firebase Storage 업로드로 응답이 **10~30초** 걸릴 수 있습니다. 로딩 화면을 반드시 표시하세요.

---

### 감정 학습 (친구랑 탭 — 마음 시나리오)

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/mind/scenario` | 마음 시나리오 생성 (AI 호출, 10~30초 소요) | 필요 |
| POST | `/mind/sessions` | 학습 완료 세션 저장 (+3풀 보상) | 필요 |
| GET | `/mind/sessions` | 학습 히스토리 목록 (최신순) | 필요 |
| GET | `/mind/sessions/{sessionId}` | 세션 상세 조회 | 필요 |

> `/mind/scenario`도 AI 호출로 **10~30초** 소요. 로딩 화면 필요.

---

### 상점

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/shop/items` | 상점 아이템 목록 | 필요 |
| GET | `/shop/my-items` | 보유 아이템 목록 | 필요 |
| POST | `/shop/items/{itemCode}/purchase` | 아이템 구매 | 필요 |
| PATCH | `/shop/items/{itemCode}/equip` | 아이템 장착 | 필요 |
| DELETE | `/shop/items/equip` | 아이템 장착 해제 | 필요 |

---

### 리포트 (부모 모드)

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/reports` | 주간 리포트 목록 (최신순) | 필요 |
| GET | `/reports/{reportId}` | 리포트 상세 (성장요약·학습패턴·챗안전신호 통합) | 필요 |
| POST | `/reports/{reportId}/parent-mission` | 보호자 미션 받기 | 필요 |
| POST | `/reports/generate` | 리포트 수동 생성 (특정 주 지정) | 필요 |

> 부모 모드 PIN 확인은 클라이언트에서 처리합니다. PIN 검증 후 리포트 API를 호출하세요.

---

### 부모 모드

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/parent/verify` | 부모 PIN 검증 | 필요 |
| PATCH | `/parent/password` | 부모 PIN 변경 (현재 PIN 확인 필요) | 필요 |
| POST | `/parent/reset-password` | 부모 PIN 초기화 | 필요 |
