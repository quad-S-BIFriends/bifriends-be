-- BiFriends 데모용 회원 데이터 시드
-- scripts/seed_demo.ps1 / seed_demo.sh 가 __DEMO_EMAIL__ 을 치환한 뒤 실행합니다.
--
-- 전제: 해당 Google 이메일로 앱에서 1회 이상 로그인한 상태 (members 행 존재)
-- 멱등: 같은 이메일로 재실행 시 기존 데모 데이터를 삭제 후 다시 채움

\set ON_ERROR_STOP on

DO $seed$
DECLARE
    v_email              TEXT := '__DEMO_EMAIL__';
    v_member_id          BIGINT;
    v_nickname           TEXT;
    v_today              DATE := (NOW() AT TIME ZONE 'Asia/Seoul')::DATE;
    v_this_week_start    DATE;
    v_this_week_end      DATE;
    v_last_week_start    DATE;
    v_last_week_end      DATE;
    v_day                DATE;
    v_i                  INT;
    v_learning_type      TEXT;
    v_math_step1_id      BIGINT;
    v_math_step2_id      BIGINT;
    v_math_concept1      TEXT;
    v_math_concept2      TEXT;
    v_korean_step1_id    BIGINT;
    v_korean_step2_id    BIGINT;
    v_korean_concept1    TEXT;
    v_korean_concept2    TEXT;
    v_math_progress1_id  BIGINT;
    v_math_progress2_id  BIGINT;
    v_korean_progress1_id BIGINT;
    v_korean_progress2_id BIGINT;
    v_session1_id        BIGINT;
    v_session2_id        BIGINT;
    v_session3_id        BIGINT;
    v_gift3_shop_id      BIGINT;
    v_parent_pin_hash    TEXT := '$2b$10$racwlTFPROeVhtoBu2TuY.uZ6YsCIgQ0q2qw6AZ/UIYGG7uy/BNoi';
    v_sections_last_week TEXT;
    v_sections_this_week TEXT;
BEGIN
    -- ── 회원 조회 ────────────────────────────────────────────────────────
    SELECT id, COALESCE(nickname, '민준')
      INTO v_member_id, v_nickname
      FROM members
     WHERE email = v_email;

    IF v_member_id IS NULL THEN
        RAISE EXCEPTION 'members 테이블에 email=% 가 없습니다. 앱에서 Google 로그인 후 다시 실행하세요.', v_email;
    END IF;

    v_this_week_start := v_today - (EXTRACT(ISODOW FROM v_today)::INT - 1);
    v_this_week_end   := v_this_week_start + 6;
    v_last_week_start := v_this_week_start - 7;
    v_last_week_end   := v_this_week_start - 1;

    RAISE NOTICE '데모 시드 시작 — member_id=%, email=%, nickname=%', v_member_id, v_email, v_nickname;

    -- ── 기존 데모 데이터 정리 (FK 순서) ───────────────────────────────────
    DELETE FROM chat_messages WHERE member_id = v_member_id;
    DELETE FROM chat_sessions WHERE member_id = v_member_id;
    DELETE FROM learning_attempt WHERE member_id = v_member_id;
    DELETE FROM user_math_progress_cycles
     WHERE progress_id IN (SELECT id FROM user_math_progress WHERE member_id = v_member_id);
    DELETE FROM user_korean_progress_cycles
     WHERE progress_id IN (SELECT id FROM user_korean_progress WHERE member_id = v_member_id);
    DELETE FROM user_math_progress WHERE member_id = v_member_id;
    DELETE FROM user_korean_progress WHERE member_id = v_member_id;
    DELETE FROM reward_history WHERE member_id = v_member_id;
    DELETE FROM todos WHERE member_id = v_member_id;
    DELETE FROM weekly_report WHERE member_id = v_member_id;
    DELETE FROM weekly_safety_report WHERE member_id = v_member_id;
    DELETE FROM member_shop_items WHERE member_id = v_member_id;

    -- ── 회원 프로필 · 온보딩 완료 상태 ───────────────────────────────────
    UPDATE members
       SET nickname               = COALESCE(nickname, '민준'),
           grade                  = COALESCE(grade, 4),
           guardian_phone         = COALESCE(guardian_phone, '01012345678'),
           onboarding_completed   = TRUE,
           terms_agreed           = TRUE,
           privacy_agreed         = TRUE,
           marketing_agreed       = COALESCE(marketing_agreed, FALSE),
           terms_agreed_at        = COALESCE(terms_agreed_at, NOW()),
           notification_enabled   = TRUE,
           microphone_enabled     = TRUE,
           parent_password        = v_parent_pin_hash,
           equipped_outfit_code   = 'GIFT_3',
           representative_item_type = COALESCE(representative_item_type, 'GIFT_3')
     WHERE id = v_member_id;

    SELECT nickname INTO v_nickname FROM members WHERE id = v_member_id;

    -- ── 관심사 · 온보딩 선물 ─────────────────────────────────────────────
    IF NOT EXISTS (SELECT 1 FROM member_interests WHERE member_id = v_member_id) THEN
        INSERT INTO member_interests (member_id, interest) VALUES
            (v_member_id, 'ANIMAL'),
            (v_member_id, 'GAME'),
            (v_member_id, 'SCIENCE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM member_items WHERE member_id = v_member_id) THEN
        INSERT INTO member_items (member_id, item_type, acquired_at)
        VALUES (v_member_id, 'GIFT_3', NOW() - INTERVAL '14 days');
    END IF;

    -- ── user_stats (풀 25+, streak 7+) ───────────────────────────────────
    INSERT INTO user_stats (
        member_id, level, total_pool_earned, available_pool,
        streak_days, last_attendance_date, created_at, updated_at
    ) VALUES (
        v_member_id, 5, 85, 30,
        10, v_today, NOW() - INTERVAL '14 days', NOW()
    )
    ON CONFLICT (member_id) DO UPDATE SET
        level                = 5,
        total_pool_earned    = 85,
        available_pool       = 30,
        streak_days          = 10,
        last_attendance_date = v_today,
        updated_at           = NOW();

    -- ── 학습 스텝 ID 조회 (4학년 기준) ───────────────────────────────────
    SELECT id, concept INTO v_math_step1_id, v_math_concept1
      FROM math_step WHERE grade = 4 AND step_number = 1;
    SELECT id, concept INTO v_math_step2_id, v_math_concept2
      FROM math_step WHERE grade = 4 AND step_number = 2;
    SELECT id, concept INTO v_korean_step1_id, v_korean_concept1
      FROM korean_step WHERE grade = 4 AND step_number = 1;
    SELECT id, concept INTO v_korean_step2_id, v_korean_concept2
      FROM korean_step WHERE grade = 4 AND step_number = 2;

    IF v_math_step1_id IS NULL OR v_korean_step1_id IS NULL THEN
        RAISE EXCEPTION 'math_step / korean_step 시드가 없습니다. docker-compose 로 BE를 한 번 기동해 주세요.';
    END IF;

    -- ── 14일치 할 일 (과거는 COMPLETED, 오늘은 2/3 완료) ─────────────────
    FOR v_i IN 0..13 LOOP
        v_day := v_today - v_i;
        v_learning_type := CASE EXTRACT(ISODOW FROM v_day)::INT
            WHEN 1 THEN 'MATH'
            WHEN 2 THEN 'LANGUAGE'
            WHEN 3 THEN 'MATH'
            WHEN 4 THEN 'LANGUAGE'
            WHEN 5 THEN 'MATH'
            WHEN 6 THEN 'LANGUAGE'
            ELSE NULL
        END;

        INSERT INTO todos (member_id, type, title, status, source, learning_type, assigned_date, completed_at, created_at, updated_at)
        VALUES
            (v_member_id, 'CHAT', '레오랑 이야기하기 🗣️', 'COMPLETED', 'SYSTEM', NULL, v_day,
             v_day + TIME '19:00', v_day + TIME '09:00', v_day + TIME '19:00'),
            (v_member_id, 'LEARNING', '오늘의 문제 3개 풀기 📚',
             CASE WHEN v_i = 0 THEN 'PENDING' ELSE 'COMPLETED' END,
             'SYSTEM', v_learning_type::TEXT, v_day,
             CASE WHEN v_i = 0 THEN NULL ELSE v_day + TIME '18:00' END,
             v_day + TIME '09:00', v_day + TIME '18:00'),
            (v_member_id, 'EMOTION', '친구 기분 알아보기 💌',
             CASE WHEN v_i <= 1 THEN 'PENDING' ELSE 'COMPLETED' END,
             'SYSTEM', NULL, v_day,
             CASE WHEN v_i <= 1 THEN NULL ELSE v_day + TIME '20:00' END,
             v_day + TIME '09:00', v_day + TIME '20:00');
    END LOOP;

    -- ── 학습 진도 ────────────────────────────────────────────────────────
    INSERT INTO user_math_progress (member_id, math_step_id, is_step_completed, last_accessed_at)
    VALUES (v_member_id, v_math_step1_id, FALSE, v_today - 2)
    RETURNING id INTO v_math_progress1_id;

    INSERT INTO user_math_progress_cycles (progress_id, cycle_number) VALUES
        (v_math_progress1_id, 1),
        (v_math_progress1_id, 2),
        (v_math_progress1_id, 3);

    INSERT INTO user_math_progress (member_id, math_step_id, is_step_completed, last_accessed_at)
    VALUES (v_member_id, v_math_step2_id, FALSE, v_today - 1)
    RETURNING id INTO v_math_progress2_id;

    INSERT INTO user_math_progress_cycles (progress_id, cycle_number) VALUES
        (v_math_progress2_id, 1);

    INSERT INTO user_korean_progress (member_id, korean_step_id, is_step_completed, last_accessed_at)
    VALUES (v_member_id, v_korean_step1_id, TRUE, v_today - 3)
    RETURNING id INTO v_korean_progress1_id;

    INSERT INTO user_korean_progress_cycles (progress_id, cycle_number) VALUES
        (v_korean_progress1_id, 1),
        (v_korean_progress1_id, 2),
        (v_korean_progress1_id, 3),
        (v_korean_progress1_id, 4),
        (v_korean_progress1_id, 5);

    INSERT INTO user_korean_progress (member_id, korean_step_id, is_step_completed, last_accessed_at)
    VALUES (v_member_id, v_korean_step2_id, FALSE, v_today - 4)
    RETURNING id INTO v_korean_progress2_id;

    INSERT INTO user_korean_progress_cycles (progress_id, cycle_number) VALUES
        (v_korean_progress2_id, 1),
        (v_korean_progress2_id, 2);

    -- ── 문제 풀이 22건 (solved=true) ─────────────────────────────────────
    INSERT INTO learning_attempt (
        member_id, subject, concept, step_id, cycle_number, question_index,
        attempts, hints_used, solved, solved_at, created_at, updated_at
    )
    SELECT
        v_member_id,
        'MATH',
        v_math_concept1,
        v_math_step1_id,
        gs.cycle_num,
        gs.q_idx,
        1 + (gs.n % 2),
        gs.n % 3,
        TRUE,
        (v_today - (gs.n % 10)) + TIME '17:30',
        (v_today - (gs.n % 10)) + TIME '17:00',
        (v_today - (gs.n % 10)) + TIME '17:30'
    FROM (
        SELECT n,
               2 + ((n - 1) / 3) AS cycle_num,
               ((n - 1) % 3) + 1 AS q_idx
        FROM generate_series(1, 12) AS n
    ) gs;

    INSERT INTO learning_attempt (
        member_id, subject, concept, step_id, cycle_number, question_index,
        attempts, hints_used, solved, solved_at, created_at, updated_at
    )
    SELECT
        v_member_id,
        'KOREAN',
        v_korean_concept1,
        v_korean_step1_id,
        gs.cycle_num,
        gs.q_idx,
        1,
        gs.n % 2,
        TRUE,
        (v_today - (gs.n % 8)) + TIME '16:00',
        (v_today - (gs.n % 8)) + TIME '15:30',
        (v_today - (gs.n % 8)) + TIME '16:00'
    FROM (
        SELECT n,
               2 + ((n - 1) / 3) AS cycle_num,
               ((n - 1) % 3) + 1 AS q_idx
        FROM generate_series(1, 10) AS n
    ) gs;

    -- ── 풀 보상 이력 ───────────────────────────────────────────────────────
    INSERT INTO reward_history (member_id, source, amount, ref_id, created_at)
    SELECT v_member_id, src, amt, NULL, ts
    FROM (VALUES
        ('ATTENDANCE'::TEXT,             5,  v_today + TIME '09:05'),
        ('LEARNING_CORRECT'::TEXT,       1,  v_today - 1 + TIME '17:10'),
        ('LEARNING_CORRECT'::TEXT,       1,  v_today - 1 + TIME '17:15'),
        ('LEARNING_CORRECT'::TEXT,       1,  v_today - 1 + TIME '17:20'),
        ('LEARNING_SET_COMPLETE'::TEXT,  2,  v_today - 1 + TIME '17:25'),
        ('EMOTION'::TEXT,                3,  v_today - 2 + TIME '20:00'),
        ('TODO_SINGLE'::TEXT,            1,  v_today - 1 + TIME '19:00'),
        ('TODO_ALL_COMPLETE'::TEXT,      3,  v_today - 1 + TIME '20:30'),
        ('LEARNING_CORRECT'::TEXT,       1,  v_today - 3 + TIME '18:00'),
        ('LEARNING_SET_COMPLETE'::TEXT,  2,  v_today - 3 + TIME '18:10')
    ) AS t(src, amt, ts);

    -- ── 채팅 세션 · 메시지 (과거 3개) ────────────────────────────────────
    INSERT INTO chat_sessions (session_key, member_id, title, status, created_at, updated_at)
    VALUES ('demo-session-001', v_member_id, '안녕 레오!', 'CLOSED', v_today - 1 + TIME '19:00', v_today - 1 + TIME '19:15')
    RETURNING id INTO v_session1_id;

    INSERT INTO chat_sessions (session_key, member_id, title, status, created_at, updated_at)
    VALUES ('demo-session-002', v_member_id, '수학이 어려워', 'CLOSED', v_today - 4 + TIME '18:00', v_today - 4 + TIME '18:20')
    RETURNING id INTO v_session2_id;

    INSERT INTO chat_sessions (session_key, member_id, title, status, created_at, updated_at)
    VALUES ('demo-session-003', v_member_id, '내일 뭐 하지?', 'CLOSED', v_today - 8 + TIME '20:00', v_today - 8 + TIME '20:10')
    RETURNING id INTO v_session3_id;

    INSERT INTO chat_messages (session_id, member_id, role, content, created_at) VALUES
        (v_session1_id, v_member_id, 'USER', '안녕 레오!', v_today - 1 + TIME '19:00'),
        (v_session1_id, v_member_id, 'ASSISTANT', '안녕 ' || v_nickname || '! 오늘 하루는 어땠어? 😊', v_today - 1 + TIME '19:01'),
        (v_session1_id, v_member_id, 'USER', '학교에서 친구랑 놀았어!', v_today - 1 + TIME '19:02'),
        (v_session1_id, v_member_id, 'ASSISTANT', '와 정말 재미있었겠다! 내일은 뭐 하고 싶어?', v_today - 1 + TIME '19:03'),
        (v_session2_id, v_member_id, 'USER', '수학 문제가 너무 어려워', v_today - 4 + TIME '18:00'),
        (v_session2_id, v_member_id, 'ASSISTANT', '어려운 문제도 천천히 풀면 괜찮아! 같이 연습해 볼까?', v_today - 4 + TIME '18:01'),
        (v_session3_id, v_member_id, 'USER', '내일 뭐 하면 좋을까?', v_today - 8 + TIME '20:00'),
        (v_session3_id, v_member_id, 'ASSISTANT', '오늘 배운 표현을 가족에게 써 보는 건 어때?', v_today - 8 + TIME '20:01');

    -- ── 상점 구매 1건 (선물 외) ──────────────────────────────────────────
    SELECT id INTO v_gift3_shop_id FROM shop_items WHERE item_code = 'GIFT_1';
    IF v_gift3_shop_id IS NOT NULL THEN
        INSERT INTO member_shop_items (member_id, shop_item_id, acquired_at)
        VALUES (v_member_id, v_gift3_shop_id, v_today - 5 + TIME '12:00');
    END IF;

    -- ── 주간 리포트 JSON ───────────────────────────────────────────────────
    v_sections_last_week := format(
        '{"growth_summary":"%s님은 지난주에도 꾸준히 학습에 참여했어요. 수학과 국어 모두 스스로 문제를 풀어보려는 태도가 좋았습니다.","math":{"well_done":"세 자리 수 덧셈·뺄셈 문제를 차분히 풀었어요.","struggled":"받아올림이 있는 문제에서 실수가 조금 있었어요."},"korean":{"well_done":"맞춤법 문제를 정확하게 풀었어요.","struggled":"긴 문장 읽기에서 시간이 조금 걸렸어요."},"parent_mission":{"praise":"%s님이 이번 주도 열심히 공부했어요!","activity":"오늘 저녁에 이번 주 배운 내용을 함께 이야기해 보세요."}}',
        v_nickname, v_nickname
    );

    v_sections_this_week := format(
        '{"growth_summary":"이번 주 %s님은 매일 할 일을 챙기며 학습 습관을 잘 유지하고 있어요.","math":{"well_done":"뛰어세기 개념을 잘 이해하고 문제를 풀었어요.","struggled":"큰 수의 크기 비교에서 헷갈리는 부분이 있었어요."},"korean":{"well_done":"받침 있는 낱말을 정확히 썼어요.","struggled":"문장 부호 사용에서 연습이 더 필요해요."},"parent_mission":{"praise":"%s님, 이번 주도 정말 잘했어요!","activity":"주말에 좋아하는 동물 이야기를 함께 읽어 보세요."}}',
        v_nickname, v_nickname
    );

    INSERT INTO weekly_report (member_id, week_start, week_end, sections_json, mission_revealed, created_at, updated_at)
    VALUES
        (v_member_id, v_last_week_start, v_last_week_end, v_sections_last_week, TRUE,  NOW(), NOW()),
        (v_member_id, v_this_week_start, v_this_week_end, v_sections_this_week, FALSE, NOW(), NOW());

    INSERT INTO weekly_safety_report (member_id, week_start, week_end, safety_signal, score, reason_summary, created_at)
    VALUES
        (v_member_id, v_last_week_start, v_last_week_end, 'GREEN',  12, NULL, NOW()),
        (v_member_id, v_this_week_start, v_this_week_end, 'YELLOW', 35, '가끔 힘들다는 표현이 있었지만 전반적으로 안전한 대화였어요.', NOW());

    RAISE NOTICE '데모 시드 완료 — member_id=%, nickname=%, available_pool=30, learning_attempt=22건, reports=2건', v_member_id, v_nickname;
    RAISE NOTICE '부모 모드 PIN: 1234';
END
$seed$;
