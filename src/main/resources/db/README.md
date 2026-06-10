docker cp src/main/resources/db/math_seed.sql bifriends-db:/tmp/
docker cp src/main/resources/db/korean_seed.sql bifriends-db:/tmp/
docker cp src/main/resources/db/shop_seed.sql bifriends-db:/tmp/
docker exec bifriends-db psql -U bifriends -d bifriends -f /tmp/math_seed.sql
docker exec bifriends-db psql -U bifriends -d bifriends -f /tmp/korean_seed.sql
docker exec bifriends-db psql -U bifriends -d bifriends -f /tmp/shop_seed.sql

위 명령어로 콘텐츠 시드 실행

---

## 데모 회원 데이터 시드

시연용 계정(학습 이력, 리포트, 채팅, 풀 등)을 **로컬 Docker DB**에 채웁니다.

### 1. 사전 준비

```bash
docker-compose up -d db   # 또는 app+db
```

앱에서 **Google 로그인 1회** (members 행 생성)

### 2. 실행

**Windows (PowerShell)**

```powershell
cd bifriends-be
.\scripts\seed_demo.ps1 -Email "your@gmail.com"
```

**macOS / Linux**

```bash
cd bifriends-be
chmod +x scripts/seed_demo.sh
./scripts/seed_demo.sh your@gmail.com
```

같은 이메일로 **재실행 가능** (기존 데모 데이터 삭제 후 다시 채움).

> **팀원 전달용 상세 가이드:** [doc/DEMO_SETUP.md](../../doc/DEMO_SETUP.md) (macOS 기준)

### 3. 시드 후 상태

| 항목 | 값 |
|------|-----|
| 부모 모드 PIN | `1234` |
| available_pool | 30 |
| streak | 10일 |
| 학습 시도 (정답) | 22건 |
| 할 일 이력 | 14일 |
| 주간 리포트 | 2건 |
| 채팅 세션 | 3개 |

> **친구랑 과거 세션**은 Firestore(`mindSessions`)에 저장됩니다. PostgreSQL 시드와 별도로 아래 스크립트를 실행하세요.

```powershell
# Windows — PG 시드 후
.\scripts\seed_demo.ps1 -Email "your@gmail.com"
.\scripts\seed_mind_firestore.ps1 -Email "your@gmail.com"
```

```bash
# macOS/Linux
pip install -r scripts/requirements-seed.txt
python scripts/seed_mind_firestore.py --email your@gmail.com
```

Firestore DB ID는 docker-compose 기본값 `bifriends` 입니다. `(default)` DB를 쓰면 `--database-id "(default)"` 를 추가하세요.