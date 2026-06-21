-- ============================================================
-- 상점 — 레오 전체 의상 프리셋 (FE 고정 item_code)
-- 온보딩 선물: GIFT_1(책), GIFT_2(리본), GIFT_3(꽃다발), GIFT_4(선글라스)
-- ============================================================

INSERT INTO shop_items (item_code, name, category, price, image_key, is_active)
VALUES
  ('OUTFIT_DEFAULT',  '기본',         'OUTFIT', 0,  'outfit_default',   true),
  ('GIFT_3',          '꽃다발',       'OUTFIT', 5,  'outfit_flower',    true),
  ('GIFT_1',          '책',           'OUTFIT', 5,  'outfit_book',      true),
  ('GIFT_4',          '선글라스',     'OUTFIT', 10, 'outfit_sunglasses', true),
  ('GIFT_2',          '리본',         'OUTFIT', 10, 'outfit_ribbon',    true),
  ('GIFT_6',          '과학자 가운',  'OUTFIT', 15, 'outfit_scientist', true),
  ('GIFT_7',          '가수',         'OUTFIT', 15, 'outfit_singer',    true),
  ('GIFT_5',          '공룡 의상',    'OUTFIT', 15, 'outfit_dino',      true),
  ('OUTFIT_STUDYING', '공부중',       'OUTFIT', 20, 'outfit_studying',  true)
ON CONFLICT (item_code) DO UPDATE SET
  name = EXCLUDED.name,
  category = EXCLUDED.category,
  price = EXCLUDED.price,
  image_key = EXCLUDED.image_key,
  is_active = EXCLUDED.is_active;
