# Chat Hikari 커넥션 풀 부하 테스트

> **Leo 채팅(톡톡)** `ChatService.sendMessage()` 트랜잭션 분리 전·후의 DB 커넥션 대기를 정량 비교한 문서입니다.  
> 친구랑 감정 시나리오(`/mind`)와 **무관**합니다.

| | |
|---|---|
| **관련 이슈** | [#5](https://github.com/quad-S-BIFriends/bifriends-be/issues/5), [#7](https://github.com/quad-S-BIFriends/bifriends-be/issues/7) |
| **레포 상세 문서** | [`doc/chat/hikari_pool_test.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/test/chat/doc/chat/hikari_pool_test.md) *(main 머지 전)* |
| **테스트 코드** | `ChatHikariPoolComparisonTest.kt` |

[[트러블슈팅-및-이슈]] · [[기술적-의사결정]]

---

## STAR 요약

### Situation / Task

- Leo 채팅·동시 API 사용 시 체감 지연
- AI HTTP 대기 중에도 `@Transactional`이 DB 커넥션을 붙잡는 구조
- TX 분리 후 **정량 검증** 필요

### Action

| | LEGACY (수정 전) | FIXED (현재) |
|---|---|---|
| 패턴 | 단일 TX 안에서 user 저장 → AI 대기 → assistant 저장 | `NOT_SUPPORTED` + `ChatMessageWriteService` 분리 |
| 테스트 | `ChatHikariPoolComparisonTest` — 동시 **12건** `sendMessage` | 동일 |
| 환경 | H2, Hikari **pool=5**, AI **mock 2초** | 동일 |

검증함: `ChatService` + JPA 저장, `threadsAwaitingConnection`, 부하 중 DB 프로브  
검증 안 함: HTTP/JWT, 실제 bifriends-ai, PostgreSQL

### Result

| 지표 | LEGACY | FIXED |
|------|--------|-------|
| 성공 / 실패 | 12 / 0 | 12 / 0 |
| **max threadsAwaiting** | **8** | **0** |
| **채팅 12건 처리 총 소요** | ~6.2s | ~2.0s |
| DB 프로브 max | ~4.2s | 0ms |

**해석:** Leo 응답 시간 자체보다, **DB 풀 고갈로 다른 API가 막히지 않게** 한 개선. 풀 5는 운영값이 아니라 현상을 크게 보이게 한 **실험 설계**입니다.

---

## 코드 수정 요약

```
[수정 전] 커넥션 획득 → user INSERT → AI 대기(커넥션 점유) → assistant INSERT → 반환
[수정 후] 짧은 TX(user) → 커넥션 반환 → AI 대기 → 짧은 TX(assistant)
```

```kotlin
@Transactional(propagation = Propagation.NOT_SUPPORTED)
fun sendMessage(...) {
    chatMessageWriteService.saveUserMessage(...)
    val aiResponse = aiChatClient.sendChat(...)  // TX 없음
    aiResponse.reply?.let { chatMessageWriteService.saveAssistantMessage(...) }
}
```

---

## 실행 방법

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
cd bifriends-be
.\gradlew test --tests "com.bifriends.domain.chat.ChatHikariPoolComparisonTest"
```

Docker: `scripts/chat_hikari_pool_test.ps1`

리포트: `build/reports/tests/test/classes/com.bifriends.domain.chat.ChatHikariPoolComparisonTest.html`

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `ChatHikariPoolComparisonTest.kt` | LEGACY vs FIXED 비교 |
| `LegacyChatTransactionSimulator.kt` | 수정 전 패턴 재현 |
| `application-chat-pool-test.yml` | H2, pool=5 |
| `ChatService.kt` / `ChatMessageWriteService.kt` | TX 분리 구현 |
