-- 위스키 테이블 생성
CREATE TABLE IF NOT EXISTS whiskies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    region VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    age INT,
    abv DOUBLE,
    description TEXT,
    tasting_notes TEXT,
    flavor_profile VARCHAR(255),
    price_range INT,
    rating DOUBLE,
    food_pairing TEXT,
    occasion VARCHAR(255),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가
CREATE INDEX idx_whisky_type ON whiskies(type);
CREATE INDEX idx_whisky_region ON whiskies(region);
CREATE INDEX idx_whisky_price ON whiskies(price_range);
CREATE INDEX idx_whisky_rating ON whiskies(rating);