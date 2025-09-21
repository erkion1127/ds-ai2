-- 위스키 테이블 초기 데이터
INSERT INTO whiskies (name, brand, region, type, age, abv, description, tasting_notes, flavor_profile, price_range, rating, food_pairing, occasion, is_available)
VALUES
-- 스코틀랜드 싱글몰트
('Macallan 12 Year', 'The Macallan', 'Scotland - Speyside', 'Single Malt', 12, 40.0,
 '스페이사이드의 대표적인 싱글몰트. 셰리 캐스크에서 숙성되어 풍부하고 부드러운 맛이 특징',
 '바닐라, 생강, 말린 과일, 오크의 조화로운 향. 부드러운 셰리의 단맛과 스파이시한 피니시',
 'sweet, fruity, sherry, smooth', 3, 4.5,
 '다크 초콜릿, 건포도, 견과류, 치즈',
 '입문용, 선물용, 특별한날', true),

('Glenfiddich 15 Year', 'Glenfiddich', 'Scotland - Speyside', 'Single Malt', 15, 40.0,
 '세계에서 가장 많이 팔리는 싱글몰트 중 하나. 솔레라 시스템으로 숙성',
 '꿀, 바닐라, 풍부한 과일향. 크리미하고 실키한 질감',
 'sweet, honey, fruity, smooth', 3, 4.3,
 '연어, 치즈, 과일 타르트',
 '입문용, 일상음용', true),

('Ardbeg 10 Year', 'Ardbeg', 'Scotland - Islay', 'Single Malt', 10, 46.0,
 '아일라의 강렬한 피트 위스키. 스모키하면서도 복잡한 맛이 특징',
 '강렬한 피트 스모크, 바다 소금, 레몬, 바닐라. 긴 여운의 스모키 피니시',
 'peaty, smoky, maritime, intense', 3, 4.6,
 '굴, 훈제 연어, 블루 치즈, 다크 초콜릿',
 '애호가용, 특별한날', true),

('Lagavulin 16 Year', 'Lagavulin', 'Scotland - Islay', 'Single Malt', 16, 43.0,
 '아일라의 클래식. 깊고 풍부한 피트와 스모크의 완벽한 균형',
 '피트 스모크, 해초, 바닐라, 캐러멜. 길고 드라이한 피니시',
 'peaty, smoky, complex, rich', 4, 4.7,
 '스테이크, 훈제 고기, 다크 초콜릿',
 '애호가용, 특별한날, 선물용', true),

('Balvenie DoubleWood 12', 'The Balvenie', 'Scotland - Speyside', 'Single Malt', 12, 40.0,
 '전통적인 오크 캐스크와 셰리 캐스크에서 이중 숙성',
 '꿀, 바닐라, 셰리의 달콤함. 부드럽고 멜로우한 피니시',
 'sweet, honey, smooth, sherry', 3, 4.4,
 '치즈, 견과류, 과일 디저트',
 '입문용, 일상음용', true),

-- 아일랜드 위스키
('Jameson', 'Jameson', 'Ireland', 'Blended', null, 40.0,
 '세계에서 가장 유명한 아일랜드 위스키. 트리플 디스틸레이션으로 부드러운 맛',
 '바닐라, 꽃향, 스파이시한 우드. 부드럽고 균형잡힌 맛',
 'smooth, vanilla, light, accessible', 2, 4.0,
 '진저에일(하이볼), 라이트한 치즈, 과일',
 '입문용, 칵테일용, 일상음용', true),

('Redbreast 12 Year', 'Redbreast', 'Ireland', 'Single Pot Still', 12, 40.0,
 '아일랜드 싱글 포트 스틸의 대표작. 풍부하고 복잡한 맛',
 '과일, 스파이스, 토피, 셰리. 크리미하고 풀바디한 질감',
 'fruity, spicy, complex, creamy', 3, 4.5,
 '로스트 치킨, 치즈, 다크 초콜릿',
 '애호가용, 선물용', true),

-- 미국 위스키
('Buffalo Trace', 'Buffalo Trace', 'USA - Kentucky', 'Bourbon', null, 45.0,
 '켄터키 버번의 스탠다드. 균형잡힌 맛과 합리적인 가격',
 '캐러멜, 바닐라, 토피, 민트. 스파이시하고 달콤한 피니시',
 'sweet, vanilla, caramel, spicy', 2, 4.2,
 'BBQ, 버거, 애플파이, 피칸파이',
 '입문용, 일상음용, 칵테일용', true),

('Maker''s Mark', 'Maker''s Mark', 'USA - Kentucky', 'Bourbon', null, 45.0,
 '밀을 사용한 버번. 부드럽고 달콤한 맛이 특징',
 '캐러멜, 바닐라, 과일, 스파이스. 부드럽고 크리미한 질감',
 'sweet, smooth, wheated, caramel', 2, 4.1,
 '스테이크, BBQ, 초콜릿 디저트',
 '입문용, 칵테일용', true),

('Woodford Reserve', 'Woodford Reserve', 'USA - Kentucky', 'Bourbon', null, 43.2,
 '프리미엄 스몰 배치 버번. 복잡하고 풍부한 맛',
 '드라이 과일, 바닐라, 토피, 코코아. 길고 스파이시한 피니시',
 'complex, fruity, spicy, rich', 3, 4.3,
 '스테이크, 다크 초콜릿, 치즈',
 '선물용, 특별한날', true),

-- 일본 위스키
('Yamazaki 12 Year', 'Suntory', 'Japan', 'Single Malt', 12, 43.0,
 '일본 위스키의 선구자. 동양적인 섬세함과 복잡한 맛의 조화',
 '복숭아, 파인애플, 바닐라, 미즈나라 오크. 긴 여운의 스파이시한 피니시',
 'fruity, complex, delicate, spicy', 4, 4.6,
 '스시, 사시미, 템푸라, 와규',
 '애호가용, 특별한날, 선물용', true),

('Nikka From The Barrel', 'Nikka', 'Japan', 'Blended', null, 51.4,
 '높은 도수의 블렌디드 위스키. 강렬하면서도 균형잡힌 맛',
 '캐러멜, 바닐라, 오렌지, 스파이스. 풀바디하고 강렬한 피니시',
 'intense, complex, fruity, spicy', 3, 4.5,
 '구운 고기, 다크 초콜릿, 치즈',
 '애호가용, 특별한날', true),

('Hibiki Harmony', 'Suntory', 'Japan', 'Blended', null, 43.0,
 '일본 블렌디드 위스키의 정수. 완벽한 하모니와 균형',
 '꿀, 오렌지, 화이트 초콜릿, 미즈나라 오크. 섬세하고 우아한 피니시',
 'balanced, elegant, honey, smooth', 4, 4.7,
 '일본 요리, 해산물, 라이트한 치즈',
 '선물용, 특별한날, 애호가용', true),

-- 기타 프리미엄
('Glenfarclas 25 Year', 'Glenfarclas', 'Scotland - Speyside', 'Single Malt', 25, 43.0,
 '셰리 캐스크 숙성의 마스터피스. 깊고 풍부한 맛',
 '다크 초콜릿, 셰리, 오크, 스파이스. 길고 복잡한 피니시',
 'sherry, complex, rich, mature', 5, 4.8,
 '다크 초콜릿, 시가, 드라이 과일',
 '특별한날, 컬렉터용', true),

('Johnnie Walker Blue Label', 'Johnnie Walker', 'Scotland', 'Blended', null, 40.0,
 '조니워커의 최고급 블렌드. 희귀한 원액들의 완벽한 조화',
 '꿀, 과일, 스모크, 다크 초콜릿. 실키하고 복잡한 맛',
 'complex, smooth, luxurious, balanced', 5, 4.5,
 '시가, 다크 초콜릿, 캐비어',
 '선물용, 특별한날, 컬렉터용', true);