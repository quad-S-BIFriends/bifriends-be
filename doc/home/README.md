# 홈 화면 (Home) 구현 요약

> 관련 기능명세: Functional Specification — 단위 시스템: 홈 (HOM-08-01 ~ HOM-08-09)  
> 관련 문서: [상세 설계 문서](./home_details.md) · [API 연동 흐름](./api_flow.md)

---

## 구현 범위

| 항목 | 파일 | 설명 |
|---|---|---|
| 인사 유형 | `model/GreetingType.kt` | FIRST_LOGIN / COMEBACK_SHORT / COMEBACK_LONG / STREAK |
| 인사 메시지 정책 | `model/GreetingPolicy.kt` | 유형 결정 + 메시지 풀 + 닉네임 치환 |
| 홈 응답 DTO | `dto/HomeDtos.kt` | HomeResponse, MemberSummary, GreetingResponse |
| 홈 서비스 | `service/HomeService.kt` | 출석 처리 + 인사 메시지 + 할 일 조회 오케스트레이션 |
| 홈 컨트롤러 | `controller/HomeController.kt` | GET /api/v1/home |

---

## 핵심 설계 결정

### 단일 홈 API

홈 화면 진입 시 필요한 모든 데이터(인사 메시지, 레벨/풀/streak, 할 일 목록)를 `GET /api/v1/home` 한 번으로 내려준다.  
동시에 **출석 체크(streak 갱신 + 보상 지급)** 도 이 API 안에서 처리된다.

```
GET /api/v1/home 응답 구조
├── member      닉네임
├── greeting    인사 유형 + streak일수 + 메시지 텍스트
├── stats       레벨 · 풀 · streak · 프로그레스 바 수치
├── attendance  오늘 출석 처리 결과 (보상 지급 여부)
└── todos       오늘의 할 일 목록
```

### 출석 = 홈 화면 진입

"로그인"이 아닌 **홈 화면 진입**을 출석 기준으로 삼는다.  
JWT 토큰이 유효한 한 앱이 계속 로그인 상태를 유지하므로, 매일 앱을 열고 홈 화면이 로드될 때마다 streak이 체크된다.  
`UserStats.lastAttendanceDate`로 오늘 이미 처리됐는지 판단하여 멱등성을 보장한다.

### pre/post attendance 분리

인사 유형(first_login / comeback / streak)은 출석 처리 **전(前)** `lastAttendanceDate`로 결정하고,  
STREAK 세부 bucket(1일 / 2~3일 / 4~6일 / 7+일)은 출석 처리 **후(後)** `streakDays`로 결정한다.

### GreetingPolicy object

`LevelPolicy`와 동일하게 순수 `object`로 분리. Spring/DB 없이 단위 테스트 가능.

---

## API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/v1/home` | 홈 화면 단일 조회 (출석 처리 포함) | Bearer JWT |

> 할 일 완료 API (`PATCH /api/v1/todos/{id}/complete`) 및 Agent CRUD API는 컨트롤러 레이어 미구현 상태.

---

## 다음 단계

- [ ] `PATCH /api/v1/todos/{id}/complete` 컨트롤러 작성
- [ ] Agent Todo CRUD 컨트롤러 작성 (`POST/PATCH/DELETE /api/v1/todos`)
- [ ] 완료 API 응답에 갱신된 stats 포함 여부 결정 (TodoCompleteResult 확장)
- [ ] 전역 예외 처리 (`@ControllerAdvice`) — IllegalStateException → 400, IllegalArgumentException → 404
