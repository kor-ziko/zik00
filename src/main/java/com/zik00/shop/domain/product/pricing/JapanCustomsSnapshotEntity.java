package com.zik00.shop.domain.product.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "japan_customs_snapshots")
public class JapanCustomsSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id;

    @Column(name = "krw_to_jpy_rate", precision = 12, scale = 6)
    private BigDecimal krwToJpyRate;

    @Column(name = "rate_from")
    private LocalDate rateFrom;

    @Column(name = "rate_to")
    private LocalDate rateTo;

    @Column(name = "simplified_tariff_rates", nullable = false, columnDefinition = "LONGTEXT")
    private String simplifiedTariffRates;

    @Column(name = "consumption_tax_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal consumptionTaxRate;

    @Column(name = "exchange_source_url", nullable = false, length = 1000)
    private String exchangeSourceUrl;

    @Column(name = "tariff_source_url", nullable = false, length = 1000)
    private String tariffSourceUrl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(nullable = false)
    private boolean fallback;

    protected JapanCustomsSnapshotEntity() {}

    public JapanCustomsSnapshotEntity(
            BigDecimal krwToJpyRate,
            LocalDate rateFrom,
            LocalDate rateTo,
            String simplifiedTariffRates,
            BigDecimal consumptionTaxRate,
            String exchangeSourceUrl,
            String tariffSourceUrl,
            Instant fetchedAt,
            boolean fallback
    ) {
        this.krwToJpyRate = krwToJpyRate;
        this.rateFrom = rateFrom;
        this.rateTo = rateTo;
        this.simplifiedTariffRates = simplifiedTariffRates;
        this.consumptionTaxRate = consumptionTaxRate;
        this.exchangeSourceUrl = exchangeSourceUrl;
        this.tariffSourceUrl = tariffSourceUrl;
        this.fetchedAt = fetchedAt;
        this.fallback = fallback;
    }
}
