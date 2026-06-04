/**
 * ERD.html §③ Firebase 상세 데이터
 * ERD.html과 같은 폴더에서 <script src="erd-firebase-enriched.js"> 로 로드
 */
window.ERD_FIREBASE = {
  firestore_users: {
    kind: "firestore",
    label: "Firestore users 루트",
    path: "users/{memberId}",
    purpose: "회원 한 명당 Firestore 최상위 문서 경로입니다. 실제 필드는 거의 없고, 하위 컬렉션 mindSessions만 씁니다.",
    role: "PostgreSQL members 테이블과 1:1로 대응하는 '폴더' 역할입니다. memberId는 members.id를 문자열로 바꾼 값(예: 1 → \"1\")입니다. BE는 이 경로 아래에만 감정 학습 데이터를 씁니다.",
    whenUsed: "로그인·회원 생성 시 PostgreSQL에만 저장되고, Firestore users 문서는 첫 mindSession 저장 때 경로가 생깁니다.",
    apis: [
      "직접 CRUD API 없음 — mind API가 내부적으로 경로만 사용",
      "GET /api/v1/members/me — 프로필은 PostgreSQL",
    ],
    flows: [
      "Google 로그인 → members INSERT (PostgreSQL)",
      "감정 학습 step4 완료 → users/{id}/mindSessions/{setId} 문서 생성",
    ],
    enums: [],
    relations: [
      { to: "members", target: "pg", label: "1:1", desc: "members.id = 문서 ID" },
      { to: "firestore_mind_sessions", label: "1:N", desc: "완료된 학습 세션" },
    ],
    columns: [
      { name: "(문서 ID)", type: "string", keys: ["PK"], desc: "members.id와 동일 숫자를 문자열로" },
      { name: "하위 컬렉션", type: "collection", keys: [], desc: "mindSessions — 실제 데이터 위치" },
    ],
    notes: [
      "Firestore에 'users 전체 목록' API는 없음 — memberId를 알 때만 접근",
      "환경변수 FIRESTORE_DATABASE_ID (기본 (default), 운영 예: bifriends)",
    ],
    entity: "infrastructure/firebase/FirestoreService.kt",
  },

  firestore_mind_sessions: {
    kind: "firestore",
    label: "mindSessions 문서",
    path: "users/{memberId}/mindSessions/{setId}",
    purpose: "친구랑 감정 학습을 끝까지 마친 세션만 저장하는 Firestore 문서입니다.",
    role: "아이의 '감정 학습 일기' 본문입니다. step1~4 화면 내용·이미지 URL·배운 표현이 들어갑니다. PostgreSQL에는 없고, 히스토리·다시보기·중복 표현 방지에 쓰입니다.",
    whenUsed: "아이가 step4까지 완료한 뒤 FE가 POST /api/v1/mind/sessions 호출 시에만 저장. POST /mind/scenario(시나리오 생성)만으로는 저장되지 않음.",
    apis: [
      "POST /api/v1/mind/sessions — 저장 + 풀 보상(+3)",
      "GET /api/v1/mind/sessions — 히스토리 목록 (completedAt 최신순)",
      "GET /api/v1/mind/sessions/{sessionId} — 다시보기",
      "POST /api/v1/mind/scenario — 생성만 (Firestore 미저장, Storage만 comic 업로드)",
    ],
    flows: [
      "POST /mind/scenario → AI 시나리오 + step3 comic PNG → Storage URL",
      "아이 step4 완료 → POST /mind/sessions → Firestore set + earnPool(EMOTION)",
      "다음 시나리오 생성 시 getLearnedExpressions()로 learnedExpression 중복 방지",
    ],
    enums: [
      {
        field: "emotion",
        type: "감정명 (문자열)",
        note: "AI·폴백 시나리오에서 사용하는 한글 감정 라벨",
        values: [
          { v: "기쁨", d: "fallback/기쁨/ 이미지 있음" },
          { v: "속상함", d: "" },
          { v: "부끄러움", d: "" },
          { v: "화남", d: "" },
          { v: "실망", d: "" },
          { v: "고마움", d: "" },
        ],
      },
      {
        field: "isFallback",
        type: "Boolean",
        values: [
          { v: "true", d: "AI 실패 시 미리 올려 둔 fallback/ 이미지 시나리오" },
          { v: "false", d: "AI가 만든 시나리오" },
        ],
      },
      {
        field: "HTTP (조회·저장 실패)",
        type: "API 응답",
        values: [
          { v: "200 + sessions: []", d: "저장된 완료 세션 없음 (정상)" },
          { v: "503", d: "Firestore 장애 — 빈 배열로 숨기지 않음" },
          { v: "404", d: "GET /sessions/{id} — 문서 없음" },
        ],
      },
    ],
    relations: [
      { to: "firestore_users", label: "N:1", desc: "상위 users 경로" },
      { to: "storage_mind_comic", label: "참조", desc: "step3.comic[].imageUrl" },
      { to: "storage_fallback", label: "참조", desc: "isFallback=true일 때" },
    ],
    columns: [
      { name: "setId", type: "string", keys: ["PK"], desc: "문서 ID = AI가 준 세트 ID" },
      { name: "emotion", type: "string", keys: [], desc: "감정명" },
      { name: "situation", type: "string", keys: [], desc: "상황 설명" },
      { name: "learnedExpression", type: "string", keys: [], desc: "배운 표현 (중복 방지 키)" },
      { name: "isFallback", type: "boolean", keys: [], desc: "폴백 시나리오 여부" },
      { name: "completedAt", type: "string", keys: ["IDX"], desc: "ISO-8601 UTC, 목록 정렬용" },
      { name: "step1", type: "map", keys: [], desc: "오늘의 표현 — title, expression, imageUrl 등" },
      { name: "step2", type: "map", keys: [], desc: "어떤 기분일까요 — choices[], imageUrl" },
      { name: "step3", type: "map", keys: [], desc: "3컷 만화 — comic[{cut,text,imageUrl}], choices[]" },
      { name: "step4", type: "map", keys: [], desc: "이렇게 말하고 싶어 — choices[], reward" },
    ],
    notes: [
      "image_b64는 저장하지 않음 — imageUrl(Storage HTTPS)만 저장",
      "completedAt 단일 필드 인덱스(내림차순) 권장, 없으면 서버 메모리 정렬 fallback",
      "레거시 POST /api/v1/emotion/scenarios 는 생성+저장+보상 일괄 (신규 mind API와 분리)",
    ],
    entity: "domain/mind/service/MindSessionService.kt",
  },

  storage_fallback: {
    kind: "storage",
    label: "fallback 감정 이미지",
    path: "fallback/{감정명}/step1.png … step3-3.png",
    purpose: "AI 시나리오 생성이 실패했을 때 쓰는 고정 이미지 세트입니다.",
    role: "감정별로 미리 올려 둔 '비상용 슬라이드·만화'입니다. BE가 런타임에 업로드하지 않고, AI 요청 시 fallback_urls 맵으로 URL을 넘깁니다.",
    whenUsed: "POST /mind/scenario 또는 레거시 emotion API에서 AI가 폴백 응답을 줄 때 step1~3 imageUrl로 사용.",
    apis: [
      "POST /api/v1/mind/scenario — AI 요청 body에 fallback_urls 포함",
      "GET (없음) — URL은 emotion/fallback-image-urls.md 또는 AI 응답 참고",
    ],
    flows: [
      "운영자가 Firebase Console 또는 스크립트로 감정 폴더별 PNG 사전 업로드",
      "BE → AI /content/scenario 요청 시 감정별 URL 맵 전달",
      "isFallback=true 세션 저장 시 step imageUrl이 이 경로를 가리킴",
    ],
    enums: [
      {
        field: "감정 폴더명",
        type: "Storage prefix",
        values: [
          { v: "기쁨", d: "step1.png, step2.png, step3-1~3.png" },
          { v: "고마움", d: "동일 파일명 패턴" },
          { v: "부끄러움", d: "" },
          { v: "속상함", d: "" },
          { v: "실망", d: "" },
          { v: "화남", d: "" },
        ],
      },
      {
        field: "파일 패턴",
        type: "object name",
        values: [
          { v: "step1.png", d: "step1 상반신" },
          { v: "step2.png", d: "step2 얼굴 클로즈업" },
          { v: "step3-1.png ~ step3-3.png", d: "3컷 만화" },
        ],
      },
    ],
    relations: [{ to: "firestore_mind_sessions", label: "참조", desc: "isFallback 세션의 imageUrl" }],
    columns: [
      { name: "bucket", type: "config", keys: [], desc: "FIREBASE_STORAGE_BUCKET" },
      { name: "URL 형식", type: "https", keys: [], desc: ".../o/fallback%2F{감정}%2Fstep1.png?alt=media&token=..." },
      { name: "BE 업로드", type: "—", keys: [], desc: "❌ 사전 업로드만" },
    ],
    notes: ["상세 URL 목록: doc/emotion/fallback-image-urls.md"],
    entity: "infrastructure/firebase/FirebaseStorageService.kt",
  },

  storage_mind_comic: {
    kind: "storage",
    label: "mindSessions 만화 컷",
    path: "mindSessions/{setId}/comic/{uuid}.png",
    purpose: "AI가 생성한 step3 3컷 만화 이미지를 저장하는 경로입니다.",
    role: "세션마다 새로 생기는 동적 파일입니다. Firestore step3.comic[].imageUrl에 HTTPS URL이 들어갑니다.",
    whenUsed: "POST /api/v1/mind/scenario 성공 시 BE가 AI의 base64 이미지를 디코딩해 업로드.",
    apis: ["POST /api/v1/mind/scenario — comic 컷마다 업로드"],
    flows: [
      "AI 응답 image_b64 → FirebaseStorageService.uploadBase64Png()",
      "반환 URL → FE가 step3에 표시 → 완료 시 POST /sessions에 URL 포함 저장",
    ],
    enums: [],
    relations: [
      { to: "firestore_mind_sessions", label: "참조", desc: "step3.comic[].imageUrl" },
    ],
    columns: [
      { name: "setId", type: "path segment", keys: [], desc: "AI setId와 동일" },
      { name: "comic/{uuid}.png", type: "object", keys: [], desc: "컷당 1파일, uuid는 BE 생성" },
      { name: "download token", type: "uuid", keys: [], desc: "URL query token=..." },
    ],
    notes: [
      "폴백 시나리오는 이 경로 대신 fallback/ 사용",
      "탈퇴 시 Storage 객체 일괄 삭제는 현재 WithdrawalService 범위 밖 (PostgreSQL만 삭제)",
    ],
    entity: "domain/mind/service/MindScenarioService.kt",
  },

  firebase_auth: {
    kind: "auth",
    label: "Firebase Auth (로그인)",
    path: "(Firebase Auth 프로젝트)",
    purpose: "Google 로그인 시 FE가 받은 Firebase id_token을 백엔드가 검증합니다.",
    role: "PostgreSQL members 행을 만들기 위한 '신원 확인' 단계입니다. 회원 프로필·풀·학습 데이터는 PostgreSQL + Firestore에 있고, Auth에는 계정 메타만 있습니다.",
    whenUsed: "POST /api/v1/members/auth/firebase — id_token 검증 후 JWT 발급.",
    apis: ["POST /api/v1/members/auth/firebase"],
    flows: [
      "FE Google 로그인 → Firebase id_token",
      "BE FirebaseTokenVerifier.verifyIdToken()",
      "email/provider_id로 members upsert → 앱 JWT 반환",
    ],
    enums: [],
    relations: [{ to: "members", target: "pg", label: "→", desc: "검증 후 PostgreSQL 회원 생성·조회" }],
    columns: [
      { name: "id_token", type: "JWT", keys: [], desc: "FE → BE 1회성" },
      { name: "firebase-service-account.json", type: "file", keys: [], desc: "Admin SDK (Git 커밋 금지)" },
      { name: "FIREBASE_CONFIG_PATH", type: "env", keys: [], desc: "서비스 계정 경로" },
    ],
    notes: [
      "프로필 사진 URL은 주로 Google → members.profile_image_url (PostgreSQL)",
      "Docker: firebase-service-account.json 마운트 필수",
    ],
    entity: "infrastructure/security/FirebaseTokenVerifier.kt",
  },
};
