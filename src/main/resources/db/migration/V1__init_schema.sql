-- =====================================================================
-- V1: 전체 초기 스키마
-- =====================================================================

-- 1. members
CREATE TABLE members (
    id                      BIGSERIAL PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    profile_image_url       VARCHAR(255),
    provider_id             VARCHAR(255) NOT NULL UNIQUE,
    provider                VARCHAR(255) NOT NULL,
    role                    VARCHAR(255) NOT NULL,
    nickname                VARCHAR(255),
    grade                   INTEGER,
    guardian_phone          VARCHAR(255),
    notification_enabled    BOOLEAN      NOT NULL,
    microphone_enabled      BOOLEAN      NOT NULL,
    onboarding_completed    BOOLEAN      NOT NULL,
    terms_agreed            BOOLEAN      NOT NULL DEFAULT FALSE,
    privacy_agreed          BOOLEAN      NOT NULL DEFAULT FALSE,
    marketing_agreed        BOOLEAN      NOT NULL DEFAULT FALSE,
    terms_agreed_at         TIMESTAMP,
    parent_password         VARCHAR(255),
    equipped_outfit_code    VARCHAR(32),
    equipped_hat_id         BIGINT,
    equipped_glasses_id     BIGINT,
    equipped_clothes_id     BIGINT,
    equipped_background_id  BIGINT,
    representative_item_type VARCHAR(255),
    created_at              TIMESTAMP    NOT NULL,
    last_login_at           TIMESTAMP    NOT NULL
);

-- 2. math_step
CREATE TABLE math_step (
    id           BIGSERIAL PRIMARY KEY,
    grade        INTEGER      NOT NULL,
    step_number  INTEGER      NOT NULL,
    step_title   VARCHAR(255) NOT NULL,
    concept      VARCHAR(255) NOT NULL,
    content_json TEXT         NOT NULL,
    CONSTRAINT uq_math_step_grade_number UNIQUE (grade, step_number)
);

-- 3. korean_step
CREATE TABLE korean_step (
    id           BIGSERIAL PRIMARY KEY,
    grade        INTEGER      NOT NULL,
    step_number  INTEGER      NOT NULL,
    step_title   VARCHAR(255) NOT NULL,
    concept      VARCHAR(255) NOT NULL,
    content_json TEXT         NOT NULL,
    CONSTRAINT uq_korean_step_grade_number UNIQUE (grade, step_number)
);

-- 4. shop_items
CREATE TABLE shop_items (
    id        BIGSERIAL PRIMARY KEY,
    item_code VARCHAR(32)  NOT NULL UNIQUE,
    name      VARCHAR(255) NOT NULL,
    category  VARCHAR(20)  NOT NULL,
    price     INTEGER      NOT NULL,
    image_key VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN      NOT NULL
);

-- 5. user_stats
CREATE TABLE user_stats (
    id                   BIGSERIAL PRIMARY KEY,
    member_id            BIGINT    NOT NULL UNIQUE,
    level                INTEGER   NOT NULL,
    total_pool_earned    INTEGER   NOT NULL,
    available_pool       INTEGER   NOT NULL,
    streak_days          INTEGER   NOT NULL,
    last_attendance_date DATE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_stats_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_user_stats_member_id ON user_stats (member_id);

-- 6. reward_history
CREATE TABLE reward_history (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT      NOT NULL,
    source     VARCHAR(30) NOT NULL,
    amount     INTEGER     NOT NULL,
    ref_id     BIGINT,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_reward_history_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_reward_history_member_id ON reward_history (member_id, created_at);

-- 7. todos
CREATE TABLE todos (
    id            BIGSERIAL PRIMARY KEY,
    member_id     BIGINT      NOT NULL,
    type          VARCHAR(20) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    source        VARCHAR(20) NOT NULL,
    learning_type VARCHAR(20),
    assigned_date DATE        NOT NULL,
    completed_at  TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    CONSTRAINT fk_todos_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_todos_member_date ON todos (member_id, assigned_date);

-- 8. chat_sessions
CREATE TABLE chat_sessions (
    id          BIGSERIAL PRIMARY KEY,
    session_key VARCHAR(100) NOT NULL UNIQUE,
    member_id   BIGINT       NOT NULL,
    title       VARCHAR(255),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_chat_sessions_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_chat_sessions_member ON chat_sessions (member_id);
CREATE INDEX idx_chat_sessions_key ON chat_sessions (session_key);

-- 9. chat_messages
CREATE TABLE chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id)
);

CREATE INDEX idx_chat_messages_session ON chat_messages (session_id);
CREATE INDEX idx_chat_messages_member_created ON chat_messages (member_id, created_at);

-- 10. learning_attempt
CREATE TABLE learning_attempt (
    id             BIGSERIAL PRIMARY KEY,
    member_id      BIGINT      NOT NULL,
    subject        VARCHAR(10) NOT NULL,
    concept        VARCHAR(255) NOT NULL,
    step_id        BIGINT      NOT NULL,
    cycle_number   INTEGER     NOT NULL,
    question_index INTEGER     NOT NULL,
    attempts       INTEGER     NOT NULL,
    hints_used     INTEGER     NOT NULL,
    solved         BOOLEAN     NOT NULL,
    solved_at      TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    CONSTRAINT fk_learning_attempt_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_learning_attempt_member_subject ON learning_attempt (member_id, subject, solved_at);
CREATE INDEX idx_learning_attempt_problem ON learning_attempt (member_id, step_id, cycle_number, question_index);

-- 11. user_math_progress
CREATE TABLE user_math_progress (
    id                BIGSERIAL PRIMARY KEY,
    member_id         BIGINT    NOT NULL,
    math_step_id      BIGINT    NOT NULL,
    is_step_completed BOOLEAN   NOT NULL,
    last_accessed_at  TIMESTAMP,
    CONSTRAINT uq_user_math_progress UNIQUE (member_id, math_step_id),
    CONSTRAINT fk_user_math_progress_member FOREIGN KEY (member_id)    REFERENCES members (id),
    CONSTRAINT fk_user_math_progress_step   FOREIGN KEY (math_step_id) REFERENCES math_step (id)
);

-- 12. user_math_progress_cycles (ElementCollection)
CREATE TABLE user_math_progress_cycles (
    progress_id  BIGINT NOT NULL,
    cycle_number INTEGER,
    CONSTRAINT fk_math_progress_cycles FOREIGN KEY (progress_id) REFERENCES user_math_progress (id)
);

-- 13. user_korean_progress
CREATE TABLE user_korean_progress (
    id                BIGSERIAL PRIMARY KEY,
    member_id         BIGINT    NOT NULL,
    korean_step_id    BIGINT    NOT NULL,
    is_step_completed BOOLEAN   NOT NULL,
    last_accessed_at  TIMESTAMP,
    CONSTRAINT uq_user_korean_progress UNIQUE (member_id, korean_step_id),
    CONSTRAINT fk_user_korean_progress_member FOREIGN KEY (member_id)      REFERENCES members (id),
    CONSTRAINT fk_user_korean_progress_step   FOREIGN KEY (korean_step_id) REFERENCES korean_step (id)
);

-- 14. user_korean_progress_cycles (ElementCollection)
CREATE TABLE user_korean_progress_cycles (
    progress_id  BIGINT NOT NULL,
    cycle_number INTEGER,
    CONSTRAINT fk_korean_progress_cycles FOREIGN KEY (progress_id) REFERENCES user_korean_progress (id)
);

-- 15. member_shop_items
CREATE TABLE member_shop_items (
    id           BIGSERIAL PRIMARY KEY,
    member_id    BIGINT    NOT NULL,
    shop_item_id BIGINT    NOT NULL,
    acquired_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_member_shop_item   UNIQUE (member_id, shop_item_id),
    CONSTRAINT fk_member_shop_member FOREIGN KEY (member_id)    REFERENCES members (id),
    CONSTRAINT fk_member_shop_item   FOREIGN KEY (shop_item_id) REFERENCES shop_items (id)
);

CREATE INDEX idx_member_shop_items_member ON member_shop_items (member_id);

-- 16. member_interests
CREATE TABLE member_interests (
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    interest  VARCHAR(255) NOT NULL,
    CONSTRAINT fk_member_interests_member FOREIGN KEY (member_id) REFERENCES members (id)
);

-- 17. member_items
CREATE TABLE member_items (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT      NOT NULL,
    item_type   VARCHAR(255) NOT NULL,
    acquired_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_member_items_member FOREIGN KEY (member_id) REFERENCES members (id)
);

-- 18. weekly_report
CREATE TABLE weekly_report (
    id               BIGSERIAL PRIMARY KEY,
    member_id        BIGINT    NOT NULL,
    week_start       DATE      NOT NULL,
    week_end         DATE      NOT NULL,
    sections_json    TEXT      NOT NULL,
    mission_revealed BOOLEAN   NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    CONSTRAINT uk_weekly_report_member_week UNIQUE (member_id, week_start),
    CONSTRAINT fk_weekly_report_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_weekly_report_member ON weekly_report (member_id, week_start DESC);

-- 19. weekly_safety_report
CREATE TABLE weekly_safety_report (
    id             BIGSERIAL PRIMARY KEY,
    member_id      BIGINT      NOT NULL,
    week_start     DATE        NOT NULL,
    week_end       DATE        NOT NULL,
    safety_signal  VARCHAR(10) NOT NULL,
    score          INTEGER     NOT NULL,
    reason_summary VARCHAR(255),
    created_at     TIMESTAMP   NOT NULL,
    CONSTRAINT uk_weekly_safety_member_week UNIQUE (member_id, week_start),
    CONSTRAINT fk_weekly_safety_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_weekly_safety_member ON weekly_safety_report (member_id, week_start DESC);
