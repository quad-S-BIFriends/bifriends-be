# 인증 아키텍처 — FE JWT · AI Internal Token

> **핵심 원칙:** FE와 AI는 **직접 통신하지 않는다.** BE가 유일한 중계자이며, **호출 주체에 따라 인증 방식이 다르다.**

| 통신 구간 | 프로토콜 | 인증 | 주체 |
|-----------|----------|------|------|
| **FE → BE** | HTTPS (운영) | `Authorization: Bearer {JWT}` | 사용자(앱) |
| **BE → AI** | HTTP (내부망) | 없음 | BE 서버 |
| **AI → BE** | HTTP (내부망) | `X-Internal-Service: {시크릿}` | bifriends-ai |
| **FE → AI** | — | **금지** | — |

[[기술적-의사결정]] · [[서비스-간-통신-아키텍처]] · [[API-명세]]

---

## 3자 통신 + 인증 (전체)

```
bifriends-client (Flutter)     bifriends-be (Kotlin)        bifriends-ai (Python)
        │                              │                            │
        │  HTTPS + JWT                 │                            │
        └─────────────────────────────▶│  HTTP (Docker 내부)         │
                                       └───────────────────────────▶│
                                       ◀───────────────────────────┘
                                         X-Internal-Service (콜백)
```

- **운영:** FE는 `https://api.bifriends.study` 만 호출 (Nginx → BE). AI URL은 앱에 없음.
- **로그인 1회만** Firebase ID Token → 이후 모든 FE API는 JWT.
- 통신·배포·개발 환경 차이: **[[서비스-간-통신-아키텍처]]**

---

## Spring Security 필터 체인

요청이 들어오면 **아래 순서**로 처리된다 (`SecurityConfig.kt`).

```
1. InternalServiceAuthenticationFilter   ← AI 내부 API (화이트리스트 + X-Internal-Service)
2. JwtAuthenticationFilter               ← FE API (Bearer JWT)
3. UsernamePasswordAuthenticationFilter  ← (미사용)
```

| 필터 | 이미 인증됐으면 | 동작 |
|------|----------------|------|
| Internal | skip | 화이트리스트 경로 + 헤더 일치 → `ROLE_INTERNAL_SERVICE`, JWT 생략 |
| JWT | skip | `Bearer` 파싱 → `memberId` → `ROLE_USER` |

`permitAll` (인증 없음): `/health`, `/api/v1/members/auth/**`, Swagger  
**그 외 전부** `authenticated()` — JWT 또는 Internal 중 하나 필요.

---

## 1. FE ↔ BE — JWT

### 왜 Firebase만 쓰지 않고 JWT를 따로 발급하나?

| | Firebase ID Token | 자체 JWT |
|---|-------------------|----------|
| 검증 | 매 요청 Firebase 서버 호출 | BE **로컬** 서명 검증 (`JwtProvider`) |
| 만료 | 약 1시간 | access **1h** / refresh **7d** |
| 주체 | Google OAuth | BE `memberId` |
| 세션 | — | **Stateless** (서버 세션 저장 없음) |

**결정:** 로그인 시에만 Firebase → 이후 API는 JWT로 지연·의존성 감소.

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

- `POST /api/v1/members/auth/google` (`OAuthController`)
- `FirebaseTokenVerifier` → `FirebaseAuth.verifyIdToken()`
- `JwtProvider` (JJWT, HMAC-SHA256)

### 이후 API 요청

```http
GET /api/v1/home HTTP/1.1
Host: api.bifriends.study
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

1. `JwtAuthenticationFilter.resolveToken()` — `Bearer ` 제거
2. `jwtProvider.validateToken()` — 서명·만료
3. `subject` → `memberId` → `SecurityContext`
4. 컨트롤러에서 `@RequestHeader("Authorization")` 또는 SecurityContext로 회원 식별

### JWT payload

| claim | 내용 |
|-------|------|
| `sub` | `memberId` |
| `email` | 회원 이메일 |
| `role` | `ROLE_USER` |
| `iat` / `exp` | 발급·만료 |

```yaml
jwt:
  secret: ${JWT_SECRET}              # 운영: openssl rand -base64 64
  access-token-expiration: 3600000     # 1시간
  refresh-token-expiration: 604800000  # 7일
```

### 로그아웃

- JWT Stateless → 서버에 세션 없음
- `POST /auth/logout` = 204, **클라이언트가 토큰 삭제**
- Refresh Token DB 미저장 → **강제 무효화 불가** (향후 Redis 블랙리스트 가능)

### FE API 예

| 기능 | 경로 | 인증 |
|------|------|------|
| 홈 | `GET /api/v1/home` | JWT |
| Leo 채팅 | `POST /api/v1/chat/messages` | JWT |
| 친구랑 | `POST /api/v1/mind/scenario` | JWT |
| 보호자 리포트 | `GET /api/v1/parent/reports` | JWT |

---

## 2. AI ↔ BE — X-Internal-Service

### 왜 JWT가 아닌가?

| 관점 | JWT | `X-Internal-Service` |
|------|-----|----------------------|
| 주체 | **사용자** | **서버** (bifriends-ai) |
| AI 콜백 | user JWT를 AI가 보관·갱신? 비현실적 | body/query `memberId` |
| 구현 | AI에 OAuth/JWT 인프라 필요 | **공유 시크릿 헤더** 1개 |
| 노출 | 공개 API에 노출 시 위험 | **Docker 내부망** + 화이트리스트 |

**결정:** AI→BE는 서버-to-서버. 사용자 신원은 **요청 데이터의 memberId**로 전달.

### InternalServiceAuthenticationFilter 동작

```
요청 수신
  → internal.service.enabled == false ? 통과
  → 경로가 InternalServicePaths 화이트리스트 ? 아니면 통과 (JWT로 감)
  → X-Internal-Service 헤더 없음 ? 통과 (JWT로 감)
  → 값 != expected-value ? 401
  → ROLE_INTERNAL_SERVICE 설정 → 다음 필터 (JWT skip)
```

### 설정 (BE · AI 동일 값)

```yaml
# BE application.yml
internal:
  service:
    enabled: ${INTERNAL_SERVICE_AUTH_ENABLED:true}
    header-name: X-Internal-Service
    expected-value: ${INTERNAL_SERVICE_TOKEN:bifriends-ai}
```

```yaml
# AI (deploy/docker-compose) — 동일 env
INTERNAL_SERVICE_TOKEN: ${INTERNAL_SERVICE_TOKEN}
BE_BASE_URL: http://bifriends-be:8080
```

- 운영: `deploy/.env`에서 `openssl rand -hex 32`로 생성
- 로컬 BE 단독: `INTERNAL_SERVICE_AUTH_ENABLED=false` 가능

### 화이트리스트 (`InternalServicePaths.kt`)

| 용도 | 경로 |
|------|------|
| Leo 할 일 | `POST/PATCH/DELETE /api/v1/todos` |
| 채팅 이력 | `GET /api/v1/chat/messages`, sessions messages |
| 학습·프로필 | `GET /api/v1/members/*/profile`, learning API |
| 배치 콜백 | `POST /api/v1/weekly-safety-report`, `weekly-report` |
| 학습 요약 | `GET /api/v1/report/learning-summary` |

요청 예:

```http
POST /api/v1/weekly-report HTTP/1.1
Host: bifriends-be:8080
X-Internal-Service: <INTERNAL_SERVICE_TOKEN>
Content-Type: application/json

{ "memberId": 42, "sectionsJson": "..." }
```

### Internal 전용 컨트롤러

- `InternalChatController`, `InternalMemberController`
- `InternalReportController`, `WeeklyReportCallbackController`
- `WeeklySafetyController`, `TodoController` (Agent 구간)

---

## 3. BE → AI — 인증 없음 (내부망 전제)

BE → AI는 **Authorization 헤더 없음**. `RestClient` + env URL.

| BE → AI | 경로 |
|---------|------|
| 채팅 | `POST {baseUrl}/api/v1/ai/chat` |
| 시나리오 | `POST .../api/v1/ai/content/scenario` |
| 배치 | `POST .../api/v1/ai/batch/...` |

```yaml
ai:
  service:
    enabled: ${AI_SERVICE_ENABLED:false}
    base-url: ${AI_SERVICE_BASE_URL:http://bifriends-ai:8001}
```

**보안 전제:** AI `8001` 포트를 **인터넷에 열지 않음** (배포 compose·Nginx).  
개발: `AI_SERVICE_ENABLED=false`로 AI 없이 BE만 동작 가능.

---

## 4. 구간별 비교

| | FE → BE | AI → BE | BE → AI |
|---|---------|---------|---------|
| **프로토콜** | HTTPS (운영) | HTTP | HTTP |
| **헤더** | `Authorization: Bearer JWT` | `X-Internal-Service` | (없음) |
| **신원** | JWT `sub` = memberId | body/query `memberId` | BE가 payload에 명시 |
| **필터** | `JwtAuthenticationFilter` | `InternalServiceAuthenticationFilter` | — |
| **외부 노출** | ✅ `api.bifriends.study` | ❌ | ❌ |

---

## 5. 트레이드오프 · 한계

| 항목 | 내용 |
|------|------|
| Refresh Token | DB/Redis 없음 — 탈취 시 만료까지 무효화 어려움 |
| Internal 시크릿 | 유출 + 내부망 침해 시 화이트리스트 API 호출 가능 |
| 화이트리스트 유지 | `InternalServicePaths` + 컨트롤러 **동시 수정** 필요 |
| 부모 PIN | BE는 JWT만, PIN 검증 UX는 FE 협업 |
| BE→AI 무인증 | 내부망 침해 시 AI 직접 호출 가능 → **네트워크 격리**가 방어선 |

---

## 6. 관련 코드·문서

| 파일 | 역할 |
|------|------|
| `SecurityConfig.kt` | 필터 순서, STATELESS |
| `JwtProvider.kt` / `JwtAuthenticationFilter.kt` | JWT |
| `OAuthController.kt` | Google → JWT |
| `InternalServiceAuthenticationFilter.kt` | Internal 헤더 |
| `InternalServicePaths.kt` | 화이트리스트 |
| `InternalServiceProperties.kt` | 설정 |
| `doc/api-spec.md` | API 명세 |
| `doc/ai/api_spec.md` | AI↔BE 계약 |
| [[서비스-간-통신-아키텍처]] | 레포 분리·배포·개발 통신 |

---

## 면접 한 줄

> “FE는 HTTPS+JWT로 BE만 호출하고, AI는 사용자가 아니라서 JWT 대신 `X-Internal-Service` 공유 시크릿과 경로 화이트리스트로 BE 콜백을 보호합니다. BE→AI는 내부망 HTTP이고 AI 포트는 외부에 안 엽니다. 레포는 나뉘지만 인증·URL은 env와 API 문서로 맞췄습니다.”
