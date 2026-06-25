# Member 도메인

회원 관리 및 Firebase Google 로그인 → JWT 발급을 담당하는 도메인.

> Spring Security OAuth2 웹 플로우(`/oauth2/**`, `login/success`)는 사용하지 않습니다.  
> Flutter 앱이 Firebase ID Token을 `POST /auth/google`로 전달하는 방식만 지원합니다.

## 패키지 구조

```
domain/member/
├── controller/
│   ├── OAuthController.kt           # POST /auth/google, /auth/logout
│   ├── MemberController.kt            # 회원 조회·탈퇴 (JWT)
│   └── InternalMemberController.kt    # Leo 내부 API
├── service/
│   ├── MemberService.kt               # 회원 생성/조회
│   └── WithdrawalService.kt           # 회원 탈퇴
├── model/
│   ├── Member.kt
│   └── Role.kt
├── repository/
│   └── MemberRepository.kt
├── dto/
│   └── MemberDtos.kt
└── event/
    └── MemberRegisteredEvent.kt

infrastructure/security/
└── FirebaseTokenVerifier.kt           # Firebase ID Token 검증
```

## API

### `POST /api/v1/members/auth/google`

Flutter 앱에서 Google Sign-In(Firebase)으로 받은 **Firebase ID Token**을 전달하면, 서버에서 토큰 검증 후 회원가입 또는 로그인을 처리하고 JWT를 발급한다.

**인증**: 불필요

**Request Body**:

```json
{
  "idToken": "firebase_id_token_string"
}
```

**Response (200)**:

```json
{
  "accessToken": "jwt_access_token",
  "refreshToken": "jwt_refresh_token",
  "email": "user@gmail.com",
  "nickname": null,
  "profileImageUrl": "https://...",
  "onboardingCompleted": false
}
```

- `onboardingCompleted`가 `false`이면 클라이언트는 온보딩 플로우로 이동
- `onboardingCompleted`가 `true`이면 홈 화면으로 이동

### `POST /api/v1/members/auth/logout`

JWT는 Stateless이므로 서버에서 토큰을 무효화하지 않는다. 클라이언트가 `accessToken`·`refreshToken`을 폐기한다.

**인증**: 불필요

**Response**: `204 No Content`

## 주요 로직

### 회원 생성/로그인 (`MemberService.findOrCreateMember`)

1. `providerId`(Firebase UID)로 기존 회원 조회
2. **기존 회원** → `lastLoginAt` 갱신 후 반환
3. **신규 회원** → `Member` 엔티티 생성 및 DB 저장

### Firebase ID Token 검증 (`FirebaseTokenVerifier`)

- Firebase Admin SDK(`FirebaseAuth.verifyIdToken`)로 ID Token 서명·만료 검증
- 검증 실패 시 `FirebaseAuthException` → `OAuthController`가 `401` 반환

## 엔티티

### Member

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK, 자동 생성 |
| `email` | String | Google 이메일 (unique) |
| `profileImageUrl` | String? | Google 프로필 이미지 |
| `providerId` | String | Firebase UID (unique) |
| `provider` | String | `"google"` |
| `role` | Role | `ROLE_USER` (기본값) |
| `nickname` | String? | 온보딩에서 입력한 아동 이름 |
| `grade` | Int? | 학년 (3~6) |
| `guardianPhone` | String? | 보호자 전화번호 |
| `notificationEnabled` | Boolean | 알림 권한 허용 여부 |
| `microphoneEnabled` | Boolean | 마이크 권한 허용 여부 |
| `onboardingCompleted` | Boolean | 온보딩 완료 여부 |
| `createdAt` | LocalDateTime | 가입 시간 |
| `lastLoginAt` | LocalDateTime | 마지막 로그인 시간 |

### Role

| 값 | 설명 |
|----|------|
| `ROLE_USER` | 일반 사용자 |
| `ROLE_ADMIN` | 관리자 |
