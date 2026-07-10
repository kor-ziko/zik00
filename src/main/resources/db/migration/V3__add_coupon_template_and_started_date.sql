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

ALTER TABLE coupon
  ADD COLUMN coupon_template_id BIGINT NULL AFTER coupon_id,
  ADD COLUMN started_date DATE AFTER minimum_order_amount,
  ADD COLUMN issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER expired_date,
  ADD COLUMN used_at DATETIME NULL AFTER used,
  ADD COLUMN coupon_code VARCHAR(100) NULL AFTER used_at,
  ADD COLUMN guest_identifier VARCHAR(255) NULL AFTER coupon_code;

ALTER TABLE coupon
  MODIFY COLUMN user_id BIGINT NULL;

CREATE INDEX idx_coupon_template_id ON coupon (coupon_template_id);
CREATE INDEX idx_coupon_member_period ON coupon (user_id, started_date, expired_date, coupon_id);
CREATE INDEX idx_coupon_code ON coupon (coupon_code);
