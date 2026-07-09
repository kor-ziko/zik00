CREATE DATABASE IF NOT EXISTS shop
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE shop;

SET @demo_user_id = 1;
SET @tester1_user_id = 2;
SET @tester2_user_id = 3;
SET @tester3_user_id = 4;

CREATE TABLE IF NOT EXISTS `user` (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100),
  birth_date DATE,
  gender VARCHAR(20),
  nickname VARCHAR(100),
  telephone VARCHAR(50),
  login_id VARCHAR(100),
  deposit_balance INT NOT NULL DEFAULT 0,
  reward_point INT NOT NULL DEFAULT 0,
  mobile_phone VARCHAR(50),
  email VARCHAR(255),
  completed_order_count INT NOT NULL DEFAULT 0,
  joined_date DATE,
  member_detail VARCHAR(500),
  alarm_consent BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @drop_user_zip_code = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `user` DROP COLUMN zip_code', 'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'zip_code'
);
PREPARE drop_user_zip_code_stmt FROM @drop_user_zip_code;
EXECUTE drop_user_zip_code_stmt;
DEALLOCATE PREPARE drop_user_zip_code_stmt;

SET @drop_user_province = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `user` DROP COLUMN province', 'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'province'
);
PREPARE drop_user_province_stmt FROM @drop_user_province;
EXECUTE drop_user_province_stmt;
DEALLOCATE PREPARE drop_user_province_stmt;

SET @drop_user_detail_address = (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `user` DROP COLUMN detail_address', 'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'detail_address'
);
PREPARE drop_user_detail_address_stmt FROM @drop_user_detail_address;
EXECUTE drop_user_detail_address_stmt;
DEALLOCATE PREPARE drop_user_detail_address_stmt;

CREATE TABLE IF NOT EXISTS addresses (
  address_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  address_name VARCHAR(100),
  receiver_name VARCHAR(100),
  receiver_phone VARCHAR(50),
  zip_code VARCHAR(20),
  province VARCHAR(100),
  detail_address VARCHAR(255),
  default_address BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (address_id),
  KEY idx_addresses_member_default (user_id, default_address, address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupon (
  coupon_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_name VARCHAR(100),
  discount_type VARCHAR(50),
  discount_value INT NOT NULL DEFAULT 0,
  minimum_order_amount INT NOT NULL DEFAULT 0,
  expired_date DATE,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (coupon_id),
  KEY idx_coupon_member_expired (user_id, expired_date, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS buylist (
  purchase_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_number VARCHAR(100),
  product_name VARCHAR(100),
  quantity INT NOT NULL DEFAULT 0,
  payment_amount INT NOT NULL DEFAULT 0,
  order_status VARCHAR(50),
  ordered_date DATE,
  PRIMARY KEY (purchase_id),
  KEY idx_buylist_member_ordered (user_id, ordered_date, purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiries (
  inquiry_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255),
  content LONGTEXT,
  status VARCHAR(50),
  created_at VARCHAR(50),
  PRIMARY KEY (inquiry_id),
  KEY idx_inquiries_member_id (user_id, inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inquiry_comments (
  comment_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  writer_name VARCHAR(100),
  content LONGTEXT,
  created_at VARCHAR(50),
  PRIMARY KEY (comment_id),
  KEY idx_inquiry_comments_inquiry_comment (inquiry_id, comment_id)
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
  login_id VARCHAR(100) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL,
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
  name,
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
  '김테스트',
  '1990-05-14',
  '남성',
  '테스터01',
  '02-1234-5678',
  'testuser01',
  50000,
  1200,
  '010-1234-5678',
  'testuser01@example.com',
  3,
  '2026-07-07',
  '테스트용 일반회원, 연락처/주문 이력 포함',
  FALSE
) ON DUPLICATE KEY UPDATE
  name = VALUES(name),
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
  name,
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
    '테스터1',
    '1995-01-01',
    '남성',
    '테스터1',
    '02-1000-0001',
    'tester1',
    10000,
    100,
    '010-1000-0001',
    'tester1@example.com',
    1,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 1',
    TRUE
  ),
  (
    @tester2_user_id,
    '테스터2',
    '1996-02-02',
    '여성',
    '테스터2',
    '02-1000-0002',
    'tester2',
    20000,
    200,
    '010-1000-0002',
    'tester2@example.com',
    2,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 2',
    FALSE
  ),
  (
    @tester3_user_id,
    '테스터3',
    '1997-03-03',
    '기타',
    '테스터3',
    '02-1000-0003',
    'tester3',
    30000,
    300,
    '010-1000-0003',
    'tester3@example.com',
    3,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 3',
    TRUE
  )
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
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
  '집',
  '김테스트',
  '010-1234-5678',
  '100-0005',
  '東京都',
  '千代田区丸の内 113232',
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

INSERT INTO coupon (
  coupon_id,
  user_id,
  coupon_name,
  discount_type,
  discount_value,
  minimum_order_amount,
  expired_date,
  used
) VALUES
  (1, @demo_user_id, '신규회원 10% 할인', 'percent', 10, 10000, '2026-12-31', FALSE),
  (2, @demo_user_id, '무료배송 쿠폰', 'shipping', 3000, 0, '2026-09-30', FALSE),
  (3, @tester1_user_id, '테스터1 쿠폰', 'amount', 5000, 30000, '2026-08-31', FALSE)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  coupon_name = VALUES(coupon_name),
  discount_type = VALUES(discount_type),
  discount_value = VALUES(discount_value),
  minimum_order_amount = VALUES(minimum_order_amount),
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
  (1, @demo_user_id, '배송은 언제 시작되나요?', '어제 주문했는데 배송 상태가 궁금합니다.', '답변대기', '2026-07-07 16:20'),
  (2, @demo_user_id, '1', '1', '답변대기', '2026-07-07 18:55')
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
  writer_name,
  content,
  created_at
) VALUES
  (1, 1, @demo_user_id, '테스터01', '확인 부탁드립니다.', '2026-07-07 16:22'),
  (2, 1, @demo_user_id, '테스터01', '11', '2026-07-07 18:55')
ON DUPLICATE KEY UPDATE
  inquiry_id = VALUES(inquiry_id),
  user_id = VALUES(user_id),
  writer_name = VALUES(writer_name),
  content = VALUES(content),
  created_at = VALUES(created_at);

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
