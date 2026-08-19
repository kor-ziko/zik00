package com.zik00.shop.service.product.pricing;

import com.zik00.shop.domain.product.pricing.JapanCustomsSnapshotEntity;
import com.zik00.shop.repository.product.pricing.JapanCustomsSnapshotRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class JapanCustomsSnapshotHistoryStore {
    private static final BigDecimal DEFAULT_CONSUMPTION_TAX_RATE = new BigDecimal("0.10");

    private final JapanCustomsSnapshotRepository repository;
    private final ObjectMapper objectMapper;
    private final String exchangeSourceUrl;
    private final String tariffSourceUrl;

    public JapanCustomsSnapshotHistoryStore(
            JapanCustomsSnapshotRepository repository,
            ObjectMapper objectMapper,
            @Value("${shop.customs.japan.exchange-index-url:https://www.customs.go.jp/english/kawase/index_e.htm}")
            String exchangeSourceUrl,
            @Value("${shop.customs.japan.simplified-tariff-url:https://www.customs.go.jp/english/c-answer_e/imtsukan/1001_e.htm}")
            String tariffSourceUrl
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.exchangeSourceUrl = exchangeSourceUrl;
        this.tariffSourceUrl = tariffSourceUrl;
    }

    @Transactional
    public void save(JapanCustomsSnapshot snapshot) {
        try {
            repository.save(new JapanCustomsSnapshotEntity(
                    snapshot.krwToJpyRate(), snapshot.rateFrom(), snapshot.rateTo(),
                    objectMapper.writeValueAsString(snapshot.simplifiedTariffRates()),
                    taxRate(snapshot), exchangeSourceUrl, tariffSourceUrl,
                    snapshot.fetchedAt(), snapshot.fallback()
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("일본 세관 기준 이력을 변환하지 못했습니다.", exception);
        }
    }

    @Transactional(readOnly = true)
    public Optional<JapanCustomsSnapshot> findLatestOfficial() {
        return repository.findTopByFallbackFalseOrderByFetchedAtDesc().map(entity -> {
            try {
                Map<String, BigDecimal> rates = objectMapper.readValue(
                        entity.getSimplifiedTariffRates(), new TypeReference<Map<String, BigDecimal>>() {}
                );
                return new JapanCustomsSnapshot(
                        entity.getKrwToJpyRate(), entity.getRateFrom(), entity.getRateTo(), rates,
                        entity.getConsumptionTaxRate(), entity.getFetchedAt(), entity.isFallback()
                );
            } catch (JacksonException exception) {
                throw new IllegalStateException("저장된 일본 세관 기준 이력을 읽지 못했습니다.", exception);
            }
        });
    }

    @Transactional
    public void saveIfEmpty(JapanCustomsSnapshot snapshot) {
        if (!snapshot.fallback() && repository.findTopByFallbackFalseOrderByFetchedAtDesc().isEmpty()) {
            save(snapshot);
        }
    }

    private BigDecimal taxRate(JapanCustomsSnapshot snapshot) {
        return snapshot.consumptionTaxRate() == null
                ? DEFAULT_CONSUMPTION_TAX_RATE : snapshot.consumptionTaxRate();
    }
}
