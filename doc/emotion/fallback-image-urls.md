# 친구랑 탭 — 폴백 이미지 URL 목록

> 대상: AI팀  
> 용도: 폴백 시나리오 생성 시 `image_url` 필드에 채울 고정 URL 참고 문서  
> 버킷: `gs://bifriends-5df72.firebasestorage.app`

---

## 사용 규칙

- **step1, step2**: AI가 폴백 시나리오 생성 시 해당 감정의 URL을 `image_url`에 직접 채워 반환
- **step3**: AI가 폴백 시나리오 생성 시 3컷 각각의 URL을 `image_url`에 채워 반환 (`image_b64` 없음)
- BE가 `/api/v1/ai/content/scenario` 요청 시 `fallback_urls` 필드로 동일한 URL 맵을 함께 전달하므로, 그 값을 우선 사용해도 됨

---

## 감정별 폴백 이미지 URL

### 고마움

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 (step1 `image_url`) | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep1.png?alt=media&token=407f1fd9-7e77-43b1-92a1-e33ee722fa30` |
| step2.png | 얼굴 클로즈업 (step2 `image_url`) | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep2.png?alt=media&token=d4041556-ec36-47bd-9f41-e8be9496e9ce` |
| step3-1.png | 3컷 만화 1컷 (step3 comic[0] `image_url`) | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-1.png?alt=media&token=ea0a0afb-3182-47c4-8944-f99bd68b9f1f` |
| step3-2.png | 3컷 만화 2컷 (step3 comic[1] `image_url`) | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-2.png?alt=media&token=60a98955-eba7-4691-a6b1-dbeda3061ec7` |
| step3-3.png | 3컷 만화 3컷 (step3 comic[2] `image_url`) | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep3-3.png?alt=media&token=27615cc5-40d6-4a24-877b-7b832f73c825` |

---

### 기쁨

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep1.png?alt=media&token=2d74e93c-76f9-4a3b-a8f0-2c50a6dd842f` |
| step2.png | 얼굴 클로즈업 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep2.png?alt=media&token=fb33c05c-84b9-46a1-bad4-e9412b7ea3be` |
| step3-1.png | 3컷 만화 1컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-1.png?alt=media&token=785bc9bb-da20-4174-87fc-5afa60f6f261` |
| step3-2.png | 3컷 만화 2컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-2.png?alt=media&token=fbe92c1e-7c54-431a-a36d-f208a4b58d42` |
| step3-3.png | 3컷 만화 3컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EA%B8%B0%EC%81%A8%2Fstep3-3.png?alt=media&token=2afc7b49-e9a0-4bd5-b4ac-644772ce579a` |

---

### 부끄러움

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EB%B6%80%EB%81%84%EB%9F%AC%EC%9B%80%2Fstep1.png?alt=media&token=9e150a5b-2477-4faf-be88-e1e29c7ce84b` |
| step2.png | 얼굴 클로즈업 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EB%B6%80%EB%81%84%EB%9F%AC%EC%9B%80%2Fstep2.png?alt=media&token=4c48d49b-5329-48cb-8025-6ded25b5721f` |
| step3-1.png | 3컷 만화 1컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EB%B6%80%EB%81%84%EB%9F%AC%EC%9B%80%2Fstep3-1.png?alt=media&token=ffca1997-8751-4cfe-b87a-602d17524da7` |
| step3-2.png | 3컷 만화 2컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EB%B6%80%EB%81%84%EB%9F%AC%EC%9B%80%2Fstep3-2.png?alt=media&token=687570ae-a1a9-426e-aef8-288a7fdaac06` |
| step3-3.png | 3컷 만화 3컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EB%B6%80%EB%81%84%EB%9F%AC%EC%9B%80%2Fstep3-3.png?alt=media&token=35de6034-c950-4428-84f0-9106d8dc07b1` |

---

### 속상함

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep1.png?alt=media&token=753c097d-5681-4dfd-9963-a34f17d1e3b6` |
| step2.png | 얼굴 클로즈업 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep2.png?alt=media&token=6d20985e-d20d-45bc-860e-45a1db97e877` |
| step3-1.png | 3컷 만화 1컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-1.png?alt=media&token=69f0c171-0a2b-4262-80f2-7a903e5e33cd` |
| step3-2.png | 3컷 만화 2컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-2.png?alt=media&token=77b773fd-ff83-4721-b041-fa932f01aa11` |
| step3-3.png | 3컷 만화 3컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%86%8D%EC%83%81%ED%95%A8%2Fstep3-3.png?alt=media&token=edc8df79-91e4-4a8a-9996-3ad7faa12e43` |

---

### 실망

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%8B%A4%EB%A7%9D%2Fstep1.png?alt=media&token=c68ff146-c6d7-4f4c-b8bb-1782864d4daa` |
| step2.png | 얼굴 클로즈업 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%8B%A4%EB%A7%9D%2Fstep2.png?alt=media&token=16ed716e-299e-4186-9993-d37c6a8bbca0` |
| step3-1.png | 3컷 만화 1컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%8B%A4%EB%A7%9D%2Fstep3-1.png?alt=media&token=bef1cf4f-6115-4723-b85e-657b648b9fac` |
| step3-2.png | 3컷 만화 2컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%8B%A4%EB%A7%9D%2Fstep3-2.png?alt=media&token=4bd7cb89-bf2c-4a22-844e-96750cc3dee2` |
| step3-3.png | 3컷 만화 3컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%EC%8B%A4%EB%A7%9D%2Fstep3-3.png?alt=media&token=3d10e6b3-a1af-4159-bd13-a20741c444b2` |

---

### 화남

| 파일 | 용도 | URL |
|---|---|---|
| step1.png | 상반신 이미지 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%ED%99%94%EB%82%A8%2Fstep1.png?alt=media&token=64edabd4-7d81-44b1-8b2a-92eb13509ef5` |
| step2.png | 얼굴 클로즈업 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%ED%99%94%EB%82%A8%2Fstep2.png?alt=media&token=5266db4a-b8f5-4adc-909f-91443137f704` |
| step3-1.png | 3컷 만화 1컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%ED%99%94%EB%82%A8%2Fstep3-1.png?alt=media&token=eae1e15c-14c7-4915-8f69-5096b04b5606` |
| step3-2.png | 3컷 만화 2컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%ED%99%94%EB%82%A8%2Fstep3-2.png?alt=media&token=1e6d3a6d-a79c-4a10-9197-e723a913e24c` |
| step3-3.png | 3컷 만화 3컷 | `https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app/o/fallback%2F%ED%99%94%EB%82%A8%2Fstep3-3.png?alt=media&token=f46fa6ff-7c78-4030-a63e-093ba1db6d14` |

---

## AI 요청 body에서 받는 fallback_urls 구조

BE는 `/api/v1/ai/content/scenario` 요청 시 아래 형식으로 `fallback_urls`를 전달한다.  
AI는 이 값을 그대로 활용하면 되므로, 위 URL을 별도로 하드코딩할 필요 없다.

```json
{
  "member_id": 1,
  "nickname": "서윤",
  "interests": ["DINOSAUR"],
  "learned_expressions": [],
  "emotion": null,
  "fallback_urls": {
    "고마움": {
      "step1":   "https://firebasestorage.googleapis.com/.../고마움/step1.png?alt=media&token=...",
      "step2":   "https://firebasestorage.googleapis.com/.../고마움/step2.png?alt=media&token=...",
      "step3-1": "https://firebasestorage.googleapis.com/.../고마움/step3-1.png?alt=media&token=...",
      "step3-2": "https://firebasestorage.googleapis.com/.../고마움/step3-2.png?alt=media&token=...",
      "step3-3": "https://firebasestorage.googleapis.com/.../고마움/step3-3.png?alt=media&token=..."
    },
    "기쁨": { ... },
    "부끄러움": { ... },
    "속상함": { ... },
    "실망": { ... },
    "화남": { ... }
  }
}
```
