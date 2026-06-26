# 7. API 명세

## Swagger UI (권장)

실제 엔드포인트·스키마·Try it out은 **Swagger**에서 확인합니다.

| 환경 | URL |
|------|-----|
| **운영** | https://api.bifriends.study/swagger-ui/index.html |
| **로컬** | http://localhost:8080/swagger-ui/index.html |
| **Docker** | http://localhost:18080/swagger-ui/index.html |

OpenAPI JSON: `/v3/api-docs`

---

## 인증

상세 설계: **[[인증-JWT-및-Internal-Token]]**

### 앱 (Flutter)

```
Authorization: Bearer {accessToken}
```

발급: `POST /api/v1/members/auth/google` + Firebase ID Token

### AI 서비스 (내부)

```
X-Internal-Service: {INTERNAL_SERVICE_TOKEN}
```

경로 목록: `InternalServicePaths.kt`

---

## 도메인별 Base Path (요약)

| 도메인 | Base Path |
|--------|-----------|
| 인증 | `/api/v1/members/auth` |
| 회원 | `/api/v1/members` |
| 온보딩 | `/api/v1/onboarding` |
| 홈 | `/api/v1/home` |
| 할 일 | `/api/v1/todos` |
| 수학 | `/api/v1/learning/math` |
| 국어 | `/api/v1/learning/korean` |
| 채팅 | `/api/v1/chat` |
| 친구랑 | `/api/v1/mind` |
| 리포트 | `/api/v1/reports` |
| 부모 | `/api/v1/parent` |
| 상점 | `/api/v1/shop` |

---

## 상세 문서 (레포)

| 문서 | 내용 |
|------|------|
| [`doc/api-spec.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/main/doc/api-spec.md) | 전체 API 명세 (Markdown) |
| [`doc/FRONTEND_INTEGRATION.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/main/doc/FRONTEND_INTEGRATION.md) | FE 연동 가이드 |
| [`doc/ai/api_spec.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/main/doc/ai/api_spec.md) | BE↔AI 계약 |

---

## Wiki UI 팁

이 페이지는 **Swagger로 링크만** 두고, 본문은 최소화하는 것을 권장합니다.  
스펙 변경 시 Swagger가 항상 최신에 가깝습니다.

## 관련

- [[환경-설정-및-규칙]]
- [[스택-및-아키텍처]]
