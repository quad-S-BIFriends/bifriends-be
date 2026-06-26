# 인증 아키텍처 — FE JWT · AI Internal Token

> **핵심 원칙:** FE와 AI는 **직접 통신하지 않는다.** BE가 유일한 중계자이며, **호출 주체에 따라 인증 방식이 다르다.**

| 통신 구간 | 인증 방식 | 주체 |
|-----------|-----------|------|
| **FE → BE** | `Authorization: Bearer {JWT}` | 사용자(아동·보호자 앱) |
| **BE → AI** | 별도 토큰 없음 (Docker 내부망 HTTP) | BE 서버 |
| **AI → BE** | `X-Internal-Service: {공유 시크릿}` | bifriends-ai 서버 |

[[기술적-의사결정]] · [[API-명세]] · [`doc/presentation/fe_ai_communication.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/test/chat/doc/presentation/fe_ai_communication.md)

---

## 전체 구조

```
Flutter (FE)
    │  Bearer JWT
    ▼
Spring Boot BE  ─── HTTP ───▶  bifriends-ai
    ▲                                │
    └──── X-Internal-Service ────────┘
         (콜백·내부 API)
```

- 로그인·회원 API만 예외적으로 **Firebase ID Token**을 1회 검증한 뒤, 이후 API는 **자체 JWT**만 사용한다.

---

## 1. FE ↔ BE — JWT

### 왜 Firebase만 쓰지 않고 JWT를 따로 발급하나?

| | Firebase ID Token | 자체 JWT |
|---|-------------------|----------|
| 검증 | 매 요청 Firebase 서버 호출 | BE가 **로컬 서명 검증** (`JwtProvider`) |
| 만료 | 약 1시간, 갱신 UX 부담 | access 1h / refresh 7d 팀 정책 |
| 주체 | Google OAuth 사용자 | BE `memberId` 기준 API 인증 |

**결정:** 로그인 시에만 Firebase로 신원 확인 → 이후 API는 **Stateless JWT**로 처리해 지연·외부 의존을 줄인다.

### 로그인 흐름

```
FE                          BE                           Firebase
│ POST /auth/google         │                            │
│ { idToken }               │ verifyIdToken(idToken)     │
│──────────────────────────▶│───────────────────────────▶│
│                           │◀───────────────────────────│
│                           │ findOrCreateMember()       │
│                           │ generateAccessToken()      │
│                           │ generateRefreshToken()     │
│◀──────────────────────────│                            │
│ { accessToken, refreshToken, onboardingCompleted, ... }  │
```

- 엔드포인트: `POST /api/v1/members/auth/google` (`OAuthController`)
- 검증: `FirebaseTokenVerifier` → `FirebaseAuth.verifyIdToken()`
- 발급: `JwtProvider` (JJWT, HMAC-SHA)

### 이후 API 요청

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

1. `JwtAuthenticationFilter`가 `Bearer ` 접두사 제거
2. `jwtProvider.validateToken()` — 서명·만료 확인
3. `subject` = `memberId` → `SecurityContext`에 `ROLE_USER` 설정
4. 컨트롤러·서비스에서 `memberId`로 회원 식별

### JWT payload (요약)

| claim | 내용 |
|-------|------|
| `sub` | `memberId` (Long 문자열) |
| `email` | 회원 이메일 |
| `role` | `ROLE_USER` 등 |
| `exp` | access 1시간 / refresh 7일 |

설정 (`application.yml`):

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000    # 1시간
  refresh-token-expiration: 604800000 # 7일
```

### Spring Security 설정

- `SessionCreationPolicy.STATELESS` — 세션 쿠키 없음
- `permitAll`: `/health`, `/api/v1/members/auth/**`, Swagger
- **그 외 모든 경로** `authenticated()` — JWT 필수
- 로그아웃: Stateless이므로 **클라이언트가 토큰 폐기** (`POST /auth/logout`은 no-content)

### FE가 쓰는 API 예

| 기능 | 메서드 | 인증 |
|------|--------|------|
| 홈 | `GET /api/v1/home` | JWT |
| Leo 채팅 | `POST /api/v1/chat/messages` | JWT |
| 친구랑 | `POST /api/v1/mind/scenario` | JWT |
| 보호자 리포트 | `GET /api/v1/parent/reports` | JWT |

---

## 2. AI ↔ BE — X-Internal-Service

### 왜 JWT가 아닌가?

| 관점 | JWT | Internal Token |
|------|-----|----------------|
| 주체 | **사용자** (`memberId`) | **서버** (bifriends-ai) |
| AI 콜백 | 어떤 회원 대신인지 payload에 매번 실어야 함 | 경로·body에 `memberId` 명시 |
| 구현 | AI에 JWT 발급·갱신 로직 필요 | **공유 시크릿 헤더** 1개 |
| 네트워크 | 공개 API에 노출되면 위험 | Docker **내부망** 전제 |

**결정:** AI→BE는 **서버-to-서버**이므로 `X-Internal-Service` 헤더로 공유 시크릿을 검증한다. 사용자 신원은 요청 body/query의 `memberId`로 전달한다.

### 필터 동작 순서

`SecurityConfig` 필터 체인 (앞 → 뒤):

```
InternalServiceAuthenticationFilter  →  JwtAuthenticationFilter  →  ...
```

1. 요청 경로가 `InternalServicePaths` 화이트리스트에 **해당하는지** 확인
2. 해당 경로 + `X-Internal-Service` 헤더가 있으면 → 시크릿 값 비교
3. 일치 시 `ROLE_INTERNAL_SERVICE` 로 인증 완료 → **JWT 검사 생략**
4. 내부 경로가 아니면 필터 통과 → 일반 JWT 검증

### 설정

```yaml
internal:
  service:
    enabled: ${INTERNAL_SERVICE_AUTH_ENABLED:true}
    header-name: X-Internal-Service
    expected-value: ${INTERNAL_SERVICE_TOKEN:bifriends-ai}
```

- BE·AI **동일 환경변수** `INTERNAL_SERVICE_TOKEN` 사용 (운영에서 반드시 교체)
- 로컬 비활성화: `INTERNAL_SERVICE_AUTH_ENABLED=false`

### AI가 호출하는 BE API (화이트리스트)

`InternalServicePaths`에 등록된 경로만 internal 인증 적용:

| 용도 | 예시 경로 |
|------|-----------|
| Leo 할 일 CRUD | `POST/PATCH/DELETE /api/v1/todos` |
| 채팅 이력 조회 | `GET /api/v1/chat/messages` |
| 학습·프로필 조회 | `GET /api/v1/members/{id}/profile`, learning API |
| 주간 배치 콜백 | `POST /api/v1/weekly-safety-report`, `POST /api/v1/weekly-report` |
| 리포트 학습 요약 | `GET /api/v1/report/learning-summary` |

요청 예:

```http
POST /api/v1/weekly-report HTTP/1.1
X-Internal-Service: bifriends-ai
Content-Type: application/json

{ "memberId": 42, "sectionsJson": "..." }
```

### 관련 컨트롤러

- `InternalChatController`, `InternalMemberController`
- `InternalReportController`, `WeeklyReportCallbackController`
- `WeeklySafetyController`, `TodoController` (Leo 내부 API 구간)

---

## 3. BE → AI — 내부망 HTTP

BE가 AI를 호출할 때는 **별도 Authorization 헤더를 붙이지 않는다** (Docker compose 내부 URL, 예: `http://bifriends-ai:8001`).

| BE → AI | 예 |
|---------|-----|
| 동기 채팅 | `POST /api/v1/ai/chat` |
| 감정 시나리오 | `POST /api/v1/ai/content/scenario` |
| 주간 배치 트리거 | `POST /api/v1/ai/batch/weekly-safety`, `.../report/weekly` |

설정:

```yaml
ai:
  service:
    enabled: ${AI_SERVICE_ENABLED:false}
    base-url: ${AI_SERVICE_BASE_URL:http://bifriends-ai:8001}
```

**전제:** AI 서비스 포트는 외부에 직접 노출하지 않고, BE만 접근 가능하게 배포한다.

---

## 4. 비교 요약

| | FE → BE | AI → BE | BE → AI |
|---|---------|---------|---------|
| **헤더** | `Authorization: Bearer JWT` | `X-Internal-Service: {token}` | (없음) |
| **신원** | JWT `sub` = memberId | body/query `memberId` | BE가 지정 |
| **필터** | `JwtAuthenticationFilter` | `InternalServiceAuthenticationFilter` | — |
| **공개 인터넷** | ✅ (HTTPS) | ❌ 내부망 권장 | ❌ 내부망 |

---

## 5. 트레이드오프 · 알고 있는 한계

| 항목 | 내용 |
|------|------|
| Refresh Token | DB/Redis 미저장 — **강제 무효화 불가**, 클라이언트 폐기 의존 |
| Internal token | 고정 시크릿 — 유출 시 화이트리스트 API 호출 가능 → **내부망·env 관리** 필수 |
| 화이트리스트 | `InternalServicePaths` + 컨트롤러 **이중 수정** 누락 위험 |
| 부모 PIN | BE는 JWT만 검증, PIN UX는 FE·`parent_password` 컬럼과 협업 |

---

## 6. 관련 코드·문서

| 파일 | 역할 |
|------|------|
| `SecurityConfig.kt` | 필터 체인, STATELESS |
| `JwtProvider.kt` / `JwtAuthenticationFilter.kt` | JWT 발급·검증 |
| `OAuthController.kt` | Google 로그인 → JWT |
| `InternalServiceAuthenticationFilter.kt` | Internal 헤더 검증 |
| `InternalServicePaths.kt` | AI 전용 경로 화이트리스트 |
| `InternalServiceProperties.kt` | 헤더명·시크릿 설정 |
| `doc/api-spec.md` | 내부 API 명세 |
| `doc/ai/api_spec.md` | BE↔AI 계약 |

---

## 면접 한 줄

> “FE는 로그인 때만 Firebase로 검증하고, API는 자체 JWT로 Stateless 인증합니다. AI는 사용자가 아니라 서버이므로 JWT 대신 Docker 내부망에서 `X-Internal-Service` 공유 시크릿으로 BE 콜백을 보호하고, 경로는 화이트리스트로 제한했습니다.”
