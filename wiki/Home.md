# BiFriends Backend Wiki

**느린학습자(경계선 지능) 아동용 앱** — Spring Boot 백엔드 (`bifriends-be`)

| | |
|---|---|
| **Repository** | [quad-S-BIFriends/bifriends-be](https://github.com/quad-S-BIFriends/bifriends-be) |
| **API (운영)** | https://api.bifriends.study |
| **Swagger** | https://api.bifriends.study/swagger-ui/index.html |
| **팀 노션** | [TEAM 5 BIFriend](https://gdg-sookmyung-25-26.notion.site/TEAM-5-BIFriend-33733fc61813816795abe94d93cef0ef) |

---

## Wiki 목차

| # | 문서 | 설명 |
|---|------|------|
| 1 | [[프로젝트-개요]] | 서비스 소개, 목표 사용자, 레포 구조 |
| 2 | [[스택-및-아키텍처]] | 기술 스택, 시스템 구성도, 데이터 저장소 |
| 3 | [[팀-및-개발-기간]] | 팀원, 역할, 일정 *(작성 중)* |
| 4 | [[나의-역할]] | 백엔드 담당 업무 범위 |
| 5 | [[기술적-의사결정]] | 주요 설계 선택과 이유 |
| | [[인증-JWT-및-Internal-Token]] | FE JWT · AI Internal Token |
| | [[서비스-간-통신-아키텍처]] | 3레포 HTTP · Docker · 개발/배포 |
| 6 | [[트러블슈팅-및-이슈]] | 해결한 버그·운영 이슈 (요약표 + 상세 링크) |
| 7 | [[API-명세]] | Swagger UI, 내부 API 개요 |
| 8 | [[환경-설정-및-규칙]] | 로컬/배포, 코딩 규칙, 문서 위치 |

---

## Monorepo 안내

이 Wiki는 **백엔드 레포** 기준입니다. 프론트(`bifriends-client`)·AI(`bifriends-ai`)는 별도 레포/폴더에서 관리합니다.

```
bifriends/
├── bifriends-be/      ← 이 Wiki
├── bifriends-client/
└── bifriends-ai/
```

---

## 빠른 링크

- 상세 API 스펙 (레포 내): [`doc/api-spec.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/main/doc/api-spec.md)
- 배포: [`deploy/DEPLOY.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/main/deploy/DEPLOY.md)
- Hikari 부하 테스트: [[Chat-Hikari-풀-테스트]] · [`doc/chat/hikari_pool_test.md`](https://github.com/quad-S-BIFriends/bifriends-be/blob/test/chat/doc/chat/hikari_pool_test.md)
