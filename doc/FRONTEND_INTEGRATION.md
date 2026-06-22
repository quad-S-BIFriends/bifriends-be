# 프론트엔드 연동 가이드 (Flutter)

백엔드 API 서버와 연동하기 위해 프론트엔드 팀에게 전달하는 문서입니다.

---

## 기본 정보

| 항목 | 값 |
|---|---|
| API Base URL | `https://api.bifriends.study/api/v1` |
| Swagger UI | `https://api.bifriends.study/swagger-ui/index.html` |
| 헬스체크 | `https://api.bifriends.study/actuator/health` |

> Swagger에서 모든 엔드포인트 스펙, 요청/응답 형식을 직접 확인할 수 있습니다.

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

서버는 Stateless이므로 실제 폐기는 Flutter에서 토큰을 삭제하면 됩니다.

---

## 에러 응답

| HTTP 상태 | 의미 |
|---|---|
| `401` | 인증 실패 (토큰 없음 / 만료) |
| `403` | 권한 없음 |
| `400` | 잘못된 요청 |

인증 실패 응답:
```json
{ "error": "Unauthorized" }
```

---

## 주요 API 목록

> 상세 요청/응답 스펙은 Swagger(`https://api.bifriends.study/swagger-ui/index.html`) 참고

### 회원

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/members/auth/google` | 구글 로그인 / 회원가입 | 불필요 |
| POST | `/members/auth/logout` | 로그아웃 | 불필요 |
| GET | `/members/me` | 내 프로필 조회 | 필요 |
| PATCH | `/members/me` | 내 정보 수정 | 필요 |
| PATCH | `/members/profile` | 프로필 이미지 수정 | 필요 |
| PATCH | `/members/me/settings` | 앱 설정 변경 | 필요 |
| PATCH | `/members/me/representative-item` | 대표 아이템 변경 | 필요 |
| DELETE | `/members/me` | 회원 탈퇴 | 필요 |

### 온보딩

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/onboarding/terms` | 약관 동의 | 필요 |
| PUT | `/onboarding/interests` | 관심사 설정 | 필요 |
| POST | `/onboarding/parent-password` | 부모 비밀번호 설정 | 필요 |
| POST | `/onboarding/complete` | 온보딩 완료 | 필요 |

### 홈 / 투두

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/home` | 홈 화면 데이터 | 필요 |
| GET | `/todos` | 투두 목록 | 필요 |
| POST | `/todos` | 투두 생성 | 필요 |
| PATCH | `/todos/{todoId}` | 투두 수정 | 필요 |
| PATCH | `/todos/{todoId}/complete` | 투두 완료 | 필요 |
| DELETE | `/todos/{todoId}` | 투두 삭제 | 필요 |

### 학습 - 수학

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/learning/math/roadmap` | 학습 로드맵 | 필요 |
| GET | `/learning/math/steps` | 단계 목록 | 필요 |
| GET | `/learning/math/steps/{stepId}/content` | 단계 콘텐츠 | 필요 |
| POST | `/learning/math/steps/{stepId}/complete` | 단계 완료 | 필요 |
| POST | `/learning/math/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 | 필요 |
| POST | `/learning/math/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 문제 정답 검증 | 필요 |

### 학습 - 국어

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/learning/korean/roadmap` | 학습 로드맵 | 필요 |
| GET | `/learning/korean/steps` | 단계 목록 | 필요 |
| GET | `/learning/korean/steps/{stepId}/content` | 단계 콘텐츠 | 필요 |
| POST | `/learning/korean/steps/{stepId}/complete` | 단계 완료 | 필요 |
| POST | `/learning/korean/steps/{stepId}/cycles/{cycleNumber}/complete` | 사이클 완료 | 필요 |
| POST | `/learning/korean/steps/{stepId}/cycles/{cycleNumber}/questions/{questionIndex}/validate` | 문제 정답 검증 | 필요 |

### AI 채팅

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/chat/sessions` | 채팅 세션 목록 | 필요 |
| POST | `/chat/sessions` | 채팅 세션 생성 | 필요 |
| GET | `/chat/sessions/{sessionId}` | 세션 상세 | 필요 |
| PATCH | `/chat/sessions/{sessionId}` | 세션 수정 | 필요 |
| DELETE | `/chat/sessions/{sessionId}` | 세션 삭제 | 필요 |
| GET | `/chat/sessions/{sessionId}/messages` | 메시지 목록 | 필요 |
| POST | `/chat/messages` | 메시지 전송 (AI 응답 포함) | 필요 |

### 감정 시나리오

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| POST | `/emotion/scenario` | 감정 시나리오 생성 | 필요 |
| POST | `/emotion/scenarios` | 시나리오 목록 요청 | 필요 |

### 마음 (Mind)

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/mind/concepts` | 개념 목록 | 필요 |
| GET | `/mind/concepts/lesson-status` | 레슨 상태 | 필요 |
| GET | `/mind/learning-summary` | 학습 요약 | 필요 |
| POST | `/mind/generate` | 콘텐츠 생성 | 필요 |

### 상점

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/shop/items` | 상점 아이템 목록 | 필요 |
| POST | `/shop/items/{itemCode}/purchase` | 아이템 구매 | 필요 |
| PATCH | `/shop/items/{itemCode}/equip` | 아이템 장착 | 필요 |
| DELETE | `/shop/items/equip` | 아이템 장착 해제 | 필요 |
| GET | `/shop/my-items` | 보유 아이템 목록 | 필요 |
| POST | `/shop/gift` | 선물 보내기 | 필요 |

### 리포트

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/reports/{reportId}` | 리포트 상세 | 필요 |
| GET | `/reports/progress` | 진행 리포트 | 필요 |
| POST | `/reports/{reportId}/parent-mission` | 부모 미션 등록 | 필요 |
| POST | `/reports/weekly-report` | 주간 리포트 생성 | 필요 |

### 부모

| Method | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/parent/members/{memberId}/profile` | 자녀 프로필 | 필요 |
| GET | `/parent/members/{memberId}/learning-progress` | 자녀 학습 현황 | 필요 |
| PATCH | `/parent/permissions` | 부모 권한 설정 | 필요 |
| PATCH | `/parent/password` | 부모 비밀번호 변경 | 필요 |
| POST | `/parent/verify` | 부모 비밀번호 검증 | 필요 |
| POST | `/parent/reset-password` | 비밀번호 초기화 | 필요 |
