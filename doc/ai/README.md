# AI 연동 문서

| 문서 | 설명 |
|------|------|
| [api_spec.md](./api_spec.md) | bifriends-ai ↔ bifriends-be API 명세 (AI 담당자 공유용) |

## 구현 현황 (2026-05-28)

| API | 상태 |
|-----|------|
| `GET /api/v1/members/{id}/profile` | ✅ |
| `POST` BE → AI `/v1/chat` | ✅ BE 중계 (AI URL 협의) |
| `POST /api/v1/chat/messages` (FE) | ✅ |
| 그 외 AI → BE 내부 API | 🚧 Security만 등록 |

코드: `infrastructure/security/InternalService*`, `domain/chat/`, `infrastructure/ai/`
