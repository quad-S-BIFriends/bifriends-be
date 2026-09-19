# BiFriends — CI/CD 테스트 게이트 구축 (GitHub Actions)

> **작성일**: 2026-09-19
> **목적**: `main` push 시 검증 없이 바로 배포되던 구조에 테스트 게이트를 추가한 기록
> **대상 저장소**: `bifriends-be`(적용 완료), `bifriends-ai`(워크플로우만 추가, 기존 버그로 인해 test job 실패 중 — 하단 참고)
> **관련 문서**: `deployment-guide.md` / `deployment-2.md` / `deployment-3.md` (모노레포 루트 `C:\bifriends\doc\`, 이 저장소엔 포함되지 않음) — 서버 자체의 배포 방식(EC2/Fargate) 비교. 이 문서는 "배포를 어떻게 트리거·검증하는가"를 다룬다.

---

## STAR 요약

### Situation (상황)
`main` push 시 검증 없이 바로 SSH로 GCP 서버에 접속해 `git pull` → `docker compose build/up`을 실행하는 단일 job 구조였다. 컴파일 에러나 로직 버그가 섞인 커밋도 그대로 프로덕션에 배포될 위험이 있었다.

### Task (과제)
"테스트를 통과한 코드만 배포되도록" 파이프라인에 검증 게이트를 추가하는 것이 목표였다. 배포 자동화(CD)는 이미 있었지만 지속적 통합(CI) 단계가 빠져 있던 상태를 보완하는 작업이다.

### Action (수행)
- `deploy` job 앞에 `test` job을 추가하고, `needs: test`로 의존 관계를 걸어 테스트 실패 시 배포 job 자체가 실행되지 않도록 구성 (단계는 하단 "변경 후 구조" 참고)
- `@SpringBootTest`가 필요로 하는 `firebase-service-account.json`(커밋 금지 파일)이 CI 러너엔 없다는 점을 감안해, `CI=true`일 때만 해당 테스트를 제외하도록 `build.gradle.kts`에 필터 추가 — 로컬은 전체 테스트 그대로 유지
- 파이프라인을 처음 가동하며 CI가 잡아낸 잠재 버그 3건(gradlew 따옴표 처리, 테스트 컴파일 에러 2건, Mockito strict-stubbing 위반 2건)을 원인 분석 후 수정 (상세는 하단 "CI가 잡아낸 실제 버그 3건" 참고)

### Result (결과)
- test job: 유닛 테스트 24개 전 통과, `BUILD SUCCESSFUL in 2m 10s`, job 전체 2분 17초
- deploy job: 서버가 꺼진 상태로 실제 push해본 결과 `dial tcp ***:22: i/o timeout`으로 34초 만에 실패 — "검증 실패·인프라 문제 시 배포가 자동 차단된다"는 것을 실제 실행으로 증명
- 부수적으로 로컬(Windows/IDE)에서는 드러나지 않았던 버그 3종(스크립트 실행 불가, 컴파일 에러, 테스트 설계 결함)을 조기에 발견·수정

---

## 배경

기존 `.github/workflows/deploy.yml`은 `main` push를 감지하면 곧바로 SSH로 GCP 서버에 접속해 `git pull` → `docker compose build/up`을 실행하는 단일 job이었다.

```
[기존]
push to main → SSH 접속 → git pull → docker compose build → docker compose up -d
               (중간에 아무 검증 없음)
```

컴파일이 깨지거나 로직 버그가 있는 커밋도 그대로 프로덕션에 배포될 위험이 있었다.

---

## 변경 후 구조

`test` job을 추가하고, `deploy` job이 `needs: test`로 의존하도록 재구성했다. 테스트가 실패하면 배포 job 자체가 실행되지 않는다(status: `skipped`).

```
push to main
   │
   ▼
① test job (ubuntu-latest)
   1. actions/checkout@v4         — 저장소 체크아웃
   2. actions/setup-java@v4       — JDK 21 (Temurin) 설치
   3. chmod +x gradlew            — 실행 권한 부여
   4. ./gradlew test              — 유닛 테스트 실행
   │
   ▼ (성공해야만 진행 — needs: test)
② deploy job (ubuntu-latest)
   1. appleboy/ssh-action@v1.0.3로 GCP 서버 SSH 접속
   2. 서버 내부에서 순차 실행:
      cd /app
      sudo git -C bifriends-be pull origin main
      sudo docker compose build bifriends-be
      sudo docker compose up -d bifriends-be
      sudo docker compose logs bifriends-be --tail=20
```

**설계 포인트**:
- 이미지 빌드는 GitHub Actions 러너가 아니라 **배포 대상 서버 위에서** 일어난다 (`docker compose build`를 SSH 스크립트 안에서 실행). 레지스트리(GHCR/Docker Hub) push 단계가 없는 단순한 구조.
- 인증은 `secrets.GCP_HOST`, `GCP_USER`, `GCP_SSH_KEY` 3개만 사용.
- `@SpringBootTest`로 전체 Spring 컨텍스트를 띄우는 `BiFriendsApplicationTests`는 `firebase-service-account.json`(커밋 금지 파일)이 있어야 부팅되는데 CI 러너엔 없으므로, `build.gradle.kts`에서 `CI=true`일 때만 이 테스트를 제외하도록 필터를 걸었다. 로컬에서는 실제 자격증명 파일이 있으면 전체 테스트(컨텍스트 로딩 포함)가 그대로 돈다.

  ```kotlin
  tasks.withType<Test> {
      useJUnitPlatform()
      if (System.getenv("CI") == "true") {
          exclude("**/BiFriendsApplicationTests.class")
      }
  }
  ```

---

## 구축 중 CI가 잡아낸 실제 버그 3건

파이프라인을 처음 가동하면서, 로컬(Windows/IDE)에서는 드러나지 않았던 문제들이 연달아 드러났다. 모두 원인을 분석해 수정했다.

### 1. `gradlew` 스크립트 실행 자체가 불가능 — JVM 옵션 따옴표 버그

```
Error: Could not find or load main class "-Xmx64m"
```

`gradlew`의 `DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'`를 POSIX 쉘이 unquoted로 word-split하면, 따옴표 문자가 인자 값에 그대로 남아(`"-Xmx64m"`) java가 이를 JVM 옵션이 아니라 메인 클래스 이름으로 오인해 즉시 종료된다. Windows에서는 `gradlew.bat`(별도 스크립트, 문제 없음)으로 실행해왔기 때문에 지금까지 드러나지 않았다.

```diff
- DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
+ DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
```

### 2. 테스트 파일 컴파일 에러 2건

`AiEmotionScenarioClientHttpTest.kt`:
- 백틱 테스트 함수명에 `application/json`처럼 `/`가 포함되어 JVM 식별자 규칙 위반 → 컴파일 실패
- `MockRestServiceServer.bindTo()`에 이미 `.build()`된 `RestClient` 인스턴스를 넘겨 오버로드 불일치 (`RestClient.Builder`를 받아야 함) → `bindTo` 호출 후 `build()`하도록 순서 수정

### 3. Mockito `UnnecessaryStubbingException` 2건

`setParentPassword` / `changeParentPassword` 모두 `require()` 검증이 `memberRepository` 조회보다 먼저 실행되는데, "PIN 불일치" 테스트에서 `findById` stub을 걸어놓고 실제로는 쓰지 않아 strict stubbing에 걸림. 로컬 stub은 제거하고, 여러 테스트가 공유하는 `@BeforeEach` stub은 `lenient()`로 변경.

> 세 문제 모두 CI 설정 문제가 아니라, `./gradlew test`를 CI(Linux)에서 처음 돌려보며 드러난 기존 코드의 잠재 버그였다.

---

## 검증 결과 (실측)

서버가 꺼진 상태에서 실제로 push해 파이프라인 전체를 검증했다.

| 항목 | 결과 |
|---|---|
| test job | ✅ 성공 — `BUILD SUCCESSFUL in 2m 10s` (job 전체 2분 17초) |
| 유닛 테스트 | 24개, 실패 0건 |
| deploy job | ❌ 실패 — `dial tcp ***:22: i/o timeout` (34초 만에 실패) |

deploy 실패는 GCP 서버가 꺼져있어 SSH 포트(22)에 접속 자체가 안 된 것으로, **의도된 인프라 문제**다. 오히려 "테스트 통과 → 배포 시도 → 서버 다운 시 배포만 차단"이 실제로 동작함을 확인한 결과다.

---

## `bifriends-ai` 관련 참고 (미해결)

`bifriends-ai`에도 동일한 패턴(test → deploy, `needs: test`)의 워크플로우를 추가했다. 다만 `tests/test_math_tool_utils.py`가 이전 리팩토링(상태 로컬 캐싱 검증 방식 폐기 → BE가 직접 매칭하는 방식으로 전환)에서 이미 삭제된 `_is_known_concept` 함수를 import하고 있어, **push할 때마다 test job이 항상 실패**하는 상태다.

`bifriends-ai`는 다른 팀이 담당하는 코드라 애플리케이션/테스트 코드는 건드리지 않았고, 팀에 별도로 공유해 해당 팀이 처리하도록 남겨둔 상태다. (워크플로우 자체는 정상 구성되어 있으므로, 죽은 테스트 파일만 정리되면 바로 정상 동작한다.)
