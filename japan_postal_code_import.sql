USE shop;

CREATE TABLE IF NOT EXISTS japan_postal_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  postal_code VARCHAR(7) NOT NULL,
  prefecture VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  town VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_japan_postal_codes_code (postal_code),
  UNIQUE KEY uk_japan_postal_codes_address (postal_code, prefecture, city, town)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS japan_postal_code_import;

CREATE TABLE japan_postal_code_import (
  jis_code VARCHAR(10),
  old_postal_code VARCHAR(5),
  postal_code VARCHAR(7),
  prefecture_kana VARCHAR(255),
  city_kana VARCHAR(255),
  town_kana VARCHAR(255),
  prefecture VARCHAR(100),
  city VARCHAR(100),
  town VARCHAR(255),
  has_multiple_postal_codes TINYINT,
  has_koaza_banchi TINYINT,
  has_chome TINYINT,
  has_multiple_towns TINYINT,
  update_status TINYINT,
  update_reason TINYINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1. 일본우편 사이트에서 KEN_ALL.CSV를 다운로드하고 압축을 풉니다.
--    https://www.post.japanpost.jp/zipcode/download.html
--
-- 2. 아래 경로를 실제 KEN_ALL.CSV 위치로 바꾼 뒤 실행합니다.
--    MySQL에서 LOCAL INFILE이 꺼져 있으면 local_infile 설정을 켜야 합니다.
--
-- LOAD DATA LOCAL INFILE 'C:/Users/user/Desktop/zik/shop/KEN_ALL.CSV'
-- INTO TABLE japan_postal_code_import
-- CHARACTER SET cp932
-- FIELDS TERMINATED BY ','
-- OPTIONALLY ENCLOSED BY '"'
-- LINES TERMINATED BY '\r\n';
--
-- 3. import 테이블 데이터를 앱에서 쓰는 조회 테이블로 옮깁니다.

INSERT INTO japan_postal_codes (
  postal_code,
  prefecture,
  city,
  town
)
SELECT DISTINCT
  postal_code,
  prefecture,
  city,
  town
FROM japan_postal_code_import
WHERE postal_code IS NOT NULL
  AND postal_code <> ''
  AND prefecture IS NOT NULL
  AND city IS NOT NULL
  AND town IS NOT NULL
ON DUPLICATE KEY UPDATE
  prefecture = VALUES(prefecture),
  city = VALUES(city),
  town = VALUES(town);
