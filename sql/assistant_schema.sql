-- 개인 비서 에이전트를 위한 데이터베이스 스키마
CREATE DATABASE IF NOT EXISTS assistant_db;
USE assistant_db;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 일정 관리 테이블
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    location VARCHAR(255),
    reminder_time DATETIME,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(50), -- daily, weekly, monthly
    status VARCHAR(50) DEFAULT 'scheduled', -- scheduled, completed, cancelled
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_time (user_id, start_time),
    INDEX idx_status (status)
);

-- 메모 테이블
CREATE TABLE IF NOT EXISTS notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    tags JSON, -- ["tag1", "tag2"]
    category VARCHAR(100),
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_category (user_id, category),
    INDEX idx_pinned (is_pinned),
    FULLTEXT idx_content (title, content)
);

-- TODO 리스트 테이블
CREATE TABLE IF NOT EXISTS todos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    priority ENUM('low', 'medium', 'high') DEFAULT 'medium',
    status ENUM('pending', 'in_progress', 'completed', 'cancelled') DEFAULT 'pending',
    completed_at TIMESTAMP NULL,
    tags JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_status (user_id, status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date)
);

-- 대화 세션 테이블
CREATE TABLE IF NOT EXISTS conversation_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    context JSON, -- 대화 컨텍스트 저장
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session (session_id),
    INDEX idx_active (is_active)
);

-- 대화 메시지 테이블
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('user', 'assistant', 'system') NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(100), -- schedule_add, note_create, todo_add, query 등
    entities JSON, -- 추출된 엔티티 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session_time (session_id, created_at)
);

-- 알림 테이블
CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    related_type ENUM('schedule', 'todo', 'custom') NOT NULL,
    related_id BIGINT,
    message TEXT NOT NULL,
    reminder_time DATETIME NOT NULL,
    is_sent BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reminder_time (reminder_time, is_sent),
    INDEX idx_user_reminders (user_id, is_sent)
);

-- 샘플 데이터 삽입
INSERT INTO users (username, email) VALUES 
('testuser', 'test@example.com');

INSERT INTO schedules (user_id, title, description, start_time, end_time, location) VALUES
(1, '팀 미팅', '주간 스프린트 리뷰', '2025-01-20 14:00:00', '2025-01-20 15:00:00', '회의실 A'),
(1, '점심 약속', '김과장님과 점심', '2025-01-21 12:00:00', '2025-01-21 13:00:00', '1층 식당');

INSERT INTO notes (user_id, title, content, tags, category) VALUES
(1, '프로젝트 아이디어', 'AI 비서 기능:\n- 일정 관리\n- 메모 정리\n- TODO 트래킹', '["ai", "project", "idea"]', 'work'),
(1, '장보기 리스트', '우유, 빵, 계란, 사과', '["shopping", "personal"]', 'personal');

INSERT INTO todos (user_id, title, description, due_date, priority, status) VALUES
(1, '보고서 작성', '월간 실적 보고서 작성', '2025-01-25', 'high', 'pending'),
(1, '코드 리뷰', 'PR #123 리뷰하기', '2025-01-20', 'medium', 'pending'),
(1, '운동하기', '헬스장 가기', '2025-01-19', 'low', 'completed');