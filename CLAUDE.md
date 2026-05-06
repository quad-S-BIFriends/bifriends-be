# BiFriends Backend — AI 지시문

## 프로젝트
느린학습자(경계선 지능) 아동용 앱의 Spring Boot 백엔드.
스택: Kotlin 2.1 / Spring Boot 3.4 / JDK 21 / PostgreSQL 17 / Firebase Admin SDK / JWT

## 환경 (로컬)
- App: `localhost:18080` (Docker) / `localhost:8080` (bootRun)
- DB: `localhost:15432` → `psql -h localhost -p 15432 -U bifriends -d bifriends`
- 실행: `docker-compose up -d db && ./gradlew bootRun`

## 패키지 구조 & 의존 방향
```
com.bifriends/
├── domain/{name}/controller → service → repository, model
├── global/config|exception|common   # Spring 전역 설정, 공통 예외/DTO
└── infrastructure/security          # JWT, Firebase, 외부 기술 구현체
```
- controller는 같은 도메인 service만 호출. 다른 도메인 controller 직접 호출 금지.
- 다른 도메인 접근은 반드시 해당 도메인 service 경유.
- model은 어떤 레이어도 의존하지 않음.

## 코딩 규칙 (위반 금지)
- Entity에 `data class` 사용 금지 — JPA 프록시 호환 이슈
- 새 도메인 구조: `controller / service / dto / model / repository`
- 모든 설정값은 `application.yml`의 `${VAR:default}` 패턴으로 관리, 하드코딩 금지
- Base path: `/api/v1`, 응답 형식: JSON
- 새 도메인 추가 후 반드시 `SecurityConfig.kt`에 인증 규칙 추가

## 주의사항
- `firebase-service-account.json` — `src/main/resources/`에 위치, 커밋 절대 금지
- `ddl-auto: update` — 개발 전용. 프로덕션은 `validate` 또는 Flyway 사용

## Token Efficiency
- `build/`, `.gradle/`, `.idea/`, `*.class`, `*.log`, `.env`, `*service-account*.json` 절대 읽지 말 것
- 파일 전체 읽기 전 `grep`/`find`로 필요한 부분만 탐색
- 대용량 파일은 필요한 라인 범위만 읽을 것

## Behavior
- 작업 시작 전: 할 일을 2-3줄로 먼저 말하고 진행
- 작업 완료 후: 변경된 파일 목록과 한 줄 요약만 출력
- DB 스키마 변경이 필요하면 SQL을 먼저 보여주고 Kotlin 코드 작성
- 기존 도메인 구조 밖에 파일 생성 시 반드시 먼저 물어볼 것
