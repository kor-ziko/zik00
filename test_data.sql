-- 현재 Spring Boot JPA 엔티티 기준 통합 스키마와 개발용 시드 데이터
-- 개발 환경의 고정 PII_ENCRYPTION_KEYS(v1)로 암호화한 개인정보 시드 데이터
CREATE DATABASE IF NOT EXISTS shop
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE shop;

SET @demo_user_id = 1;
SET @tester1_user_id = 2;
SET @tester2_user_id = 3;
SET @tester3_user_id = 4;

-- access_id는 회원 생성 시 UUID()로 각각 발급한다.
-- 아래 UPSERT에서는 access_id를 갱신하지 않아 SQL 재실행으로 기존 JWT sub가 바뀌지 않는다.
-- login_id는 Google sub 조회와 UNIQUE 로그인을 위한 검색 키이므로 암호화하지 않는다.

CREATE TABLE IF NOT EXISTS `user` (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  access_id VARCHAR(36) NOT NULL,
  name VARCHAR(2048),
  name_kana VARCHAR(2048),
  birth_date VARCHAR(255),
  gender VARCHAR(2048),
  nickname VARCHAR(100),
  telephone VARCHAR(2048),
  login_id VARCHAR(100),
  deposit_balance INT NOT NULL DEFAULT 0,
  reward_point INT NOT NULL DEFAULT 0,
  mobile_phone VARCHAR(2048),
  email VARCHAR(2048),
  completed_order_count INT NOT NULL DEFAULT 0,
  joined_date DATE,
  member_detail VARCHAR(500) NOT NULL DEFAULT '일반회원',
  member_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  withdrawn_at DATETIME NULL,
  alarm_consent BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_access_id (access_id),
  UNIQUE KEY uk_user_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- noinspection SqlResolve
SET @member_status_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'member_status'
);
SET @member_status_sql = IF(@member_status_exists = 0,
  'ALTER TABLE `user` ADD COLUMN member_status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE'' AFTER member_detail', 'SELECT 1');
PREPARE member_status_statement FROM @member_status_sql;
EXECUTE member_status_statement;
DEALLOCATE PREPARE member_status_statement;

-- noinspection SqlResolve
SET @withdrawn_at_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'withdrawn_at'
);
SET @withdrawn_at_sql = IF(@withdrawn_at_exists = 0,
  'ALTER TABLE `user` ADD COLUMN withdrawn_at DATETIME NULL AFTER member_status', 'SELECT 1');
PREPARE withdrawn_at_statement FROM @withdrawn_at_sql;
EXECUTE withdrawn_at_statement;
DEALLOCATE PREPARE withdrawn_at_statement;

CREATE TABLE IF NOT EXISTS reward_point_history (
  point_history_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  amount INT NOT NULL,
  balance_after INT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (point_history_id),
  KEY idx_reward_point_member_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deposit_request (
  deposit_request_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  amount INT NOT NULL,
  depositor_name VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  admin_memo VARCHAR(500) NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  PRIMARY KEY (deposit_request_id),
  KEY idx_deposit_request_status_created (status, requested_at),
  KEY idx_deposit_request_member (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deposit_history (
  deposit_history_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  transaction_type VARCHAR(20) NOT NULL,
  amount INT NOT NULL,
  balance_after INT NOT NULL,
  description VARCHAR(255) NOT NULL,
  deposit_request_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (deposit_history_id),
  KEY idx_deposit_history_member_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 예전 시드에서 사용한 순차형 placeholder access_id만 한 번 실제 UUID로 교체한다.
-- 이 변경은 해당 개발용 테스트 계정의 기존 JWT를 무효화한다.
UPDATE `user`
SET access_id = UUID()
WHERE access_id IN (
  '00000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000003',
  '00000000-0000-0000-0000-000000000004'
);

CREATE TABLE IF NOT EXISTS addresses (
  address_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  address_name VARCHAR(2048),
  receiver_name VARCHAR(2048),
  receiver_phone VARCHAR(2048),
  zip_code VARCHAR(255),
  province VARCHAR(255),
  detail_address VARCHAR(2048),
  default_address BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (address_id),
  KEY idx_addresses_member_default (user_id, default_address, address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupon (
  coupon_id BIGINT NOT NULL AUTO_INCREMENT,
  coupon_template_id BIGINT NULL,
  user_id BIGINT NULL,
  coupon_name VARCHAR(255),
  discount_type VARCHAR(255),
  discount_value INT NOT NULL DEFAULT 0,
  minimum_order_amount INT NOT NULL DEFAULT 0,
  started_date DATE,
  expired_date DATE,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  used_at DATETIME NULL,
  coupon_code VARCHAR(255) NULL,
  guest_identifier VARCHAR(255) NULL,
  PRIMARY KEY (coupon_id),
  KEY idx_coupon_template_id (coupon_template_id),
  KEY idx_coupon_member_period (user_id, started_date, expired_date, coupon_id),
  KEY idx_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupon_template (
  coupon_template_id BIGINT NOT NULL AUTO_INCREMENT,
  coupon_name VARCHAR(255),
  discount_type VARCHAR(255),
  discount_value INT NOT NULL DEFAULT 0,
  minimum_order_amount INT NOT NULL DEFAULT 0,
  started_date DATE,
  expired_date DATE,
  target_type VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (coupon_template_id),
  KEY idx_coupon_template_active_period (active, started_date, expired_date, coupon_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS buylist (
  purchase_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_number VARCHAR(255),
  product_name VARCHAR(255),
  quantity INT NOT NULL DEFAULT 0,
  payment_amount INT NOT NULL DEFAULT 0,
  order_status VARCHAR(255),
  ordered_date DATE,
  PRIMARY KEY (purchase_id),
  KEY idx_buylist_member_ordered (user_id, ordered_date, purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiries (
  inquiry_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255),
  content LONGTEXT,
  status BOOLEAN NOT NULL DEFAULT FALSE,
  created_at VARCHAR(255),
  PRIMARY KEY (inquiry_id),
  KEY idx_inquiries_member_id (user_id, inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiry_comments (
  comment_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  user_id BIGINT,
  admin_id BIGINT,
  writer_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  writer_name VARCHAR(255),
  content LONGTEXT,
  created_at VARCHAR(255),
  PRIMARY KEY (comment_id),
  KEY idx_inquiry_comments_inquiry_comment (inquiry_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiry_comment_images (
  comment_image_id BIGINT NOT NULL AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  image_uuid VARCHAR(36) NOT NULL,
  image_path VARCHAR(255) NOT NULL,
  PRIMARY KEY (comment_image_id),
  UNIQUE KEY uk_inquiry_comment_images_uuid (image_uuid),
  KEY idx_inquiry_comment_images_comment_id (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiry_images (
  image_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  image_uuid VARCHAR(36) NOT NULL,
  image_path VARCHAR(255) NOT NULL,
  PRIMARY KEY (image_id),
  UNIQUE KEY uk_inquiry_images_uuid (image_uuid),
  KEY idx_inquiry_images_inquiry_id (inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_users (
  admin_id BIGINT NOT NULL AUTO_INCREMENT,
  login_id VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (admin_id),
  UNIQUE KEY uk_admin_users_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO admin_users (
  admin_id,
  login_id,
  password_hash,
  name,
  active
) VALUES (
  1,
  'admin',
  '$2b$10$l1oG8xMjNpmKTDQeyZYLFeGvXUuBheTR1I4020kgO9nNkcQXz9Hq.',
  '관리자',
  TRUE
) ON DUPLICATE KEY UPDATE
  login_id = VALUES(login_id),
  password_hash = VALUES(password_hash),
  name = VALUES(name),
  active = VALUES(active);

INSERT INTO `user` (
  user_id,
  access_id,
  name,
  name_kana,
  birth_date,
  gender,
  nickname,
  telephone,
  login_id,
  deposit_balance,
  reward_point,
  mobile_phone,
  email,
  completed_order_count,
  joined_date,
  member_detail,
  alarm_consent
) VALUES (
  @demo_user_id,
  UUID(),
  'enc:v1:y4tY-RWIkjBfCPUUMwBqlrxZX_F_vT3fnlil7g8VotzL7vvXW4kczw',
  'enc:v1:Cj8PfPOWPXFW1o35N6rEvN6TEc9YAXBJ6KKy-baroEWKLzQkeLUzgEnL3tQ',
  'enc:v1:uVWf_dKA-WXak6ajhv0WnXQhcDchMZRpti40a-ST0laHd-EBpDI',
  'enc:v1:jkFej_nengq7UF4MAff6gpPjNuNQ-TqkxBPwmuYvjqVrnA',
  '테스터01',
  'enc:v1:9-cGc-gelTvlJUNDJkAyKCIjv--DUZfOn9X_CpN7KO_Dsdb19gLsBQ',
  'testuser01',
  50000,
  1200,
  'enc:v1:Ml0_6uX10O4-w5WGLYVN_nl4XoDArHNEoiNyrCEzRuY4C0Gid9RznIQ',
  'enc:v1:tL9K3w6IcRejJYJnczqetNO988Xj6NNo6EgazD9R_hcWimSskffV8yXYsgG1UX-IBfQ',
  3,
  '2026-07-07',
  '테스트용 일반회원, 연락처/주문 이력 포함',
  FALSE
) ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  name_kana = VALUES(name_kana),
  birth_date = VALUES(birth_date),
  gender = VALUES(gender),
  nickname = VALUES(nickname),
  telephone = VALUES(telephone),
  login_id = VALUES(login_id),
  deposit_balance = VALUES(deposit_balance),
  reward_point = VALUES(reward_point),
  mobile_phone = VALUES(mobile_phone),
  email = VALUES(email),
  completed_order_count = VALUES(completed_order_count),
  joined_date = VALUES(joined_date),
  member_detail = VALUES(member_detail),
  alarm_consent = VALUES(alarm_consent);

INSERT INTO `user` (
  user_id,
  access_id,
  name,
  name_kana,
  birth_date,
  gender,
  nickname,
  telephone,
  login_id,
  deposit_balance,
  reward_point,
  mobile_phone,
  email,
  completed_order_count,
  joined_date,
  member_detail,
  alarm_consent
) VALUES
  (
    @tester1_user_id,
    UUID(),
    'enc:v1:PqhK9vvE22EnOvr0BG1L5nJSB4_DPFE4Y-ptr0rbXPZWKUoQFeE',
    'enc:v1:xRus8IeKsXc5MCZl-q8l6_M4pSSTEhVMX9gGwBSlSW04_Sm2d5m-QiNb0at0cOU',
    'enc:v1:tn1b3MEhG2XmTvMihJg-gqB-_ZIouTWL-hZVXBK4BKQZsx5X14c',
    'enc:v1:s7wO3QY1-ynRAq1ffWJY5XeJ5Jf13thpv5---M0GQCcvMQ',
    '테스터1',
    'enc:v1:QKuG9V-cTvEOZZcah06aCI7GE1fmKlUGWDqVpvWE49SrzuloxDPgAw',
    'tester1',
    10000,
    100,
    'enc:v1:yX_M4jxMC4IsHBkKmU6gU3ieRpcb7ZrcyfCslyEPIQxHgnBkAn15Z-g',
    'enc:v1:dqMKbJMSlLwlWEUFLICJ6hDjPBfM14A8Jt8FU3utGO_molbrOH6LNK0HgK50pz0',
    1,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 1',
    TRUE
  ),
  (
    @tester2_user_id,
    UUID(),
    'enc:v1:pe9D_YLLSgMGLasG7HwQESN1Sw_hgS7mIqAeTIKud4QT3HC5tXo',
    'enc:v1:Jv8z30HygDSQr_ELCA7uoBNPuawXMPaf3RzGIGAv-hzd098pUFClzrq4h-E',
    'enc:v1:0hAa6nU8WTjCEZIgCK3sGV-iAK3trz12Tp5R14XQuKaWAlLjBUI',
    'enc:v1:Mzk_0WLeT2fyyAaI6rMIfSevohTQwrpFhtQmxiGtRvY_OQ',
    '테스터2',
    'enc:v1:mvzAAoU7Ry6dyjjkvwvMnxuY9_MDMlJDKDokeAd3t5FBUrj1piWqhQ',
    'tester2',
    20000,
    200,
    'enc:v1:KCfOatvIfOteGAH_6BfKji84ZNmFT5gA9FjYf85lA0-Pu8kdSei_gPw',
    'enc:v1:cNHAln1VbujkYrMRAB_IXuQqfhxzx0N0-VCFZUbmgE2zd3tc9UnBYTZ5QzVHwbU',
    2,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 2',
    FALSE
  ),
  (
    @tester3_user_id,
    UUID(),
    'enc:v1:G7ek4K7eN-aNPqWs_-bhlQZvJANGDXdaxw10PAYRQtkGS_xVRx0',
    'enc:v1:rhZxDcMoO74LtNXuqNlbqm7Mcbu1McU_U-TewCfcdrbFqGd51661zHWHvmWr4Xc',
    'enc:v1:2blnSrsdfICi1-vhbvSOj2Q2SKNd8NIPpGT2e_A9u2eA4yZ2czg',
    'enc:v1:q0wVb2U7mcEoz9tlsqINpp0_OigQmb3WGS1xySalh5ZmAQ',
    '테스터3',
    'enc:v1:n9T9HibxkcsNtJp3bS8Z8AsVqI6X3S3PGqbwpA8gpjfhI_iqBQKmLA',
    'tester3',
    30000,
    300,
    'enc:v1:iBcLJRLF8ILcP-pkBxgos-iBTJbc-WSvh8Z8J_iNnx-_cfWu5hpnvyQ',
    'enc:v1:YtqGw7MKhQNlcNWdFF5rJoVSbtqfkScuo_kJLMDQPe076RNQnuu_49FJWko7LDY',
    3,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 3',
    TRUE
  )
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  name_kana = VALUES(name_kana),
  birth_date = VALUES(birth_date),
  gender = VALUES(gender),
  nickname = VALUES(nickname),
  telephone = VALUES(telephone),
  login_id = VALUES(login_id),
  deposit_balance = VALUES(deposit_balance),
  reward_point = VALUES(reward_point),
  mobile_phone = VALUES(mobile_phone),
  email = VALUES(email),
  completed_order_count = VALUES(completed_order_count),
  joined_date = VALUES(joined_date),
  member_detail = VALUES(member_detail),
  alarm_consent = VALUES(alarm_consent);

UPDATE `user`
SET member_status = 'ACTIVE', withdrawn_at = NULL
WHERE user_id IN (@demo_user_id, @tester1_user_id, @tester2_user_id);

UPDATE `user`
SET member_status = 'WITHDRAWN', withdrawn_at = '2026-08-01 15:30:00'
WHERE user_id = @tester3_user_id;

INSERT INTO reward_point_history
  (point_history_id, user_id, amount, balance_after, reason, created_at)
VALUES
  (1, @demo_user_id, 1200, 1200, '신규 가입 및 구매 적립', '2026-07-07 10:00:00'),
  (2, @tester1_user_id, 100, 100, '첫 구매 적립', '2026-07-09 11:30:00'),
  (3, @tester2_user_id, 500, 500, '상품 구매 적립', '2026-07-10 14:20:00'),
  (4, @tester2_user_id, -300, 200, '주문 결제 사용', '2026-07-11 09:40:00')
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id), amount = VALUES(amount), balance_after = VALUES(balance_after),
  reason = VALUES(reason), created_at = VALUES(created_at);

INSERT INTO deposit_request
  (deposit_request_id, user_id, amount, depositor_name, status, admin_memo, requested_at, processed_at)
VALUES
  (1, @tester1_user_id, 30000, '테스터1', 'PENDING', NULL, '2026-08-10 10:10:00', NULL),
  (2, @tester2_user_id, 20000, '테스터2', 'APPROVED', '입금 확인', '2026-08-08 13:25:00', '2026-08-08 14:00:00')
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id), amount = VALUES(amount), depositor_name = VALUES(depositor_name),
  status = VALUES(status), admin_memo = VALUES(admin_memo), requested_at = VALUES(requested_at), processed_at = VALUES(processed_at);

INSERT INTO deposit_history
  (deposit_history_id, user_id, transaction_type, amount, balance_after, description, deposit_request_id, created_at)
VALUES
  (1, @demo_user_id, 'CHARGE', 60000, 60000, '예치금 충전', NULL, '2026-07-07 10:30:00'),
  (2, @demo_user_id, 'USE', -10000, 50000, '구매대행 주문 결제', NULL, '2026-07-12 16:15:00'),
  (3, @tester2_user_id, 'CHARGE', 20000, 20000, '예치금 신청 승인', 2, '2026-08-08 14:00:00')
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id), transaction_type = VALUES(transaction_type), amount = VALUES(amount),
  balance_after = VALUES(balance_after), description = VALUES(description), deposit_request_id = VALUES(deposit_request_id),
  created_at = VALUES(created_at);

INSERT INTO addresses (
  address_id,
  user_id,
  address_name,
  receiver_name,
  receiver_phone,
  zip_code,
  province,
  detail_address,
  default_address
) VALUES (
  1,
  @demo_user_id,
  'enc:v1:LjddRM51oM_x6-DsTNVpNYYq3PByByefzt5ah8g_HA',
  'enc:v1:YVz-e0WG_yrdUpuP9c0eMNewo95pZgeS6LlOvGT9Xup8HtL5Wc8ZTg',
  'enc:v1:nZ5VQcHVyC8kRmMTEn-GPW2l-1r4lSH9c9VCdUTNZsef8GEBM-JxVF4',
  '100-0005',
  '東京都',
  'enc:v1:3BpKN-2ulUalI8SdmG04aZGfoYtApjT6knC0YGfRIfbqvovyQcpn1v4ZVkbDaNdBVZvBHjqHe3I',
  1
) ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  address_name = VALUES(address_name),
  receiver_name = VALUES(receiver_name),
  receiver_phone = VALUES(receiver_phone),
  zip_code = VALUES(zip_code),
  province = VALUES(province),
  detail_address = VALUES(detail_address),
  default_address = VALUES(default_address);

INSERT INTO coupon_template (
  coupon_template_id,
  coupon_name,
  discount_type,
  discount_value,
  minimum_order_amount,
  started_date,
  expired_date,
  target_type,
  active
) VALUES
  (1, '신규회원 10% 할인', 'percent', 10, 10000, '2026-12-01', '2026-12-31', 'MEMBER', TRUE),
  (2, '무료배송 쿠폰', 'shipping', 3000, 0, '2026-09-01', '2026-09-30', 'ALL', TRUE),
  (3, '테스터1 쿠폰', 'amount', 5000, 30000, '2026-08-01', '2026-08-31', 'MEMBER', TRUE)
ON DUPLICATE KEY UPDATE
  coupon_name = VALUES(coupon_name),
  discount_type = VALUES(discount_type),
  discount_value = VALUES(discount_value),
  minimum_order_amount = VALUES(minimum_order_amount),
  started_date = VALUES(started_date),
  expired_date = VALUES(expired_date),
  target_type = VALUES(target_type),
  active = VALUES(active);

INSERT INTO coupon (
  coupon_id,
  coupon_template_id,
  user_id,
  coupon_name,
  discount_type,
  discount_value,
  minimum_order_amount,
  started_date,
  expired_date,
  used
) VALUES
  (1, 1, @demo_user_id, '신규회원 10% 할인', 'percent', 10, 10000, '2026-12-01', '2026-12-31', FALSE),
  (2, 2, @demo_user_id, '무료배송 쿠폰', 'shipping', 3000, 0, '2026-09-01', '2026-09-30', FALSE),
  (3, 3, @tester1_user_id, '테스터1 쿠폰', 'amount', 5000, 30000, '2026-08-01', '2026-08-31', FALSE)
ON DUPLICATE KEY UPDATE
  coupon_template_id = VALUES(coupon_template_id),
  user_id = VALUES(user_id),
  coupon_name = VALUES(coupon_name),
  discount_type = VALUES(discount_type),
  discount_value = VALUES(discount_value),
  minimum_order_amount = VALUES(minimum_order_amount),
  started_date = VALUES(started_date),
  expired_date = VALUES(expired_date),
  used = VALUES(used);

INSERT INTO buylist (
  purchase_id,
  user_id,
  order_number,
  product_name,
  quantity,
  payment_amount,
  order_status,
  ordered_date
) VALUES
  (1, @demo_user_id, 'ORD-20260701-001', '린넨 셔츠', 2, 59000, '주문완료', '2026-07-01'),
  (2, @demo_user_id, 'ORD-20260703-002', '데님 팬츠', 1, 79000, '배송중', '2026-07-03'),
  (3, @tester1_user_id, 'ORD-20260704-003', '테스터1 테스트상품', 1, 30000, '주문완료', '2026-07-04')
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  order_number = VALUES(order_number),
  product_name = VALUES(product_name),
  quantity = VALUES(quantity),
  payment_amount = VALUES(payment_amount),
  order_status = VALUES(order_status),
  ordered_date = VALUES(ordered_date);

INSERT INTO inquiries (
  inquiry_id,
  user_id,
  title,
  content,
  status,
  created_at
) VALUES
  (1, @demo_user_id, '배송은 언제 시작되나요?', '어제 주문했는데 배송 상태가 궁금합니다.', TRUE, '2026-07-07 16:20'),
  (2, @demo_user_id, '1', '1', FALSE, '2026-07-07 18:55')
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  title = VALUES(title),
  content = VALUES(content),
  status = VALUES(status),
  created_at = VALUES(created_at);

INSERT INTO inquiry_comments (
  comment_id,
  inquiry_id,
  user_id,
  admin_id,
  writer_type,
  writer_name,
  content,
  created_at
) VALUES
  (1, 1, @demo_user_id, NULL, 'USER', '테스터01', '확인 부탁드립니다.', '2026-07-07 16:22'),
  (2, 1, @demo_user_id, NULL, 'USER', '테스터01', '11', '2026-07-07 18:55'),
  (1001, 1, NULL, 1, 'ADMIN', '관리자', '배송 완료하였습니다.', '2026-07-13 15:55')
ON DUPLICATE KEY UPDATE
  inquiry_id = VALUES(inquiry_id),
  user_id = VALUES(user_id),
  admin_id = VALUES(admin_id),
  writer_type = VALUES(writer_type),
  writer_name = VALUES(writer_name),
  content = VALUES(content),
  created_at = VALUES(created_at);

-- @Suil - 관리자 답변 테스트 사진을 답변 댓글과 연결
INSERT INTO inquiry_comment_images (
  comment_image_id,
  comment_id,
  image_uuid,
  image_path
) VALUES (
  1001,
  1001,
  'a79f2835-b7c0-4995-8b49-426bd35ebb17',
  'a79f2835-b7c0-4995-8b49-426bd35ebb17.webp'
)
ON DUPLICATE KEY UPDATE
  comment_id = VALUES(comment_id),
  image_uuid = VALUES(image_uuid),
  image_path = VALUES(image_path);

INSERT INTO inquiry_images (
  image_id,
  inquiry_id,
  image_uuid,
  image_path
) VALUES
  (
    1,
    1,
    '11111111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111.jpg'
  ),
  (
    2,
    2,
    '22222222-2222-2222-2222-222222222222',
    '22222222-2222-2222-2222-222222222222.png'
  ),
  (
    3,
    2,
    '33333333-3333-3333-3333-333333333333',
    '33333333-3333-3333-3333-333333333333.jpg'
  )
ON DUPLICATE KEY UPDATE
  inquiry_id = VALUES(inquiry_id),
  image_uuid = VALUES(image_uuid),
  image_path = VALUES(image_path);

-- @Daeyoung - 사용자 서비스 소개와 공지사항 공개 콘텐츠
CREATE TABLE IF NOT EXISTS service_intro_sections (
  section_id BIGINT NOT NULL AUTO_INCREMENT,
  section_type VARCHAR(40) NOT NULL,
  eyebrow VARCHAR(100) NULL,
  title VARCHAR(200) NOT NULL,
  content LONGTEXT NOT NULL,
  detail VARCHAR(500) NULL,
  image_url VARCHAR(500) NULL,
  display_order INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (section_id),
  KEY idx_service_intro_active_order (active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_setting_entries (
  setting_id BIGINT NOT NULL AUTO_INCREMENT,
  setting_type VARCHAR(40) NOT NULL,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(200) NOT NULL,
  content LONGTEXT NULL,
  field_data LONGTEXT NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (setting_id),
  UNIQUE KEY uk_admin_setting_type_code (setting_type, code),
  KEY idx_admin_setting_type_order (setting_type, display_order, setting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO admin_setting_entries
  (setting_id, setting_type, code, name, content, field_data, display_order, active)
VALUES
  (1, 'MEMBER_GRADE', 'BASIC', '일반회원', '기본 회원 등급입니다.', '{"minimumPurchaseAmount":"0","pointRate":"1","discountRate":"0"}', 1, TRUE),
  (2, 'MEMBER_GRADE', 'VIP', 'VIP', '누적 구매금액 기준 우수 회원입니다.', '{"minimumPurchaseAmount":"1000000","pointRate":"2","discountRate":"1"}', 2, TRUE),
  (3, 'SHIPPING_ADDRESS', 'SEOUL_CENTER', '서울 물류센터', '상품 입고 시 주문번호와 회원명을 함께 기재합니다.', '{"postalCode":"04524","address1":"서울특별시 중구 세종대로 110","address2":"ZIK00 물류센터","receiverName":"입고담당자","phone":"02-0000-0000"}', 1, TRUE),
  (4, 'DEPOSIT_ACCOUNT', 'MAIN_DEPOSIT', '기본 입금계좌', '입금 후 예치금 신청에서 입금자명을 정확히 입력해 주세요.', '{"bankName":"국민은행","accountNumber":"000000-00-000000","accountHolder":"주식회사 ZIK00","usage":"ALL"}', 1, TRUE),
  (9, 'MAIL_TEMPLATE', 'WELCOME', '회원가입 환영 메일', 'ZIK:00 가입을 환영합니다. 원하는 한국 상품을 더 편리하게 만나보세요.', '{"templateType":"SIGNUP","subject":"ZIK:00 회원가입을 환영합니다","senderName":"ZIK:00","replyTo":"support@zik00.example","defaultTemplate":"true"}', 1, TRUE),
  (10, 'COMPANY_INFO', 'PRIMARY', 'ZIK:00', '운영시간: 평일 10:00-17:00', '{"representative":"대표자","businessNumber":"000-00-00000","commerceNumber":"제2026-서울중구-0000호","phone":"02-0000-0000","email":"support@zik00.example","postalCode":"04524","address":"서울특별시 중구 세종대로 110"}', 1, TRUE)
ON DUPLICATE KEY UPDATE
  name=VALUES(name), content=VALUES(content), field_data=VALUES(field_data),
  display_order=VALUES(display_order), active=VALUES(active);

DELETE FROM admin_setting_entries WHERE setting_type = 'ITEM_CATEGORY';
UPDATE admin_setting_entries
SET setting_type = 'MAIL_TEMPLATE',
    field_data = JSON_SET(field_data, '$.templateType', 'SIGNUP', '$.defaultTemplate', 'true')
WHERE setting_type = 'SIGNUP_MAIL';

CREATE TABLE IF NOT EXISTS notices (
  notice_id BIGINT NOT NULL AUTO_INCREMENT,
  category VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  pinned BOOLEAN NOT NULL DEFAULT FALSE,
  published BOOLEAN NOT NULL DEFAULT TRUE,
  published_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (notice_id),
  KEY idx_notices_public_list (published, pinned, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO service_intro_sections (
  section_id, section_type, eyebrow, title, content, detail, image_url, display_order, active
) VALUES
  (1, 'HERO', 'ZIK:00 SERVICE', '한국 쇼핑을 더 가까이, 주문 과정은 더 분명하게',
   'ZIK:00은 원하는 한국 상품을 찾고 주문한 뒤 배송 과정을 확인할 수 있도록 돕는 구매대행 서비스입니다.',
   '상품 탐색부터 국제배송 안내까지 필요한 정보를 한곳에 모았습니다.',
   '/assets/hero-seoul-summer.webp', 1, TRUE),
  (2, 'PROCESS', '01', '상품 찾기',
   '검색창과 카테고리에서 원하는 상품을 찾거나 상품 URL을 직접 입력합니다.',
   '상품 가격과 선택 가능한 옵션을 확인하세요.', NULL, 2, TRUE),
  (3, 'PROCESS', '02', '주문 요청',
   '상품 옵션과 수량을 선택하고 주문을 요청합니다.',
   '결제 전에 예상 비용과 배송 정보를 확인할 수 있습니다.', NULL, 3, TRUE),
  (4, 'PROCESS', '03', '한국에서 발송',
   '판매처 주문과 국내 입고가 완료되면 검수 후 해외 배송을 준비합니다.',
   '마이페이지에서 진행 상태를 확인할 수 있습니다.', NULL, 4, TRUE),
  (5, 'VALUE', 'CLEAR', '확인 가능한 진행 과정',
   '주문 접수부터 배송까지 현재 단계를 알기 쉽게 안내합니다.',
   '궁금한 점은 주문별 문의에서 이어서 확인할 수 있습니다.', NULL, 5, TRUE),
  (6, 'VALUE', 'CHOICE', '다양한 상품 탐색',
   '검색 결과와 원본 상품 페이지 정보를 바탕으로 필요한 상품을 비교할 수 있습니다.',
   '상품 정보는 판매처 상황에 따라 달라질 수 있습니다.', NULL, 6, TRUE),
  (7, 'VALUE', 'SUPPORT', '주문별 고객 지원',
   '주문이나 배송 중 확인이 필요한 내용은 고객센터를 통해 문의할 수 있습니다.',
   '문의 내역과 답변은 마이페이지에서 관리됩니다.', NULL, 7, TRUE)
ON DUPLICATE KEY UPDATE
  section_type = VALUES(section_type), eyebrow = VALUES(eyebrow), title = VALUES(title),
  content = VALUES(content), detail = VALUES(detail), image_url = VALUES(image_url),
  display_order = VALUES(display_order), active = VALUES(active);

INSERT INTO notices (
  notice_id, category, title, content, pinned, published, published_at
) VALUES
  (1, '안내', 'ZIK:00 서비스 이용 안내',
   'ZIK:00을 이용해 주셔서 감사합니다.\n\n상품 검색 후 상세 페이지에서 가격과 옵션을 확인하고 주문을 진행해 주세요. 판매처의 재고와 가격은 주문 시점에 달라질 수 있습니다.',
   TRUE, TRUE, '2026-08-01 09:00:00'),
  (2, '배송', '국제배송 진행 단계 안내',
   '판매처 주문, 국내 입고, 검수, 국제배송 순서로 진행됩니다. 주문별 진행 상태는 마이페이지에서 확인할 수 있습니다.',
   TRUE, TRUE, '2026-08-03 10:30:00'),
  (3, '점검', '정기 시스템 점검 안내',
   '더 안정적인 서비스를 위해 정기 점검을 진행할 예정입니다. 점검 시간에는 일부 기능 이용이 일시적으로 제한될 수 있습니다.',
   FALSE, TRUE, '2026-08-07 14:00:00'),
  (4, '상품', '상품 가격 및 재고 표시에 관한 안내',
   '상품 가격과 재고는 원본 판매처의 상황에 따라 변경될 수 있습니다. 최종 주문 전 상품 상세 정보를 다시 확인해 주세요.',
   FALSE, TRUE, '2026-08-09 11:20:00'),
  (5, '배송', '배송지 정보 입력 시 유의사항',
   '정확한 배송을 위해 우편번호, 주소, 연락처를 빠짐없이 입력해 주세요. 주소 오류로 인한 배송 지연이 발생할 수 있습니다.',
   FALSE, TRUE, '2026-08-10 16:10:00')
ON DUPLICATE KEY UPDATE
  category = VALUES(category), title = VALUES(title), content = VALUES(content),
  pinned = VALUES(pinned), published = VALUES(published), published_at = VALUES(published_at);

CREATE TABLE IF NOT EXISTS service_reviews (
  review_id BIGINT NOT NULL AUTO_INCREMENT,
  author_name VARCHAR(100) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content LONGTEXT NOT NULL,
  rating INT NOT NULL,
  product_name VARCHAR(200) NOT NULL,
  image_url VARCHAR(500) NULL,
  featured BOOLEAN NOT NULL DEFAULT FALSE,
  published BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (review_id),
  KEY idx_service_reviews_public_list (published, created_at),
  KEY idx_service_reviews_public_rating (published, rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS service_review_comments (
  comment_id BIGINT NOT NULL AUTO_INCREMENT,
  review_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  admin_name VARCHAR(100) NOT NULL,
  content LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (comment_id),
  KEY idx_service_review_comments_review_created (review_id, created_at),
  CONSTRAINT fk_service_review_comments_review
    FOREIGN KEY (review_id) REFERENCES service_reviews (review_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notice_categories (
  category_id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_notice_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO notice_categories (category_id, name, display_order) VALUES
  (1, '배송', 1),
  (2, '상품', 2),
  (3, '안내', 3),
  (4, '점검', 4)
ON DUPLICATE KEY UPDATE name=VALUES(name), display_order=VALUES(display_order);

CREATE TABLE IF NOT EXISTS homepage_contents (
  content_id BIGINT NOT NULL AUTO_INCREMENT,
  content_type VARCHAR(40) NOT NULL,
  title VARCHAR(200) NOT NULL,
  subtitle VARCHAR(300) NULL,
  content LONGTEXT NULL,
  image_url VARCHAR(1000) NULL,
  link_url VARCHAR(1000) NULL,
  link_label VARCHAR(100) NULL,
  application_type VARCHAR(30) NULL,
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  starts_at DATETIME NULL,
  ends_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (content_id),
  KEY idx_homepage_contents_type_order (content_type, active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- noinspection SqlResolve
SET @homepage_application_type_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`COLUMNS`
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'homepage_contents'
    AND COLUMN_NAME = 'application_type'
);
SET @homepage_application_type_sql = IF(
  @homepage_application_type_exists = 0,
  'ALTER TABLE homepage_contents ADD COLUMN application_type VARCHAR(30) NULL AFTER link_label',
  'SELECT 1'
);
PREPARE homepage_application_type_statement FROM @homepage_application_type_sql;
EXECUTE homepage_application_type_statement;
DEALLOCATE PREPARE homepage_application_type_statement;

INSERT INTO homepage_contents
  (content_id, content_type, title, subtitle, content, image_url, link_url, link_label, display_order, active)
VALUES
  (1, 'MAIN_BANNER', '한국의 여름을\n가볍게 즐기는 방법', 'SEOUL SUMMER 2026', '서울에서 지금 뜨는 여름 아이템을 일본까지 만나보세요.', '/assets/hero-seoul-summer.webp', '#recommendations', '기획전 보기', 1, TRUE),
  (2, 'MAIN_BANNER', '오늘의 스타일을\n가볍게 업데이트', 'SEOUL STREET', '스니커즈부터 데일리 아이템까지 빠르게 둘러보세요.', '/assets/hero-style.webp', '#recommendations', '기획전 보기', 2, TRUE),
  (3, 'MAIN_BANNER', '취향을 채우는\n작고 좋은 물건들', 'K-LIFESTYLE', '문구, 리빙, 굿즈를 현지 배송부터 통관까지 편리하게.', '/assets/hero-living.webp', '#recommendations', '기획전 보기', 3, TRUE),
  (4, 'OTHER_BANNER', '검수부터 포장까지', '안전한 배송 대행', NULL, NULL, '/service-intro', NULL, 1, TRUE),
  (5, 'OTHER_BANNER', '비용을 한눈에', '예상 금액 미리 확인', NULL, NULL, '/service-intro', NULL, 2, TRUE),
  (6, 'OTHER_BANNER', '진행 상황 확인', '주문부터 배송까지', NULL, NULL, '/mypage/orders', NULL, 3, TRUE),
  (7, 'FOOTER_COPYRIGHT', '한국의 좋은 상품을 일본까지 편리하게 연결합니다.', '상호명: ZIK:00 · 운영시간: 평일 10:00-17:00', '© 2026 ZIK:00. All rights reserved.', NULL, NULL, NULL, 1, TRUE)
ON DUPLICATE KEY UPDATE
  content_type=VALUES(content_type), title=VALUES(title), subtitle=VALUES(subtitle),
  content=VALUES(content), image_url=VALUES(image_url), link_url=VALUES(link_url),
  link_label=VALUES(link_label), display_order=VALUES(display_order), active=VALUES(active);

INSERT INTO service_reviews (
  review_id, author_name, title, content, rating, product_name, image_url, featured, published, created_at
) VALUES
  (1, '미나***', '처음 이용했는데 진행 상황을 알기 쉬웠어요',
   '찾던 운동화를 주문했습니다. 국내 입고와 국제배송 단계가 구분되어 있어서 기다리는 동안에도 안심할 수 있었어요.',
   5, '나이키 데일리 스니커즈', '/assets/product-shoes.webp', TRUE, TRUE, '2026-08-10 18:20:00'),
  (2, '하루***', '상품 포장이 꼼꼼했습니다',
   '가방 모양이 흐트러지지 않게 포장되어 도착했습니다. 문의 답변도 필요한 내용을 정확하게 알려줬어요.',
   5, '미니 크로스백', '/assets/product-bag.webp', TRUE, TRUE, '2026-08-09 14:40:00'),
  (3, '유키***', '생각보다 배송이 빨랐어요',
   '예상 배송 기간 안에 잘 도착했습니다. 주문 전에 비용을 확인할 수 있어서 편리했습니다.',
   4, '서머 스트랩 샌들', '/assets/product-sandals.webp', FALSE, TRUE, '2026-08-08 09:15:00'),
  (4, '지아***', '한국 한정 상품을 편하게 주문했어요',
   '직접 구매하기 어려웠던 상품을 검색해서 주문할 수 있었습니다. 다음에도 이용할 생각입니다.',
   5, '서울 에디션 볼캡', '/assets/product-summer-cap.webp', FALSE, TRUE, '2026-08-06 20:05:00'),
  (5, '렌***', '문의 답변이 친절했어요',
   '옵션을 잘못 선택해서 문의했는데 주문 전 빠르게 확인해 주셨습니다.',
   4, '데일리 손목시계', '/assets/product-watch.webp', FALSE, TRUE, '2026-08-05 11:50:00'),
  (6, '소라***', '제품 상태가 좋았습니다',
   '외부 포장과 제품 상태 모두 문제없이 도착했습니다. 배송 조회도 편리했어요.',
   5, '무선 헤드폰', '/assets/product-headphones.webp', FALSE, TRUE, '2026-08-03 16:25:00'),
  (7, '나오***', '전체적으로 만족합니다',
   '주문 과정은 편리했지만 인기 상품이라 국내 입고까지 시간이 조금 걸렸습니다.',
   4, '선케어 세트', '/assets/product-suncare.webp', FALSE, TRUE, '2026-08-02 13:10:00'),
  (8, '아키***', '다시 이용하고 싶은 서비스예요',
   '여러 쇼핑몰을 따로 확인하지 않아도 원하는 상품을 찾을 수 있어 좋았습니다.',
   5, '24시간 보냉 텀블러', '/assets/product-tumbler.webp', FALSE, TRUE, '2026-07-31 10:30:00')
ON DUPLICATE KEY UPDATE
  author_name = VALUES(author_name), title = VALUES(title), content = VALUES(content),
  rating = VALUES(rating), product_name = VALUES(product_name), image_url = VALUES(image_url),
  featured = VALUES(featured), published = VALUES(published), created_at = VALUES(created_at);

CREATE TABLE IF NOT EXISTS wishlist_items (
  wishlist_item_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id VARCHAR(255) NOT NULL,
  product_name VARCHAR(500) NOT NULL,
  brand VARCHAR(200) NULL,
  image_url VARCHAR(1500) NULL,
  price BIGINT NOT NULL,
  currency VARCHAR(10) NOT NULL,
  source_url VARCHAR(2000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (wishlist_item_id),
  UNIQUE KEY uk_wishlist_user_product (user_id, product_id),
  KEY idx_wishlist_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
  cart_item_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id VARCHAR(255) NOT NULL,
  product_name VARCHAR(500) NOT NULL,
  brand VARCHAR(200) NULL,
  image_url VARCHAR(1500) NULL,
  unit_price BIGINT NOT NULL,
  currency VARCHAR(10) NOT NULL,
  source_url VARCHAR(2000) NULL,
  option_data LONGTEXT NOT NULL,
  option_key VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (cart_item_id),
  UNIQUE KEY uk_cart_user_product_option (user_id, product_id, option_key),
  KEY idx_cart_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS japan_customs_snapshots (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
  krw_to_jpy_rate DECIMAL(12,6) NULL,
  rate_from DATE NULL,
  rate_to DATE NULL,
  simplified_tariff_rates LONGTEXT NOT NULL,
  consumption_tax_rate DECIMAL(8,6) NOT NULL,
  exchange_source_url VARCHAR(1000) NOT NULL,
  tariff_source_url VARCHAR(1000) NOT NULL,
  fetched_at DATETIME(6) NOT NULL,
  fallback BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (snapshot_id),
  KEY idx_japan_customs_fetched (fallback, fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
