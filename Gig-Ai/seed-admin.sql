-- Run this AFTER starting the backend (tables must exist first)
-- Password is: admin123 (BCrypt encoded)

INSERT INTO users (name, phone, email, password, city, risk_level, role, wallet_balance, created_at)
VALUES (
  'Admin',
  '0000000000',
  'admin@gigshield.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9S',
  'Mumbai',
  'LOW',
  'ADMIN',
  10000.00,
  NOW()
) ON DUPLICATE KEY UPDATE role = 'ADMIN';
