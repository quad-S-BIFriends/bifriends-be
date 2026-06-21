-- =====================================================================
-- V2: 상점 초기 데이터
-- =====================================================================

INSERT INTO shop_items (item_code, name, category, price, image_key, is_active)
VALUES
  ('OUTFIT_DEFAULT',  '기본',        'OUTFIT', 0,  'outfit_default',    TRUE),
  ('GIFT_3',          '꽃다발',      'OUTFIT', 5,  'outfit_flower',     TRUE),
  ('GIFT_1',          '책',          'OUTFIT', 5,  'outfit_book',       TRUE),
  ('GIFT_4',          '선글라스',    'OUTFIT', 10, 'outfit_sunglasses', TRUE),
  ('GIFT_2',          '리본',        'OUTFIT', 10, 'outfit_ribbon',     TRUE),
  ('GIFT_6',          '과학자 가운', 'OUTFIT', 15, 'outfit_scientist',  TRUE),
  ('GIFT_7',          '가수',        'OUTFIT', 15, 'outfit_singer',     TRUE),
  ('GIFT_5',          '공룡 의상',   'OUTFIT', 15, 'outfit_dino',       TRUE),
  ('OUTFIT_STUDYING', '공부중',      'OUTFIT', 20, 'outfit_studying',   TRUE)
ON CONFLICT (item_code) DO UPDATE SET
  name      = EXCLUDED.name,
  category  = EXCLUDED.category,
  price     = EXCLUDED.price,
  image_key = EXCLUDED.image_key,
  is_active = EXCLUDED.is_active;
