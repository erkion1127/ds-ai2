-- Sales Management Schema for Doubless
-- Database: js (JavaScript라는 의미로 추정)

CREATE SCHEMA IF NOT EXISTS js DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE js;

-- 회원 테이블
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_name VARCHAR(100) NOT NULL,
    member_code VARCHAR(50) UNIQUE,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    registration_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_name (member_name),
    INDEX idx_member_code (member_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품/서비스 테이블
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    product_type VARCHAR(50), -- PT, 헬스, 필라테스 등
    base_price DECIMAL(10, 2),
    duration_days INT,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_name (product_name),
    INDEX idx_product_type (product_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 매출 거래 테이블
CREATE TABLE IF NOT EXISTS sales_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(50) UNIQUE,
    transaction_date DATE NOT NULL,
    member_id BIGINT,
    product_id BIGINT,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50), -- 카드, 현금, 계좌이체 등
    payment_status VARCHAR(20) DEFAULT 'COMPLETED', -- COMPLETED, PENDING, CANCELLED
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    final_amount DECIMAL(10, 2),
    staff_name VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_member_id (member_id),
    INDEX idx_product_id (product_id),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일별 매출 요약 테이블 (집계용)
CREATE TABLE IF NOT EXISTS daily_sales_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_date DATE NOT NULL UNIQUE,
    total_transactions INT DEFAULT 0,
    total_sales_amount DECIMAL(12, 2) DEFAULT 0,
    cash_amount DECIMAL(12, 2) DEFAULT 0,
    card_amount DECIMAL(12, 2) DEFAULT 0,
    transfer_amount DECIMAL(12, 2) DEFAULT 0,
    new_members INT DEFAULT 0,
    active_members INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_summary_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 월별 매출 요약 테이블
CREATE TABLE IF NOT EXISTS monthly_sales_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL,
    month INT NOT NULL,
    total_transactions INT DEFAULT 0,
    total_sales_amount DECIMAL(12, 2) DEFAULT 0,
    average_transaction_amount DECIMAL(10, 2) DEFAULT 0,
    top_product_id BIGINT,
    top_member_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_year_month (year, month),
    FOREIGN KEY (top_product_id) REFERENCES products(id) ON DELETE SET NULL,
    FOREIGN KEY (top_member_id) REFERENCES members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 데이터 임포트 로그 테이블
CREATE TABLE IF NOT EXISTS import_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255),
    import_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_rows INT,
    success_rows INT,
    failed_rows INT,
    status VARCHAR(20),
    error_message TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 뷰: 최근 30일 매출 현황
CREATE OR REPLACE VIEW v_recent_sales AS
SELECT 
    st.transaction_date,
    COUNT(*) as transaction_count,
    SUM(st.final_amount) as total_amount,
    AVG(st.final_amount) as avg_amount
FROM sales_transactions st
WHERE st.transaction_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
  AND st.payment_status = 'COMPLETED'
GROUP BY st.transaction_date
ORDER BY st.transaction_date DESC;

-- 뷰: 상품별 매출 순위
CREATE OR REPLACE VIEW v_product_sales_ranking AS
SELECT 
    p.product_name,
    p.product_type,
    COUNT(st.id) as sales_count,
    SUM(st.final_amount) as total_sales,
    AVG(st.final_amount) as avg_sales
FROM products p
LEFT JOIN sales_transactions st ON p.id = st.product_id
WHERE st.payment_status = 'COMPLETED'
GROUP BY p.id, p.product_name, p.product_type
ORDER BY total_sales DESC;

-- 뷰: 회원별 구매 현황
CREATE OR REPLACE VIEW v_member_purchase_summary AS
SELECT 
    m.member_name,
    m.member_code,
    COUNT(st.id) as purchase_count,
    SUM(st.final_amount) as total_spent,
    MAX(st.transaction_date) as last_purchase_date
FROM members m
LEFT JOIN sales_transactions st ON m.id = st.member_id
WHERE st.payment_status = 'COMPLETED'
GROUP BY m.id, m.member_name, m.member_code
ORDER BY total_spent DESC;