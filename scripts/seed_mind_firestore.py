#!/usr/bin/env python3
"""
BiFriends 친구랑(mindSessions) Firestore 데모 시드

Firestore 경로: users/{memberId}/mindSessions/{setId}

사용:
  pip install -r scripts/requirements-seed.txt
  python scripts/seed_mind_firestore.py --email your@gmail.com

전제:
  - firebase-service-account.json (BE와 동일)
  - 앱에서 Google 로그인 1회 (members.id = Firestore users/{id})
  - (선택) docker-compose up -d db  → email로 member_id 자동 조회

주의:
  - POST /api/v1/mind/sessions 대신 Firestore에 직접 씁니다 (풀 보상 중복 방지).
  - setId가 demo-mind-* 인 문서만 삭제 후 재생성 (멱등).
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("firebase-admin 이 필요합니다: pip install -r scripts/requirements-seed.txt", file=sys.stderr)
    sys.exit(1)

# doc/emotion/fallback-image-urls.md — Firebase Storage 폴백 URL
FALLBACK_URLS: dict[str, dict[str, str]] = {
    "기쁨": {
        "step1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep1.png?alt=media&token=2d74e93c-76f9-4a3b-a8f0-2c50a6dd842f",
        "step2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep2.png?alt=media&token=fb33c05c-84b9-46a1-bad4-e9412b7ea3be",
        "step3-1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-1.png?alt=media&token=785bc9bb-da20-4174-87fc-5afa60f6f261",
        "step3-2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-2.png?alt=media&token=fbe92c1e-7c54-431a-a36d-f208a4b58d42",
        "step3-3": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-3.png?alt=media&token=2afc7b49-e9a0-4bd5-b4ac-644772ce579a",
    },
    "고마움": {
        "step1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep1.png?alt=media&token=407f1fd9-7e77-43b1-92a1-e33ee722fa30",
        "step2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep2.png?alt=media&token=d4041556-ec36-47bd-9f41-e8be9496e9ce",
        "step3-1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-1.png?alt=media&token=ea0a0afb-3182-47c4-8944-f99bd68b9f1f",
        "step3-2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-2.png?alt=media&token=60a98955-eba7-4691-a6b1-dbeda3061ec7",
        "step3-3": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-3.png?alt=media&token=27615cc5-40d6-4a24-877b-7b832f73c825",
    },
    "속상함": {
        "step1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep1.png?alt=media&token=753c097d-5681-4dfd-9963-a34f17d1e3b6",
        "step2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep2.png?alt=media&token=6d20985e-d20d-45bc-860e-45a1db97e877",
        "step3-1": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-1.png?alt=media&token=69f0c171-0a2b-4262-80f2-7a903e5e33cd",
        "step3-2": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-2.png?alt=media&token=77b773fd-ff83-4721-b041-fa932f01aa11",
        "step3-3": "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-3.png?alt=media&token=edc8df79-91e4-4a8a-9996-3ad7faa12e43",
    },
}

DEMO_SET_IDS = ("demo-mind-001", "demo-mind-002", "demo-mind-003")

DEMO_SESSIONS = (
    {
        "set_id": "demo-mind-001",
        "emotion": "기쁨",
        "situation": "친구가 나를 칭찬해줬어요",
        "learned_expression": "기뻐",
        "days_ago": 8,
    },
    {
        "set_id": "demo-mind-002",
        "emotion": "고마움",
        "situation": "선생님이 도와주셨어요",
        "learned_expression": "고마워",
        "days_ago": 4,
    },
    {
        "set_id": "demo-mind-003",
        "emotion": "속상함",
        "situation": "친구와 다퉜어요",
        "learned_expression": "속상해",
        "days_ago": 2,
    },
)


def resolve_member_id(email: str | None, member_id: int | None, docker_container: str) -> int:
    if member_id is not None:
        return member_id
    if not email:
        raise SystemExit("--email 또는 --member-id 가 필요합니다.")

    sql = f"SELECT id FROM members WHERE email = '{email.replace(chr(39), chr(39)+chr(39))}' LIMIT 1;"
    cmd = [
        "docker", "exec", docker_container,
        "psql", "-U", "bifriends", "-d", "bifriends",
        "-t", "-A", "-c", sql,
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
    except FileNotFoundError:
        raise SystemExit("docker 명령을 찾을 수 없습니다. --member-id 로 직접 지정하세요.")
    except subprocess.CalledProcessError as e:
        raise SystemExit(f"PostgreSQL 조회 실패: {e.stderr or e.stdout}")

    raw = result.stdout.strip()
    if not raw:
        raise SystemExit(
            f"members 테이블에 email={email} 가 없습니다. 앱에서 Google 로그인 후 다시 실행하세요."
        )
    return int(raw)


def build_session_doc(
    set_id: str,
    emotion: str,
    situation: str,
    learned_expression: str,
    days_ago: int,
) -> dict:
    urls = FALLBACK_URLS[emotion]
    completed_at = (datetime.now(timezone.utc) - timedelta(days=days_ago)).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    return {
        "setId": set_id,
        "emotion": emotion,
        "situation": situation,
        "learnedExpression": learned_expression,
        "isFallback": True,
        "completedAt": completed_at,
        "step1": {
            "title": "오늘의 표현",
            "expression": learned_expression,
            "emotion": emotion,
            "bodySensation": "가슴이 두근거려요" if emotion == "기쁨" else "마음이 무거워요",
            "situationExample": situation,
            "imageUrl": urls["step1"],
            "nextButtonText": "다음",
        },
        "step2": {
            "title": "어떤 기분일까요?",
            "visualClue": f"{emotion} 표정",
            "question": "어떤 기분일까요?",
            "imageUrl": urls["step2"],
            "retryMessage": "다시 골라볼까요?",
            "nextButtonText": "다음",
            "choices": [
                {
                    "id": "a",
                    "text": f"{emotion}인 표정",
                    "isCorrect": True,
                    "feedback": "맞아요!",
                },
                {
                    "id": "b",
                    "text": "화난 표정",
                    "isCorrect": False,
                    "feedback": "다시 골라볼까요?",
                },
            ],
        },
        "step3": {
            "title": "3컷 만화",
            "question": "무슨 일이 있었을까요?",
            "retryMessage": "다시 골라볼까요?",
            "nextButtonText": "다음",
            "comic": [
                {"cut": 1, "text": situation, "imageUrl": urls["step3-1"]},
                {"cut": 2, "text": f"'{learned_expression}'라고 말하고 싶어요", "imageUrl": urls["step3-2"]},
                {"cut": 3, "text": "레오가 함께 연습해요", "imageUrl": urls["step3-3"]},
            ],
            "choices": [
                {
                    "id": "a",
                    "text": situation[:20],
                    "isCorrect": True,
                    "feedback": "맞아요!",
                },
            ],
        },
        "step4": {
            "title": "이렇게 말하고 싶어!",
            "leoIntro": "레오가 도와줄게",
            "question": "어떻게 말할까요?",
            "retryMessage": "다시 골라볼까요?",
            "successMessage": "잘했어요!",
            "completeButtonText": "완료",
            "reward": {"type": "POOL", "amount": 3},
            "choices": [
                {
                    "id": "a",
                    "text": learned_expression,
                    "type": "expression",
                    "isCorrect": True,
                    "feedback": "완벽해요!",
                },
            ],
        },
    }


def init_firestore(service_account: Path, database_id: str):
    if not service_account.is_file():
        raise SystemExit(f"서비스 계정 파일 없음: {service_account}")

    if not firebase_admin._apps:
        cred = credentials.Certificate(str(service_account))
        firebase_admin.initialize_app(cred)

    if database_id == "(default)":
        return firestore.client()
    return firestore.client(database_id=database_id)


def seed(member_id: int, db, dry_run: bool) -> None:
    col = db.collection("users").document(str(member_id)).collection("mindSessions")

    for set_id in DEMO_SET_IDS:
        if dry_run:
            print(f"[dry-run] delete users/{member_id}/mindSessions/{set_id}")
        else:
            col.document(set_id).delete()

    for spec in DEMO_SESSIONS:
        doc = build_session_doc(**{k: spec[k] for k in ("set_id", "emotion", "situation", "learned_expression", "days_ago")})
        path = f"users/{member_id}/mindSessions/{spec['set_id']}"
        if dry_run:
            print(f"[dry-run] set {path} completedAt={doc['completedAt']}")
        else:
            col.document(spec["set_id"]).set(doc)
            print(f"  saved {path} ({doc['emotion']}, {doc['learnedExpression']})")


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    default_sa = root / "src" / "main" / "resources" / "firebase-service-account.json"

    parser = argparse.ArgumentParser(description="BiFriends mindSessions Firestore demo seed")
    parser.add_argument("--email", help="Google login email (looks up member_id via Docker PG)")
    parser.add_argument("--member-id", type=int, help="members.id (skip PG lookup)")
    parser.add_argument(
        "--service-account",
        type=Path,
        default=default_sa,
        help=f"Firebase service account JSON (default: {default_sa})",
    )
    parser.add_argument(
        "--database-id",
        default=None,
        help="Firestore database ID (default: env FIRESTORE_DATABASE_ID or 'bifriends')",
    )
    parser.add_argument(
        "--docker-container",
        default="bifriends-db",
        help="PostgreSQL Docker container name",
    )
    parser.add_argument("--dry-run", action="store_true", help="Print actions without writing")
    args = parser.parse_args()

    import os
    database_id = args.database_id or os.environ.get("FIRESTORE_DATABASE_ID", "bifriends")
    member_id = resolve_member_id(args.email, args.member_id, args.docker_container)

    print(f">> Firestore mindSessions seed - member_id={member_id}, database={database_id}")

    db = init_firestore(args.service_account, database_id)
    seed(member_id, db, args.dry_run)

    if not args.dry_run:
        print("")
        print("Done! Verify with:")
        print(f"  GET /api/v1/mind/sessions  (JWT required)")
        print(f"  Firebase Console → users/{member_id}/mindSessions")


if __name__ == "__main__":
    main()
