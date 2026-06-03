# AI 연동 문서

| 문서 | 설명 |
|------|------|
| [api_spec.md](./api_spec.md) | bifriends-ai ↔ bifriends-be API 명세 (AI 담당자 공유용) |

## 구현 현황 (2026-06-03)

| API | 상태 |
|-----|------|
| `GET /api/v1/members/{id}/profile` | ✅ |
| `POST` BE → AI `/v1/ai/chat` | ✅ BE 중계 |
| `POST /api/v1/chat/messages` (FE) | ✅ |
| `POST /api/v1/weekly-safety-report` (AI → BE) | ✅ |
| `POST /api/v1/weekly-report` (AI → BE) | ✅ |
| `GET /api/v1/report/learning-summary` (AI → BE) | ✅ |
| 그 외 AI → BE 내부 API | 🚧 Security만 등록 |

코드: `infrastructure/security/InternalService*`, `domain/chat/`, `infrastructure/ai/`
