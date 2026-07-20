-- 현재 Spring Boot JPA 엔티티 기준 통합 스키마와 개발용 시드 데이터
-- 개인정보 시드 값은 애플리케이션 시작 시 PII_MIGRATE_PLAINTEXT_ON_STARTUP=true로 암호화한다.
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
  alarm_consent BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_access_id (access_id),
  UNIQUE KEY uk_user_login_id (login_id)
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
    UUID(),
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
    UUID(),
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
