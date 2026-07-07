CREATE DATABASE IF NOT EXISTS shop
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE shop;

CREATE TABLE IF NOT EXISTS `user` (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100),
  birth_date DATE,
  gender VARCHAR(20),
  nickname VARCHAR(100),
  zip_code VARCHAR(20),
  province VARCHAR(100),
  detail_address VARCHAR(255),
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
  content TEXT,
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
  content TEXT,
  created_at VARCHAR(50),
  PRIMARY KEY (comment_id),
  KEY idx_inquiry_comments_inquiry_comment (inquiry_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `user` (
  user_id,
  name,
  birth_date,
  gender,
  nickname,
  zip_code,
  province,
  detail_address,
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
  1,
  '김테스트',
  '1990-05-14',
  '남성',
  '테스터01',
  '06236',
  '서울특별시11',
  '강남구 테헤란로 123, 101동 1001호',
  '02-1234-5678',
  'testuser01',
  50000,
  1200,
  '010-1234-5678',
  'testuser01@example.com',
  3,
  '2026-07-07',
  '테스트용 일반회원, 주소/연락처/주문 이력 포함',
  FALSE
) ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  birth_date = VALUES(birth_date),
  gender = VALUES(gender),
  nickname = VALUES(nickname),
  zip_code = VALUES(zip_code),
  province = VALUES(province),
  detail_address = VALUES(detail_address),
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
  1,
  '집',
  '김테스트',
  '010-1234-5678',
  '06236',
  '서울특별시',
  '강남구 테헤란로 123, 101동 1001호',
  TRUE
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
  (1, 1, '신규회원 10% 할인', 'percent', 10, 10000, '2026-12-31', FALSE),
  (2, 1, '무료배송 쿠폰', 'shipping', 3000, 0, '2026-09-30', FALSE),
  (3, 2, '다른회원 쿠폰', 'amount', 5000, 30000, '2026-08-31', FALSE)
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
  (1, 1, 'ORD-20260701-001', '린넨 셔츠', 2, 59000, '주문완료', '2026-07-01'),
  (2, 1, 'ORD-20260703-002', '데님 팬츠', 1, 79000, '배송중', '2026-07-03'),
  (3, 2, 'ORD-20260704-003', '다른회원 테스트상품', 1, 30000, '주문완료', '2026-07-04')
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
  (1, 1, '배송은 언제 시작되나요?', '어제 주문했는데 배송 상태가 궁금합니다.', '답변대기', '2026-07-07 16:20'),
  (2, 1, '1', '1', '답변대기', '2026-07-07 18:55')
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
  (1, 1, 1, '테스터01', '확인 부탁드립니다.', '2026-07-07 16:22'),
  (2, 1, 1, '테스터01', '11', '2026-07-07 18:55')
ON DUPLICATE KEY UPDATE
  inquiry_id = VALUES(inquiry_id),
  user_id = VALUES(user_id),
  writer_name = VALUES(writer_name),
  content = VALUES(content),
  created_at = VALUES(created_at);
