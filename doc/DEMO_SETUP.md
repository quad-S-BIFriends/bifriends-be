# 시연용 데모 데이터 세팅 가이드 (macOS)

> 시연 영상 촬영 전, 로컬 환경에 **풀·학습·리포트·채팅·친구랑 히스토리**가 채워진 계정을 만드는 방법입니다.

---

## 1. 개요

| 구분 | 저장소 | 스크립트 |
|------|--------|----------|
| 학습, 풀, 할 일, 리포트, 채팅 | PostgreSQL (Docker) | `scripts/seed_demo.sh` |
| 친구랑 과거 세션 | Firestore | `scripts/seed_mind_firestore.py` |

**새 Google 계정을 만들 필요 없습니다.** 본인 계정으로 로그인한 뒤, 그 이메일로 시드를 실행하면 됩니다.

### 시드 실행 시 데이터 영향

| 대상 | 영향 |
|------|------|
| DB 전체 / 다른 사람 계정 | 영향 없음 |
| **시드 실행한 본인 계정** | 학습·할일·풀·채팅·리포트 등 **데모 데이터로 교체** |
| Google 로그인 | 유지 (같은 계정으로 계속 사용) |
| Firestore `demo-mind-*` 외 세션 | 유지 |

평소 테스트 데이터를 보존하려면 **다른 Google 계정**으로 로그인 후 시드를 실행하세요.

---

## 2. 사전 준비

### 필수 설치

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (실행 상태)
- [Flutter SDK](https://docs.flutter.dev/get-started/install/macos)
- Python 3 (`python3 --version`으로 확인)
- Xcode (iOS 시뮬레이터 사용 시)

### 코드

```bash
git pull   # demo 시드 스크립트가 포함된 브랜치
```

### 비밀 파일 (팀 DM으로 전달받기 — Git 커밋 금지)

| 받은 파일 | 복사 위치 |
|-----------|-----------|
| `.env` | `bifriends-be/.env` |
| `firebase-service-account.json` | `bifriends-be/src/main/resources/firebase-service-account.json` |

---

## 3. 실행 순서

### Step 1 — 백엔드 기동

```bash
cd bifriends-be
docker-compose up -d
```

| 서비스 | URL / 포트 |
|--------|------------|
| API | `http://localhost:18080` |
| PostgreSQL | `localhost:15432` |

컨테이너 확인:

```bash
docker ps
# bifriends-db, bifriends-be 가 Up 이어야 함
```

---

### Step 2 — Flutter 앱 실행

```bash
cd bifriends-client
flutter pub get
flutter run
```

iOS 시뮬레이터에서 실행하면 API 주소(`127.0.0.1:18080`)는 별도 설정 없이 동작합니다.

---

### Step 3 — Google 로그인 1회

1. 앱에서 **본인 Google 계정**으로 로그인
2. 온보딩을 진행해도 되고, 시드가 일부 프로필을 채워 줍니다
3. **로그인에 사용한 이메일**을 메모 (다음 단계에서 필요)

> `members` 테이블에 행이 생겨야 시드가 동작합니다. 로그인 전에 시드를 실행하면 오류가 납니다.

---

### Step 4 — PostgreSQL 데모 데이터

```bash
cd bifriends-be
chmod +x scripts/seed_demo.sh    # 최초 1회만
./scripts/seed_demo.sh your@gmail.com
```

`your@gmail.com` → Step 3에서 로그인한 **실제 이메일**로 바꿉니다.

성공 시 출력 예:

```text
Done! Reopen the app to verify.
  Parent mode PIN: 1234
  available_pool: 30
  22 learning attempts, 2 weekly reports, 3 chat sessions
```

---

### Step 5 — Firestore 친구랑 과거 세션

```bash
cd bifriends-be
python3 -m pip install -r scripts/requirements-seed.txt   # 최초 1회
python3 scripts/seed_mind_firestore.py --email your@gmail.com
```

성공 시 `demo-mind-001` ~ `003` 세션 3개가 생성됩니다.

Firestore DB ID가 `(default)`인 경우:

```bash
python3 scripts/seed_mind_firestore.py --email your@gmail.com --database-id "(default)"
```

(docker-compose 기본값은 `bifriends`)

---

### Step 6 — 앱에서 확인

앱을 **완전히 종료 후 다시 실행**하고 아래를 확인합니다.

| 화면 | 확인 항목 |
|------|-----------|
| 홈 | 풀 30개, streak 10일, 레벨 |
| 학습(수학/국어) | 진행 중인 스텝, 과거 풀이 이력 |
| 상점 | 아이템 구매 가능 (풀 30) |
| 채팅 | 과거 세션 3개 |
| 친구랑 | 히스토리 3개 (기쁨 / 고마움 / 속상함) |
| 부모 모드 | PIN **`1234`** |

---

## 4. 시드 후 데이터 요약

| 항목 | 값 |
|------|-----|
| 부모 모드 PIN | `1234` |
| available_pool | 30 |
| streak | 10일 |
| 정답 학습 시도 | 22건 |
| 할 일 이력 | 14일 |
| 주간 리포트 (PG) | 2건 |
| 채팅 세션 | 3개 |
| 친구랑 완료 세션 | 3개 |

---

## 5. 트러블슈팅

| 증상 | 원인 / 해결 |
|------|-------------|
| `members 테이블에 email이 없습니다` | 앱에서 Google 로그인 후 다시 실행 |
| `Docker container 'bifriends-db' is not running` | `docker-compose up -d` 실행 |
| API 연결 실패 | BE 컨테이너(`18080`) 기동 여부 확인 |
| Firestore 403 / permission denied | `firebase-service-account.json` 경로·내용 확인 |
| `firebase-admin` 없음 | `pip install -r scripts/requirements-seed.txt` |
| 친구랑 히스토리 0건 | Step 5 Firestore 시드 재실행 |
| 보호자 리포트 화면 | FE가 아직 mock 데이터 사용 중 — UI 시연은 mock으로도 가능. BE 실데이터는 PG 시드 + FE 연동 필요 |

---

## 6. 재실행 (멱등)

같은 이메일로 **여러 번 실행해도 됩니다.**

- PostgreSQL: 해당 회원의 데모 데이터 삭제 → 다시 생성
- Firestore: `demo-mind-*` 문서만 삭제 → 다시 생성

시연 직전에 한 번 더 돌려도 됩니다.

---

## 7. 한 줄 요약

```bash
cd bifriends-be && docker-compose up -d
# → Flutter 앱에서 Google 로그인
./scripts/seed_demo.sh your@gmail.com
python3 scripts/seed_mind_firestore.py --email your@gmail.com
```

부모 모드 PIN: **`1234`**

---

## 8. Windows 참고 (팀원 Mac 아닌 경우)

| macOS | Windows (PowerShell) |
|-------|----------------------|
| `./scripts/seed_demo.sh EMAIL` | `.\scripts\seed_demo.ps1 -Email "EMAIL"` |
| `python3 scripts/seed_mind_firestore.py --email EMAIL` | `.\scripts\seed_mind_firestore.ps1 -Email "EMAIL"` |

`chmod` / `./seed_demo.sh` 는 Mac/Linux 전용입니다. Windows에서는 `.ps1` 스크립트를 사용하세요.
