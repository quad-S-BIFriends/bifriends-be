# Firestore DB ID 장애 (#8)

> Firebase 콘솔은 named DB `bifriends`인데, BE가 `(default)`에 연결해 **친구랑·지난 여행 전면 503**이 발생한 운영 장애.

| | |
|---|---|
| **이슈** | [#8](https://github.com/quad-S-BIFriends/bifriends-be/issues/8) |
| **구분** | 운영 장애 |
| **영향** | `MindSessionService`, 시나리오 생성, 주간 리포트 학습 요약 |

[[트러블슈팅-및-이슈]]

---

## STAR

### Situation

- 운영·테스트에서 친구랑·지난 마음 여행 **503**
- 로그: `The database (default) does not exist for project bifriends-5df72`

### Task

- 콘솔 DB 이름(`bifriends`)과 BE 연결 설정 일치
- 로컬·VM 배포 환경 모두 재발 방지

### Action

1. `application.yml`에 `firebase.firestore.database-id: ${FIRESTORE_DATABASE_ID:(default)}` 추가
2. `deploy/docker-compose.yml`·`.env.example`에 `FIRESTORE_DATABASE_ID=bifriends` 반영
3. VM `/app/.env`·compose **수동 동기화** (CI는 이미지만 갱신)

**원인 요약:** `FIRESTORE_DATABASE_ID` env는 있었으나 yaml 매핑이 없어 Spring이 `(default)` 사용.

### Result

- 기대 로그: `[Firestore] named database 사용 — databaseId=bifriends`
- 친구랑 세션 저장·조회, 리포트 학습 요약 정상화

---

## 검증

```bash
docker logs bifriends-be 2>&1 | grep -i firestore
```

---

## 관련 파일

- `FirestoreService.kt` — `database-id` 주입
- `application.yml`, `deploy/docker-compose.yml`, `deploy/.env.example`
