-- Initialize MySQL database for ds-ai2 project
CREATE DATABASE IF NOT EXISTS js CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE js;

-- Grant all privileges to nerget-user
GRANT ALL PRIVILEGES ON js.* TO 'nerget-user'@'%';
FLUSH PRIVILEGES;