# BiFriends Backend

느린학습자(경계선 지능) 아동용 앱의 **Spring Boot 기반 백엔드 API 서버**입니다.

> 팀 노션: https://gdg-sookmyung-25-26.notion.site/TEAM-5-BIFriend-33733fc61813816795abe94d93cef0ef?pvs=143



> <img width="506" height="700" alt="bifriends-poster" src="https://github.com/user-attachments/assets/a7bf371b-f7ef-4976-bf0c-a2703a070e90" />


---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 2.1 / JDK 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA (Hibernate) |
| Auth | Firebase Admin SDK (ID Token 검증) + JWT |
| BaaS | Firebase Admin SDK (Auth + Firestore + Storage) |
| API Docs | springdoc-openapi 2.8.6 (Swagger UI) |
| Build | Gradle (Kotlin DSL) |
| Container | Docker, Docker Compose |

---

## 실행 방법

```bash
# 1. DB만 Docker로 띄우고 앱은 로컬 실행 (개발 권장)
docker-compose up -d db
./gradlew bootRun
# → App: http://localhost:8080
# → Swagger: http://localhost:8080/swagger-ui/index.html

# 2. DB + App 전체 Docker 실행
docker-compose up -d
# → App: http://localhost:18080

# DB 직접 접속
psql -h localhost -p 15432 -U bifriends -d bifriends
```

---

## 환경변수

`application.yml`에서 참조하는 환경변수 목록입니다. 기본값이 없는 항목은 반드시 설정해야 합니다.

### 필수

| 변수 | 설명 |
|------|------|
| `FIREBASE_CONFIG_PATH` | Firebase 서비스 계정 JSON 경로 (기본: `classpath:firebase-service-account.json`) |
| `FIREBASE_STORAGE_BUCKET` | Firebase Storage 버킷명 (기본: `bifriends-5df72.firebasestorage.app`) |
| `INTERNAL_SERVICE_TOKEN` | AI 서비스 내부 인증 토큰 (기본: `bifriends-ai`) |

### 선택 (기본값 있음)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_HOST` | DB 호스트 | `localhost` |
| `DB_PORT` | DB 포트 | `15432` |
| `DB_NAME` | DB 이름 | `bifriends` |
| `DB_USERNAME` | DB 사용자 | `bifriends` |
| `DB_PASSWORD` | DB 비밀번호 | `bifriends` |
| `JWT_SECRET` | JWT 서명 키 (256bit 이상 권장) | 개발용 기본값 |
| `AI_SERVICE_BASE_URL` | AI 서비스 주소 | `http://bifriends-ai:8001` |
| `AI_SERVICE_ENABLED` | AI 연동 활성화 여부 | `false` |

> `firebase-service-account.json`은 절대 커밋하지 않습니다. 팀 채널에서 파일을 받아 `bifriends-be/src/main/resources/`에 배치하세요.

---

## 프로젝트 구조

```
src/main/kotlin/com/bifriends/
├── BiFriendsApplication.kt
├── controller/
│   └── HealthController.kt              # GET /health
├── global/
│   ├── config/
│   │   ├── SecurityConfig.kt            # Spring Security 필터 체인
│   │   ├── FirebaseConfig.kt            # Firebase Admin SDK 초기화
│   │   ├── InternalServiceConfig.kt     # AI 내부 서비스 설정
│   │   └── SchedulingConfig.kt
│   └── exception/
├── domain/
│   ├── member/                          # 회원/인증
│   │   ├── controller/
│   │   │   ├── OAuthController.kt       # POST /auth/google, POST /auth/logout
│   │   │   ├── MemberController.kt      # 회원 정보 CRUD
│   │   │   └── InternalMemberController.kt  # Leo 전용 (프로필, 학습 진도)
│   │   ├── service/  model/  repository/
│   │   └── event/
│   │       └── MemberRegisteredEvent.kt
│   ├── onboarding/                      # 온보딩 6단계
│   ├── home/                            # 홈 화면 (할 일, 보상, 레벨, 인사말)
│   │   ├── controller/
│   │   │   ├── HomeController.kt
│   │   │   └── TodoController.kt
│   │   └── service/  model/  repository/
│   ├── learning/                        # 학습 (수학 / 국어)
│   │   ├── controller/
│   │   │   ├── StudyMathController.kt
│   │   │   └── StudyKoreanController.kt
│   │   └── service/  model/  repository/
│   ├── chat/                            # Leo AI 대화
│   │   ├── controller/
│   │   │   ├── ChatController.kt
│   │   │   └── InternalChatController.kt  # Leo 전용
│   │   └── service/  model/  repository/
│   ├── emotion/                         # 감정 시나리오 (친구랑 탭)
│   ├── mind/                            # 마음 세션 (마음이랑 탭)
│   ├── report/                          # 주간 성장 리포트
│   │   ├── controller/
│   │   │   ├── ReportController.kt          # 부모 모드 리포트 조회
│   │   │   ├── InternalReportController.kt  # Leo 전용 학습 집계
│   │   │   └── WeeklyReportCallbackController.kt  # AI 콜백
│   │   └── service/  model/  repository/
│   ├── safety/                          # 챗 안전 신호
│   ├── parent/                          # 부모 모드 (PIN, 대시보드)
│   └── shop/                            # 아이템 상점
└── infrastructure/
    ├── security/
    │   ├── JwtProvider.kt
    │   ├── JwtAuthenticationFilter.kt
    │   ├── FirebaseTokenVerifier.kt      # Firebase ID Token 검증
    │   ├── InternalServiceAuthenticationFilter.kt
    │   └── InternalServicePaths.kt       # Leo 전용 API 경로 목록
    ├── firebase/
    │   ├── FirestoreService.kt           # Firestore 감정 세션 데이터
    │   └── FirebaseStorageService.kt     # Firebase Storage 이미지
    └── ai/
        └── dto/                          # AI 서비스 요청/응답 DTO
```

---

## 인증 구조

### Flutter 앱 (일반 사용자)

```
Flutter 앱
 └─ Google Sign-In → Firebase 인증 → Firebase ID Token 획득
    └─ POST /api/v1/members/auth/google  { idToken: "..." }
       └─ FirebaseTokenVerifier.verifyIdToken()
          └─ Member 조회/생성 → JWT(accessToken + refreshToken) 반환
```

이후 모든 API 요청에 `Authorization: Bearer {accessToken}` 헤더를 포함합니다.

### AI 서비스 (Leo, 내부망)

Docker 내부망에서 `X-Internal-Service: {token}` 헤더로 인증합니다. JWT 없음.  
허용된 경로 목록은 `InternalServicePaths.kt`에서 관리합니다.

---

## API 도메인 요약

상세 명세는 Swagger UI에서 확인하세요: `http://localhost:8080/swagger-ui/index.html`

| 도메인 | Base Path | 주요 기능 |
|--------|-----------|-----------|
| 인증 | `/api/v1/members/auth` | Google 로그인 (Firebase), 로그아웃 |
| 회원 | `/api/v1/members` | 프로필 조회/수정, 설정 변경 |
| 온보딩 | `/api/v1/onboarding` | 6단계 온보딩 (약관, 프로필, 관심사, 선물, 완료) |
| 홈 | `/api/v1/home`, `/api/v1/todos` | 홈 정보, 할 일 CRUD, 보상 |
| 학습 | `/api/v1/study/math`, `/api/v1/study/korean` | 수학/국어 학습 진행 |
| 대화 | `/api/v1/chat` | Leo AI 채팅 세션 및 메시지 |
| 감정 | `/api/v1/emotion` | 감정 시나리오 생성 및 조회 |
| 마음 | `/api/v1/mind` | 마음 세션 생성 및 조회 |
| 리포트 | `/api/v1/reports` | 주간 성장 리포트 목록/상세, 보호자 미션 |
| 안전 | `/api/v1/weekly-safety-report` | 챗 안전 신호 |
| 부모 | `/api/v1/parent` | 부모 모드 PIN 설정/검증, 대시보드 |
| 상점 | `/api/v1/shop` | 아이템 구매 및 착용 |
| 내부(Leo) | `/api/v1/report/learning-summary`<br>`/api/v1/members/{id}/profile` 등 | AI 서비스 전용 (X-Internal-Service 인증) |

---

## 설계 규칙

### 패키지 구조

새 도메인 추가 시 아래 구조를 따릅니다:

```
domain/{도메인명}/
├── controller/   # @RestController — 요청/응답 처리만 담당
├── service/      # @Service — 비즈니스 로직, 트랜잭션 경계
├── dto/          # 요청·응답 DTO
├── model/        # @Entity, enum, VO — 도메인 모델 (data class 금지)
└── repository/   # JpaRepository interface
```

- `domain/{name}/event/` — 해당 도메인이 **발행**하는 이벤트 클래스
- 이벤트 **리스너**는 처리하는 도메인의 `service/`에 위치
- 도메인 간 의존: controller → 타 domain controller 직접 호출 금지, service를 통해 접근

### 주요 컨벤션

| 항목 | 규칙 |
|------|------|
| Entity | `data class` 금지 (JPA 프록시 이슈) |
| Base path | 모든 API `/api/v1` 접두어 |
| 시간대 | 날짜/시간 계산 시 `ZoneId.of("Asia/Seoul")` 명시 |
| 새 도메인 추가 | `SecurityConfig.kt` 경로 허용 여부 반드시 업데이트 |
| 환경 설정값 | `application.yml`에서 `${VAR}` 방식으로 관리, 하드코딩 금지 |
| 도메인 이벤트 | 실패가 발행자 롤백으로 번지면 안 될 때 → `AFTER_COMMIT`, 원자성 필요 시 → `@EventListener` |

### global vs infrastructure

| 패키지 | 용도 |
|--------|------|
| `global/config` | Spring 전역 설정 (SecurityConfig 등) |
| `global/exception` | 공통 예외 처리 |
| `global/common` | 공통 DTO, 유틸 |
| `infrastructure/` | 외부 기술 구현체 (JWT, Firebase, AI 클라이언트) |
