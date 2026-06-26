# GitHub Wiki 업로드 가이드

`wiki/` 폴더는 **GitHub Wiki용 Markdown 소스**입니다.  
레포 본문과 별도로 [Wiki 저장소](https://github.com/quad-S-BIFriends/bifriends-be/wiki)에 push합니다.

## 1. Wiki 활성화

GitHub → `bifriends-be` → **Settings** → **Features** → **Wikis** 체크

## 2. 최초 업로드 (Windows PowerShell)

```powershell
cd C:\bifriends
git clone https://github.com/quad-S-BIFriends/bifriends-be.wiki.git
Copy-Item -Path bifriends-be\wiki\* -Destination bifriends-be.wiki\ -Force
cd bifriends-be.wiki
git add .
git commit -m "docs(wiki): initial structure and troubleshooting"
git push origin master
```

> 기본 브랜치가 `main`이면 `git push origin main`

## 3. 이후 수정

1. `bifriends-be/wiki/*.md` 편집
2. `bifriends-be.wiki`에 복사 후 commit & push

또는 `scripts/push_wiki.ps1` 사용:

```powershell
cd C:\bifriends\bifriends-be
.\scripts\push_wiki.ps1 -WikiClonePath C:\bifriends\bifriends-be.wiki
```

## 4. UI/UX 권장 (깔끔한 Wiki)

| 요소 | 권장 |
|------|------|
| **Home** | 목차 표만 — 긴 본문 X |
| **_Sidebar.md** | 모든 페이지 링크 (이미 포함) |
| **페이지 길이** | 한 화면 스크롤 2~3회 이내, 길면 레포 `doc/` 링크 |
| **이슈** | 표 + 한 줄 요약, 상세는 GitHub Issue 링크 |
| **API** | Swagger URL만 강조, 중복 스펙 작성 X |
| **다이어그램** | Mermaid는 GitHub Wiki에서 렌더 안 될 수 있음 → 레포 `doc/presentation/*.mmd` 링크 |
| **팀 정보** | `팀-및-개발-기간.md`만 노션과 동기화 |

## 5. 페이지 목록

| 파일 | Wiki 제목 |
|------|-----------|
| `Home.md` | Home |
| `_Sidebar.md` | (사이드바) |
| `프로젝트-개요.md` | 프로젝트-개요 |
| `스택-및-아키텍처.md` | 스택-및-아키텍처 |
| `팀-및-개발-기간.md` | 팀-및-개발-기간 |
| `나의-역할.md` | 나의-역할 |
| `기술적-의사결정.md` | 기술적-의사결정 |
| `트러블슈팅-및-이슈.md` | 트러블슈팅-및-이슈 |
| `API-명세.md` | API-명세 |
| `환경-설정-및-규칙.md` | 환경-설정-및-규칙 |

Wiki 내부 링크: `[[페이지-이름]]`
