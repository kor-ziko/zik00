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
