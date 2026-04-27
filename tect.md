
# BiFriends 백엔드 전체 API 명세서

본 문서는 프론트엔드 개발자가 실제 서비스 연동에 필요한 모든 주요 엔드포인트, 요청/응답, 데이터 구조를 상세히 안내합니다.

---

## 1. 인증/회원

### 1.1 구글 로그인 (Firebase 기반)
- **POST** `/api/v1/members/auth/google`
- **설명:** 클라이언트에서 Google 로그인 후 받은 idToken을 전달하면, 회원가입/로그인 및 JWT 발급
- **요청**
  ```json
  { "idToken": "firebase에서 발급받은 idToken" }
  ```
- **응답**
  ```json
  {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "JWT_REFRESH_TOKEN",
    "email": "user@email.com",
    "name": "홍길동",
    "profileImageUrl": "https://...",
    "onboardingCompleted": false
  }
  ```
- **비고:** 이후 모든 인증이 필요한 API 호출 시 Authorization 헤더에 Bearer로 전달

---

## 2. 온보딩

### 2.1 보호자 전화번호 등록
- **POST** `/api/v1/onboarding/guardian`
- **요청**
  ```json
  { "phoneNumber": "01012345678" }
  ```
- **응답**
  ```json
  { "verified": true }
  ```

### 2.2 프로필(닉네임/학년) 등록/수정
- **PATCH** `/api/v1/onboarding/profile`
- **요청**
  ```json
  { "nickname": "레오", "grade": 4 }
  ```
- **응답**
  ```json
  { "nickname": "레오", "grade": 4 }
  ```

### 2.3 관심사 등록
- **PUT** `/api/v1/onboarding/interests`
- **요청**
  ```json
  { "interests": ["DINOSAUR", "GAME", "SCIENCE"] }
  ```
- **응답**
  ```json
  { "interests": ["DINOSAUR", "GAME", "SCIENCE"] }
  ```
- **관심사 종류:** DINOSAUR, ANIMAL, SPACE, SPORTS, KPOP_MUSIC, GAME, COOKING, CRAFTING, SCIENCE (최대 3개)

### 2.4 아이템(보물) 선택
- **POST** `/api/v1/onboarding/gift`
- **요청**
  ```json
  { "itemType": "GIFT_1" }
  ```
- **응답**
  ```json
  { "itemType": "GIFT_1", "acquiredAt": "2026-04-27T12:34:56" }
  ```
- **아이템 종류:** GIFT_1, GIFT_2, GIFT_3, GIFT_4

### 2.5 권한(알림/마이크) 설정
- **PATCH** `/api/v1/onboarding/permissions`
- **요청**
  ```json
  { "notificationEnabled": true, "microphoneEnabled": false }
  ```
- **응답**
  ```json
  { "notificationEnabled": true, "microphoneEnabled": false }
  ```

### 2.6 온보딩 완료 처리
- **POST** `/api/v1/onboarding/complete`
- **응답**
  ```json
  { "completed": true }
  ```

---

## 3. 멤버 정보

### 3.1 내 정보 조회
- **GET** `/api/v1/members/me`
- **응답**
  ```json
  {
    "email": "user@email.com",
    "name": "홍길동",
    "profileImageUrl": "https://...",
    "nickname": "레오",
    "grade": 4,
    "guardianPhone": "01012345678",
    "notificationEnabled": true,
    "microphoneEnabled": false,
    "onboardingCompleted": true
  }
  ```

---

## 4. 헬스 체크

### 4.1 서버 상태 확인
- **GET** `/api/health`
- **설명:** 서버 상태 확인용 (200 OK 반환)

---

## 5. 공통 사항
- 모든 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더 필요
- 응답은 기본적으로 JSON
- 에러 발생 시 HTTP 상태코드와 함께 `{ "error": "메시지" }` 형태로 반환

---

## 6. 기타
- 추가 엔드포인트, 상세 요청/응답 스펙은 백엔드 개발자에게 문의
