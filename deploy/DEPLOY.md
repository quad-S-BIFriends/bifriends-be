# BiFriends 배포 가이드

## 인프라 현황

| 항목 | 값 |
|---|---|
| 클라우드 | GCP |
| VM 인스턴스 | `instance-template-20260621-20260621-055209` |
| 외부 IP | `34.50.31.222` |
| OS | Debian/Ubuntu (systemd 기반) |
| 도메인 등록처 | Namecheap |
| API 도메인 | `api.bifriends.study` |
| 웹 도메인 | `bifriends.study` (미래 프론트엔드용 예약) |

---

## 배포 구조

```
/app/
├── bifriends-be/                      ← git clone (Spring Boot)
│   └── deploy/                        ← 이 폴더 (배포 설정)
│       ├── docker-compose.yml
│       ├── nginx.conf
│       └── .env.example
└── bifriends-ai/                      ← git clone (FastAPI)

/etc/bifriends/
└── firebase-service-account.json      ← Firebase 키 (절대 커밋 금지)
```

### GitHub 레포
- 백엔드(이 레포): https://github.com/quad-S-BIFriends/bifriends-be
- AI: https://github.com/quad-S-BIFriends/bifriends-ai

### docker-compose.yml build context 설명
- `bifriends-be`: `../` → `/app/bifriends-be/` (이 레포 루트)
- `bifriends-ai`: `../../bifriends-ai` → `/app/bifriends-ai/`

### 서비스 포트
| 서비스 | 컨테이너 내부 | 호스트 노출 |
|---|---|---|
| PostgreSQL | 5432 | 미노출 (내부망만) |
| bifriends-be | 8080 | 8080 |
| bifriends-ai | 8001 | 미노출 (내부망만) |
| Nginx | 80/443 | 80/443 |

---

## VM 초기 설치 순서 (한 번만)

### 1. SSH 접속
```bash
gcloud compute ssh instance-template-20260621-20260621-055209
```

### 2. Docker 설치
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Nginx + Certbot 설치
```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
```

### 4. 레포 클론
```bash
sudo mkdir -p /app /etc/bifriends
cd /app
sudo git clone https://github.com/quad-S-BIFriends/bifriends-be
sudo git clone https://github.com/quad-S-BIFriends/bifriends-ai
```

### 5. .env 파일 작성
```bash
cd /app/bifriends-be/deploy
sudo cp .env.example .env
sudo nano .env    # 실제 값으로 채우기
```

비밀값 생성 명령어:
```bash
openssl rand -base64 64   # JWT_SECRET용
openssl rand -hex 32      # INTERNAL_SERVICE_TOKEN용
```

### 6. Firebase 서비스 계정 키 업로드

**로컬 PowerShell** (새 창):
```powershell
gcloud compute scp "C:\bifriends\bifriends-be\src\main\resources\firebase-service-account.json" `
  instance-template-20260621-20260621-055209:/tmp/firebase-service-account.json `
  --zone=<YOUR_ZONE>
```

VM SSH에서:
```bash
sudo mv /tmp/firebase-service-account.json /etc/bifriends/firebase-service-account.json
sudo chmod 600 /etc/bifriends/firebase-service-account.json
```

### 7. Nginx 설정
```bash
sudo cp /app/bifriends-be/deploy/nginx.conf /etc/nginx/sites-available/bifriends
sudo ln -s /etc/nginx/sites-available/bifriends /etc/nginx/sites-enabled/bifriends
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

### 8. SSL 인증서 발급
```bash
# DNS 전파 확인 (api.bifriends.study → 34.50.31.222 인지 확인)
nslookup api.bifriends.study

sudo certbot --nginx -d api.bifriends.study
# 이메일 입력 → 약관 동의(Y) → 리다이렉트 선택(2번)
```

### 9. 빌드 및 실행
```bash
cd /app/bifriends-be/deploy
sudo docker compose build        # 최초 빌드 (10~15분, Gradle 포함)
sudo docker compose up -d
sudo docker compose logs -f      # 로그 확인 (Ctrl+C로 종료)
```

---

## 배포 완료 확인

```bash
# 컨테이너 상태
sudo docker compose ps

# 헬스체크
curl https://api.bifriends.study/actuator/health

# 로그
sudo docker compose logs bifriends-be --tail=50
sudo docker compose logs bifriends-ai --tail=50
```

---

## 업데이트 배포 (코드 변경 후)

```bash
cd /app

# 코드 최신화
sudo git -C bifriends-be pull origin main
sudo git -C bifriends-ai pull origin main

# 재빌드
cd bifriends-be/deploy
sudo docker compose build bifriends-be   # BE만 변경 시
sudo docker compose up -d bifriends-be

# 전체 재시작
sudo docker compose build && sudo docker compose up -d
```

---

## 현재 진행 상황

| 단계 | 상태 |
|---|---|
| 코드 준비 (Flyway, prod 설정) | 완료 |
| GitHub 푸시 | 완료 |
| Namecheap DNS A 레코드 설정 | 완료 |
| GCP VM 생성 | 완료 |
| VM Docker/Nginx 설치 | 완료 |
| 레포 클론 (/app) | 완료 |
| .env 파일 작성 | 완료 |
| Firebase 키 업로드 | 완료 |
| SSL 인증서 발급 | 완료 |
| 첫 빌드 & 실행 | 완료 |
| CI/CD (GitHub Actions) | 완료 |

---

## CI/CD (GitHub Actions 자동 배포)

`main` 브랜치에 push하면 VM에 자동으로 배포됩니다.

### 워크플로우 위치
`.github/workflows/deploy.yml`

### 동작 흐름
```
push to main
  → GitHub Actions 실행
  → VM SSH 접속
  → git pull (bifriends-be)
  → docker compose build bifriends-be
  → docker compose up -d bifriends-be
```

### GitHub Secrets 설정 (최초 1회)

레포 Settings → Secrets and variables → Actions → New repository secret

| Secret 이름 | 값 |
|---|---|
| `GCP_HOST` | `34.50.31.222` |
| `GCP_USER` | `sunnybin04` |
| `GCP_SSH_KEY` | SSH 개인키 전체 내용 (`-----BEGIN OPENSSH PRIVATE KEY-----` 포함) |

로컬에서 개인키 클립보드 복사:
```powershell
Get-Content "$env:USERPROFILE\.ssh\gcp_key" | Set-Clipboard
```

### 배포 결과 확인
GitHub → Actions 탭에서 워크플로우 실행 결과 확인 가능.
실패 시 로그를 보고 VM에서 직접 원인 파악:
```bash
sudo docker compose logs bifriends-be --tail=50
```

---

## 주의사항

- `deploy/.env` 는 `.gitignore`에 추가해서 절대 커밋하지 않는다
- `/etc/bifriends/firebase-service-account.json` 도 절대 커밋 금지
- GCP 방화벽에서 포트 80, 443이 열려있어야 Certbot이 동작한다
- `docker compose build` 는 최초 실행 시 15분 이상 걸릴 수 있다
- DB 포트(5432)는 외부 미노출 — 컨테이너 내부망으로만 통신
