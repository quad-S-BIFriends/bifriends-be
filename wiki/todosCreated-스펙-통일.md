# todosCreated 스펙 통일 (#6)

> Leo 채팅에서 AI가 반환한 할 일 목록(`todosCreated`)의 **BE↔AI JSON 스펙 불일치**로 500 발생.

| | |
|---|---|
| **이슈** | [#6](https://github.com/quad-S-BIFriends/bifriends-be/issues/6) |
| **구분** | 스펙 / API 계약 |

[[트러블슈팅-및-이슈]]

---

## STAR

### Situation

- Leo에게 “할 일 등록해줘” 등 요청 후 채팅 응답 처리 중 **500**
- AI는 `{ title, assigned_date }` 객체 배열 반환
- BE는 `long[]` (todo ID 목록)으로 역직렬화 시도 → Jackson 실패

### Task

- bifriends-ai 응답 스펙과 BE DTO를 **동일 계약**으로 맞추기
- 할 일 생성 로직과 응답 필드 정합

### Action

- `ChatTodoCreated(title, assigned_date)` DTO 도입
- `todosCreated` 필드를 객체 배열로 역직렬화
- AI 스펙·`doc/ai/api_spec.md`와 정렬

### Result

- Leo 할 일 등록 플로우 정상화
- BE↔AI 계약 문서화 사례로 회귀 방지 참고

---

## 관련 파일

- 채팅 응답 DTO (`ChatTodoCreated` 등)
- `AiChatClient` / AI 응답 매핑
