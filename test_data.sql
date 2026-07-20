-- @Suil -
-- 1. v1, v2 sql 지우고 여기에 통합
-- 2. 1대1 답변처리 로직 수정 line 112
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
  access_id VARCHAR(36) NOT NULL,
  name VARCHAR(100),
  name_kana VARCHAR(100),
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
  member_detail VARCHAR(500) NOT NULL DEFAULT '일반회원',
  alarm_consent BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_access_id (access_id),
  UNIQUE KEY uk_user_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //
DROP PROCEDURE IF EXISTS add_google_oauth_user_columns//
CREATE PROCEDURE add_google_oauth_user_columns()
BEGIN
  DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;
  ALTER TABLE `user` ADD COLUMN name_kana VARCHAR(100) AFTER name;
END//
DELIMITER ;
CALL add_google_oauth_user_columns();
DROP PROCEDURE add_google_oauth_user_columns;

DELIMITER //
DROP PROCEDURE IF EXISTS add_user_access_id//
CREATE PROCEDURE add_user_access_id()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'access_id'
  ) THEN
    ALTER TABLE `user` ADD COLUMN access_id VARCHAR(36) NULL AFTER user_id;
  END IF;
  UPDATE `user` SET access_id = UUID() WHERE access_id IS NULL OR access_id = '';
  ALTER TABLE `user` MODIFY COLUMN access_id VARCHAR(36) NOT NULL;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_access_id'
  ) THEN
    ALTER TABLE `user` ADD UNIQUE KEY uk_user_access_id (access_id);
  END IF;
END//
DELIMITER ;
CALL add_user_access_id();
DROP PROCEDURE add_user_access_id;

DELIMITER //
DROP PROCEDURE IF EXISTS normalize_user_profile_columns//
CREATE PROCEDURE normalize_user_profile_columns()
BEGIN
  UPDATE `user`
  SET gender = CASE gender
    WHEN 'MALE' THEN '남자'
    WHEN '남성' THEN '남자'
    WHEN 'FEMALE' THEN '여자'
    WHEN '여성' THEN '여자'
    WHEN 'OTHER' THEN '기타'
    ELSE gender
  END;
  UPDATE `user` SET member_detail = '일반회원'
  WHERE member_detail IS NULL OR TRIM(member_detail) = '';
  UPDATE `user` SET login_id = NULL
  WHERE login_id IS NOT NULL AND TRIM(login_id) = '';

  ALTER TABLE `user`
    MODIFY COLUMN name VARCHAR(100) NULL,
    MODIFY COLUMN name_kana VARCHAR(100) NULL,
    MODIFY COLUMN gender VARCHAR(20) NULL,
    MODIFY COLUMN nickname VARCHAR(100) NULL,
    MODIFY COLUMN telephone VARCHAR(50) NULL,
    MODIFY COLUMN login_id VARCHAR(100) NULL,
    MODIFY COLUMN mobile_phone VARCHAR(50) NULL,
    MODIFY COLUMN member_detail VARCHAR(500) NOT NULL DEFAULT '일반회원';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_login_id'
  ) AND NOT EXISTS (
    SELECT 1
    FROM (SELECT login_id FROM `user` WHERE login_id IS NOT NULL GROUP BY login_id HAVING COUNT(*) > 1) duplicate_login_ids
  ) THEN
    ALTER TABLE `user` ADD UNIQUE KEY uk_user_login_id (login_id);
  END IF;
END//
DELIMITER ;
CALL normalize_user_profile_columns();
DROP PROCEDURE normalize_user_profile_columns;

-- Google Access Token은 로그인 판별에 사용하지 않으므로 DB에 저장하지 않는다.
DROP TABLE IF EXISTS oauth_access_tokens;

-- Refresh Token 상태는 Redis TTL 키로 관리한다.
DROP TABLE IF EXISTS refresh_tokens;

DELIMITER //
CREATE PROCEDURE drop_legacy_user_address_columns()
BEGIN
  DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
  SET @drop_legacy_column_sql = 'ALTER TABLE `user` DROP COLUMN zip_code';
  PREPARE drop_legacy_column_stmt FROM @drop_legacy_column_sql;
  EXECUTE drop_legacy_column_stmt;
  DEALLOCATE PREPARE drop_legacy_column_stmt;

  SET @drop_legacy_column_sql = 'ALTER TABLE `user` DROP COLUMN province';
  PREPARE drop_legacy_column_stmt FROM @drop_legacy_column_sql;
  EXECUTE drop_legacy_column_stmt;
  DEALLOCATE PREPARE drop_legacy_column_stmt;

  SET @drop_legacy_column_sql = 'ALTER TABLE `user` DROP COLUMN detail_address';
  PREPARE drop_legacy_column_stmt FROM @drop_legacy_column_sql;
  EXECUTE drop_legacy_column_stmt;
  DEALLOCATE PREPARE drop_legacy_column_stmt;
END//
DELIMITER ;
CALL drop_legacy_user_address_columns();
DROP PROCEDURE drop_legacy_user_address_columns;

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
  coupon_template_id BIGINT NULL,
  user_id BIGINT NULL,
  coupon_name VARCHAR(100),
  discount_type VARCHAR(50),
  discount_value INT NOT NULL DEFAULT 0,
  minimum_order_amount INT NOT NULL DEFAULT 0,
  started_date DATE,
  expired_date DATE,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  used_at DATETIME NULL,
  coupon_code VARCHAR(100) NULL,
  guest_identifier VARCHAR(255) NULL,
  PRIMARY KEY (coupon_id),
  KEY idx_coupon_template_id (coupon_template_id),
  KEY idx_coupon_member_period (user_id, started_date, expired_date, coupon_id),
  KEY idx_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupon_template (
  coupon_template_id BIGINT NOT NULL AUTO_INCREMENT,
  coupon_name VARCHAR(100) NOT NULL,
  discount_type VARCHAR(50) NOT NULL,
  discount_value INT NOT NULL DEFAULT 0,
  minimum_order_amount INT NOT NULL DEFAULT 0,
  started_date DATE,
  expired_date DATE,
  target_type VARCHAR(30) NOT NULL DEFAULT 'ALL',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (coupon_template_id),
  KEY idx_coupon_template_active_period (active, started_date, expired_date, coupon_template_id)
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
  status BOOLEAN NOT NULL DEFAULT FALSE,
  created_at VARCHAR(50),
  PRIMARY KEY (inquiry_id),
  KEY idx_inquiries_member_id (user_id, inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 DB의 문자열 문의 상태를 boolean 답변 상태로 변환
DELIMITER //
CREATE PROCEDURE normalize_inquiry_status_column()
BEGIN
  DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
  UPDATE inquiries
  SET status = CASE
    WHEN status IN ('1', 'true', 'TRUE', '답변완료', 'ANSWERED', 'answered') THEN 1
    ELSE 0
  END
  WHERE inquiry_id > 0;
  ALTER TABLE inquiries MODIFY COLUMN status BOOLEAN NOT NULL DEFAULT FALSE;
END//
DELIMITER ;
CALL normalize_inquiry_status_column();
DROP PROCEDURE normalize_inquiry_status_column;

CREATE TABLE IF NOT EXISTS inquiry_comments (
  comment_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  user_id BIGINT,
  admin_id BIGINT,
  writer_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  writer_name VARCHAR(100),
  content LONGTEXT,
  created_at VARCHAR(50),
  PRIMARY KEY (comment_id),
  KEY idx_inquiry_comments_inquiry_comment (inquiry_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- @Suil - 기존 DB에도 관리자 문의 답변 작성자 컬럼을 추가
DELIMITER //
CREATE PROCEDURE add_inquiry_comment_writer_columns()
BEGIN
  DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
  ALTER TABLE inquiry_comments ADD COLUMN admin_id BIGINT NULL AFTER user_id;
  ALTER TABLE inquiry_comments ADD COLUMN writer_type VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER admin_id;
END//
DELIMITER ;
CALL add_inquiry_comment_writer_columns();
DROP PROCEDURE add_inquiry_comment_writer_columns;

ALTER TABLE inquiry_comments MODIFY COLUMN user_id BIGINT NULL;
UPDATE inquiry_comments
SET writer_type = 'USER'
WHERE comment_id > 0
  AND (writer_type IS NULL OR writer_type = '');

-- @Suil - 관리자 문의 답변에 첨부한 사진을 댓글과 연결
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
  '00000000-0000-0000-0000-000000000001',
  '김테스트',
  'キム テスト',
  '1990-05-14',
  '남자',
  '테스터01',
  '02-1234-5678',
  'testuser01',
  50000,
  1200,
  '090-1234-5678',
  'testuser01@example.com',
  3,
  '2026-07-07',
  '테스트용 일반회원, 연락처/주문 이력 포함',
  FALSE
) ON DUPLICATE KEY UPDATE
  access_id = VALUES(access_id),
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
    '00000000-0000-0000-0000-000000000002',
    '테스터1',
    'テスター イチ',
    '1995-01-01',
    '남자',
    '테스터1',
    '02-1000-0001',
    'tester1',
    10000,
    100,
    '090-1000-0001',
    'tester1@example.com',
    1,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 1',
    TRUE
  ),
  (
    @tester2_user_id,
    '00000000-0000-0000-0000-000000000003',
    '테스터2',
    'テスター ニ',
    '1996-02-02',
    '여자',
    '테스터2',
    '02-1000-0002',
    'tester2',
    20000,
    200,
    '090-1000-0002',
    'tester2@example.com',
    2,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 2',
    FALSE
  ),
  (
    @tester3_user_id,
    '00000000-0000-0000-0000-000000000004',
    '테스터3',
    'テスター サン',
    '1997-03-03',
    '기타',
    '테스터3',
    '02-1000-0003',
    'tester3',
    30000,
    300,
    '090-1000-0003',
    'tester3@example.com',
    3,
    '2026-07-08',
    '관리자 회원관리 테스트용 회원 3',
    TRUE
  )
ON DUPLICATE KEY UPDATE
  access_id = VALUES(access_id),
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
  '090-1234-5678',
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
