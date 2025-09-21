-- Doubless Gym Membership Management System Database Schema
-- 더블리스 헬스장 회원권 관리 시스템 데이터베이스 스키마

-- 1. 회원 정보 테이블 (고객정보.xlsx 기반)
DROP TABLE IF EXISTS gym_members;
CREATE TABLE gym_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(50) UNIQUE NOT NULL COMMENT '회원번호',
    name VARCHAR(100) NOT NULL COMMENT '회원명',
    phone VARCHAR(20) COMMENT '전화번호',
    email VARCHAR(100) COMMENT '이메일',
    birth_date DATE COMMENT '생년월일',
    gender ENUM('M', 'F', 'OTHER') COMMENT '성별',
    address TEXT COMMENT '주소',
    registration_date DATE NOT NULL COMMENT '등록일',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'EXPIRED') DEFAULT 'ACTIVE' COMMENT '회원상태',
    notes TEXT COMMENT '비고',
    emergency_contact VARCHAR(20) COMMENT '비상연락처',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_status (status),
    INDEX idx_member_phone (phone),
    INDEX idx_member_registration (registration_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='헬스장 회원 정보';

-- 2. 회원권 종류 테이블
DROP TABLE IF EXISTS membership_types;
CREATE TABLE membership_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) UNIQUE NOT NULL COMMENT '회원권 코드',
    type_name VARCHAR(100) NOT NULL COMMENT '회원권명',
    category ENUM('GYM', 'PT', 'PILATES', 'YOGA', 'SWIMMING', 'PACKAGE') COMMENT '카테고리',
    duration_months INT COMMENT '기간(개월)',
    session_count INT COMMENT '횟수(PT/필라테스용)',
    price DECIMAL(10,2) NOT NULL COMMENT '정가',
    description TEXT COMMENT '설명',
    is_active BOOLEAN DEFAULT TRUE COMMENT '판매중 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_category (category),
    INDEX idx_type_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원권 종류';

-- 3. 회원권 구매 정보 테이블 (doubless.xlsx 기반)
DROP TABLE IF EXISTS membership_purchases;
CREATE TABLE membership_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_no VARCHAR(50) UNIQUE NOT NULL COMMENT '구매번호',
    member_id BIGINT NOT NULL COMMENT '회원ID',
    membership_type_id BIGINT NOT NULL COMMENT '회원권종류ID',
    purchase_date DATE NOT NULL COMMENT '구매일',
    start_date DATE NOT NULL COMMENT '시작일',
    end_date DATE NOT NULL COMMENT '종료일',
    original_price DECIMAL(10,2) NOT NULL COMMENT '정가',
    discount_amount DECIMAL(10,2) DEFAULT 0 COMMENT '할인금액',
    final_price DECIMAL(10,2) NOT NULL COMMENT '실제결제금액',
    payment_method VARCHAR(50) COMMENT '결제방법',
    remaining_sessions INT COMMENT '잔여횟수(PT/필라테스)',
    status ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED', 'TRANSFERRED') DEFAULT 'ACTIVE' COMMENT '상태',
    suspension_start_date DATE COMMENT '정지시작일',
    suspension_end_date DATE COMMENT '정지종료일',
    transfer_to_member_id BIGINT COMMENT '양도받은회원ID',
    transfer_date DATE COMMENT '양도일자',
    cancellation_date DATE COMMENT '취소일자',
    cancellation_reason TEXT COMMENT '취소사유',
    notes TEXT COMMENT '비고',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES gym_members(id),
    FOREIGN KEY (membership_type_id) REFERENCES membership_types(id),
    FOREIGN KEY (transfer_to_member_id) REFERENCES gym_members(id),
    INDEX idx_purchase_member (member_id),
    INDEX idx_purchase_status (status),
    INDEX idx_purchase_dates (start_date, end_date),
    INDEX idx_purchase_date (purchase_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원권 구매정보';

-- 4. PT/필라테스 세션 사용 기록
DROP TABLE IF EXISTS session_usage;
CREATE TABLE session_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    membership_purchase_id BIGINT NOT NULL COMMENT '회원권구매ID',
    member_id BIGINT NOT NULL COMMENT '회원ID',
    trainer_id BIGINT COMMENT '트레이너ID',
    usage_date DATETIME NOT NULL COMMENT '사용일시',
    session_type ENUM('PT', 'PILATES', 'YOGA', 'OTHER') COMMENT '세션종류',
    duration_minutes INT DEFAULT 60 COMMENT '세션시간(분)',
    notes TEXT COMMENT '비고',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (membership_purchase_id) REFERENCES membership_purchases(id),
    FOREIGN KEY (member_id) REFERENCES gym_members(id),
    INDEX idx_usage_member (member_id),
    INDEX idx_usage_date (usage_date),
    INDEX idx_usage_purchase (membership_purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PT/필라테스 세션 사용기록';

-- 5. 트레이너/강사 정보
DROP TABLE IF EXISTS trainers;
CREATE TABLE trainers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trainer_code VARCHAR(50) UNIQUE NOT NULL COMMENT '트레이너코드',
    name VARCHAR(100) NOT NULL COMMENT '이름',
    phone VARCHAR(20) COMMENT '전화번호',
    email VARCHAR(100) COMMENT '이메일',
    specialty VARCHAR(200) COMMENT '전문분야',
    hire_date DATE COMMENT '입사일',
    status ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE') DEFAULT 'ACTIVE' COMMENT '상태',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trainer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='트레이너/강사 정보';

-- 6. 매출 내역 (기존 sales_transactions 테이블 보완)
ALTER TABLE sales_transactions 
ADD COLUMN membership_purchase_id BIGINT COMMENT '회원권구매ID',
ADD FOREIGN KEY (membership_purchase_id) REFERENCES membership_purchases(id);

-- 7. 출입 기록 테이블
DROP TABLE IF EXISTS check_in_logs;
CREATE TABLE check_in_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL COMMENT '회원ID',
    check_in_time DATETIME NOT NULL COMMENT '입장시간',
    check_out_time DATETIME COMMENT '퇴장시간',
    membership_purchase_id BIGINT COMMENT '사용회원권ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES gym_members(id),
    FOREIGN KEY (membership_purchase_id) REFERENCES membership_purchases(id),
    INDEX idx_checkin_member (member_id),
    INDEX idx_checkin_date (check_in_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='출입 기록';

-- 8. 회원 건강 정보 (선택사항)
DROP TABLE IF EXISTS member_health_info;
CREATE TABLE member_health_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE COMMENT '회원ID',
    height DECIMAL(5,2) COMMENT '신장(cm)',
    weight DECIMAL(5,2) COMMENT '체중(kg)',
    body_fat_percentage DECIMAL(5,2) COMMENT '체지방률(%)',
    muscle_mass DECIMAL(5,2) COMMENT '근육량(kg)',
    health_conditions TEXT COMMENT '건강상태/질병',
    fitness_goals TEXT COMMENT '운동목표',
    measurement_date DATE COMMENT '측정일',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES gym_members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 건강정보';

-- 뷰 생성
-- 1. 활성 회원권 현황
CREATE OR REPLACE VIEW v_active_memberships AS
SELECT 
    m.member_code,
    m.name AS member_name,
    m.phone,
    mt.type_name AS membership_type,
    mp.start_date,
    mp.end_date,
    mp.remaining_sessions,
    mp.status,
    DATEDIFF(mp.end_date, CURDATE()) AS days_remaining
FROM membership_purchases mp
JOIN gym_members m ON mp.member_id = m.id
JOIN membership_types mt ON mp.membership_type_id = mt.id
WHERE mp.status = 'ACTIVE'
AND mp.end_date >= CURDATE();

-- 2. 회원권 만료 예정 (30일 이내)
CREATE OR REPLACE VIEW v_expiring_memberships AS
SELECT 
    m.member_code,
    m.name AS member_name,
    m.phone,
    mt.type_name AS membership_type,
    mp.end_date,
    DATEDIFF(mp.end_date, CURDATE()) AS days_until_expiry
FROM membership_purchases mp
JOIN gym_members m ON mp.member_id = m.id
JOIN membership_types mt ON mp.membership_type_id = mt.id
WHERE mp.status = 'ACTIVE'
AND mp.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
ORDER BY mp.end_date;

-- 3. PT/필라테스 잔여 횟수 현황
CREATE OR REPLACE VIEW v_remaining_sessions AS
SELECT 
    m.member_code,
    m.name AS member_name,
    mt.type_name AS membership_type,
    mp.purchase_date,
    mt.session_count AS total_sessions,
    mp.remaining_sessions,
    (mt.session_count - mp.remaining_sessions) AS used_sessions
FROM membership_purchases mp
JOIN gym_members m ON mp.member_id = m.id
JOIN membership_types mt ON mp.membership_type_id = mt.id
WHERE mt.category IN ('PT', 'PILATES')
AND mp.status = 'ACTIVE'
AND mp.remaining_sessions > 0;

-- 샘플 데이터 입력
INSERT INTO membership_types (type_code, type_name, category, duration_months, session_count, price, description) VALUES
('GYM_1M', '헬스 1개월', 'GYM', 1, NULL, 100000, '헬스장 1개월 이용권'),
('GYM_3M', '헬스 3개월', 'GYM', 3, NULL, 270000, '헬스장 3개월 이용권'),
('GYM_6M', '헬스 6개월', 'GYM', 6, NULL, 480000, '헬스장 6개월 이용권'),
('GYM_12M', '헬스 12개월', 'GYM', 12, NULL, 840000, '헬스장 12개월 이용권'),
('PT_10', 'PT 10회', 'PT', NULL, 10, 500000, '퍼스널 트레이닝 10회'),
('PT_20', 'PT 20회', 'PT', NULL, 20, 900000, '퍼스널 트레이닝 20회'),
('PT_30', 'PT 30회', 'PT', NULL, 30, 1200000, '퍼스널 트레이닝 30회'),
('PILATES_10', '필라테스 10회', 'PILATES', NULL, 10, 400000, '필라테스 10회'),
('PILATES_20', '필라테스 20회', 'PILATES', NULL, 20, 700000, '필라테스 20회'),
('YOGA_1M', '요가 1개월', 'YOGA', 1, NULL, 120000, '요가 1개월 이용권'),
('PACKAGE_3M', '헬스+PT 패키지', 'PACKAGE', 3, 10, 600000, '헬스 3개월 + PT 10회');