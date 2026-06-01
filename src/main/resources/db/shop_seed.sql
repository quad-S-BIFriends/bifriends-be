-- ============================================================
-- 상점 아이템 시드 데이터
-- 가격 정책 (기능명세서 기준):
--   HAT        5풀  (소형)
--   GLASSES   10풀  (소형)
--   CLOTHES   15풀  (중형)
--   BACKGROUND 30풀  (대형)
-- ============================================================

INSERT INTO shop_items (name, category, price, image_key, is_active)
SELECT * FROM (VALUES
  -- 모자 (5풀)
  ('빨간 야구모자',  'HAT',        5,  'hat_baseball_red',    true),
  ('파란 야구모자',  'HAT',        5,  'hat_baseball_blue',   true),
  ('밀짚모자',      'HAT',        5,  'hat_straw',           true),
  ('왕관',          'HAT',        5,  'hat_crown',           true),

  -- 안경 (10풀)
  ('하트 안경',     'GLASSES',    10, 'glasses_heart',       true),
  ('별 안경',       'GLASSES',    10, 'glasses_star',        true),
  ('둥근 안경',     'GLASSES',    10, 'glasses_round',       true),

  -- 옷 (15풀)
  ('우주복',        'CLOTHES',    15, 'clothes_spacesuit',   true),
  ('줄무늬 티셔츠', 'CLOTHES',    15, 'clothes_stripe',      true),
  ('공룡 후드집업', 'CLOTHES',    15, 'clothes_dino_hoodie', true),

  -- 배경 (30풀)
  ('우주 배경',     'BACKGROUND', 30, 'bg_space',            true),
  ('숲속 배경',     'BACKGROUND', 30, 'bg_forest',           true),
  ('바닷속 배경',   'BACKGROUND', 30, 'bg_ocean',            true)
) AS v(name, category, price, image_key, is_active)
WHERE NOT EXISTS (SELECT 1 FROM shop_items LIMIT 1);
