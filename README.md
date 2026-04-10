# BiFriends Backend

Spring Boot 기반 백엔드 API 서버.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 2.1, JDK 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA |
| Auth | Spring Security + OAuth2 (Google) + JWT |
| Build | Gradle (Kotlin DSL) |
| Container | Docker, Docker Compose |

## 실행 방법

```bash
# Docker Compose (DB + App)
docker-compose up -d

# 로컬 개발 (DB만 Docker, 앱은 직접 실행)
docker-compose up -d db
./gradlew bootRun
```

### 환경변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_HOST` | DB 호스트 | `localhost` |
| `DB_PORT` | DB 포트 | `5432` |
| `DB_NAME` | DB 이름 | `bifriends` |
| `DB_USERNAME` | DB 사용자 | `bifriends` |
| `DB_PASSWORD` | DB 비밀번호 | `bifriends` |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID | (필수) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 | (필수) |
| `JWT_SECRET` | JWT 서명 키 (256bit 이상) | 개발용 기본값 |

## 프로젝트 구조

```
src/main/kotlin/com/bifriends/
├── BiFriendsApplication.kt          # 엔트리포인트
├── global/                          # 프로젝트 전역 설정
│   └── config/                      # @Configuration 클래스
│       └── SecurityConfig.kt
├── domain/                          # 도메인 레이어 (DDD)
│   └── {도메인명}/                    # ex: member, post, chat ...
│       ├── controller/              # REST Controller
│       ├── service/                 # 비즈니스 로직
│       ├── model/                   # Entity, Enum, VO
│       └── repository/             # JPA Repository (interface)
├── infrastructure/                  # 기술 의존 구현체
│   └── security/                    # JWT, OAuth2 관련
└── controller/                      # 도메인에 속하지 않는 공통 컨트롤러
    └── HealthController.kt
```

## 설계 규칙

### API

- **base path**: `/api/v1`
- **네이밍**: 명사형 복수 (ex: `/api/v1/members`, `/api/v1/posts`)
- **HTTP Method**: REST 표준 준수
  - `GET` 조회 / `POST` 생성 / `PUT` 전체 수정 / `PATCH` 부분 수정 / `DELETE` 삭제
- **응답**: JSON

### DDD 패키지 구조

새 도메인 추가 시 아래 구조를 따른다:

```
domain/{도메인명}/
├── controller/     # @RestController — 요청/응답 처리만 담당
├── service/        # @Service — 비즈니스 로직, 트랜잭션 경계
├── model/          # @Entity, enum, VO — 도메인 모델
└── repository/     # JpaRepository interface
```

- **controller** → service만 의존. 다른 도메인의 controller를 직접 호출하지 않는다.
- **service** → 같은 도메인의 repository 의존. 다른 도메인은 해당 도메인의 service를 통해 접근한다.
- **model** → 순수 도메인 객체. 다른 레이어에 의존하지 않는다.
- **repository** → Spring Data JPA interface만 선언한다.

### global vs infrastructure

| 패키지 | 용도 | 예시 |
|--------|------|------|
| `global/config` | Spring 전역 설정 | SecurityConfig, WebConfig, CorsConfig |
| `global/exception` | 공통 예외 처리 | GlobalExceptionHandler, ErrorResponse |
| `global/common` | 공통 DTO, 유틸 | BaseEntity, PageResponse |
| `infrastructure/` | 외부 기술 구현체 | JWT, S3, Redis, 외부 API 클라이언트 |

### 컨벤션

- Entity 클래스에 `data class` 사용 금지 — JPA 프록시 호환 이슈
- Repository는 `JpaRepository<Entity, Long>` 상속
- REST Controller에는 `@RequestMapping("/api/v1/{도메인}")` prefix 사용
- 환경 설정값은 `application.yml`에 환경변수(`${VAR}`)로 관리 — 하드코딩 금지
