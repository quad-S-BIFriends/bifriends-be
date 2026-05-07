# BiFriends 백엔드 — AI 지시문

## 스택
- **언어/런타임**: Kotlin 2.1 / JDK 21
- **프레임워크**: Spring Boot 3.4
- **DB**: PostgreSQL 17 (`localhost:15432` 개발, `localhost:15432` Docker)
- **인증**: Firebase Admin SDK + Google OAuth2 + JWT
- **API 문서**: springdoc-openapi 2.8.6 (Swagger)

## 로컬 실행
```
docker-compose up -d db && ./gradlew bootRun
```
- App: `localhost:8080`
- DB 접속: `psql -h localhost -p 15432 -U bifriends -d bifriends`

---

## 패키지 구조 규칙

```
com.bifriends/
├── domain/{name}/
│   ├── controller/   # REST 엔드포인트
│   ├── service/      # 비즈니스 로직, 스케줄러, 이벤트 리스너
│   ├── dto/          # 요청·응답 DTO
│   ├── model/        # JPA 엔티티, 도메인 정책 object
│   └── repository/   # Spring Data JPA
├── domain/member/
│   └── event/        # 도메인 이벤트 클래스 (MemberRegisteredEvent 등)
├── global/
│   ├── config/       # Spring 설정 (@Configuration)
│   ├── exception/    # 전역 예외 처리
│   └── common/       # 공통 유틸
└── infrastructure/
    └── security/     # SecurityConfig, JWT 필터 등
```

**이벤트 클래스 위치**: 이벤트를 *발행하는* 도메인의 `event/` 패키지에 둔다.  
**이벤트 리스너 위치**: 이벤트를 *처리하는* 도메인의 `service/` 패키지에 둔다.

---

## 핵심 코딩 규칙

- **Entity에 `data class` 금지** — JPA 프록시가 equals/hashCode를 오염시킴
- **Base path**: 모든 API는 `/api/v1` 접두어 사용
- **새 도메인 추가 시** `SecurityConfig.kt` 경로 허용 여부 반드시 업데이트
- **`firebase-service-account.json` 절대 커밋 금지**
- **KST 명시**: 날짜/시간 계산 시 `ZoneId.of("Asia/Seoul")` 항상 명시
- **도메인 이벤트 트랜잭션 페이즈**:
  - 수신자 실패가 발행자 롤백으로 번지면 안 될 때 → `AFTER_COMMIT`
  - 원자성이 필요할 때 (함께 성공·실패해야 할 때) → `@EventListener` (같은 트랜잭션)

---

## 문서 작성 규칙

> 모든 문서는 `bifriends-be/doc/` 하위에 작성한다.

### 1. 단일 도메인 기능

기능이 하나의 도메인(`domain/{name}`) 안에서 완결될 때.

```
doc/{도메인명}/
├── README.md          # 1페이지 구현 요약 (구현 범위 표, 핵심 설계 결정, DB 스키마)
└── {기능명}_details.md # 상세 설계 문서
```

**예시**: `doc/todo/README.md`, `doc/todo/todo_details.md`

### 2. 크로스 도메인 상호작용 (이벤트·오케스트레이션)

둘 이상의 도메인이 연관될 때. **Spring ApplicationEvent**가 주요 연결 메커니즘.

```
doc/events/README.md   # 전체 이벤트 레지스트리 (단일 파일로 관리)
```

- 새 이벤트 추가 시 `doc/events/README.md` 표에 행 추가
- 관련 도메인 `_details.md`에도 해당 이벤트 섹션을 추가하고 `doc/events/README.md`로 링크

### 3. 상세 설계 문서(`_details.md`) 필수 섹션

| 번호 | 섹션명 | 내용 |
|---|---|---|
| 1 | 배경 및 목적 | 왜 이 기능이 필요한가, 기능명세 연결 |
| 2 | 설계 결정 | A안 vs B안 비교, 채택 이유 |
| 3 | 소프트웨어 설계 고려 사항 | 패턴 선택 이유, 트레이드오프 |
| 4 | 패키지 구조 설계 이유 | 왜 이 위치에 두었는가 |
| 5 | 향후 고려 사항 | MVP 한계와 확장 방안 (표 형식) |
| — | 크로스 도메인 기능이면 추가 | 연관 이벤트 설명 + `doc/events/README.md` 링크 |

### 4. 작성 언어 규칙
- 본문: **한국어**
- 코드·식별자·파일명: **영어** 그대로 사용
- 이모지: 강조 목적으로 절제하여 사용

### 5. 문서 갱신 시점
- 새 도메인 추가 → `doc/{도메인}/` 폴더와 README 생성
- 기존 기능에 크로스 도메인 상호작용 추가 → `doc/events/README.md` 업데이트 + 관련 `_details.md` 섹션 추가
- API 스펙 변경 → 루트 `CLAUDE.md`의 "공통 인터페이스 변경 시" 규칙에 따라 클라이언트 영향도 함께 언급
