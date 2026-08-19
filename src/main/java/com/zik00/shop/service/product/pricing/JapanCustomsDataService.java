package com.zik00.shop.service.product.pricing;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JapanCustomsDataService {
    private static final Logger log = LoggerFactory.getLogger(JapanCustomsDataService.class);
    private static final Duration STALE_AFTER = Duration.ofHours(36);

    private final JapanCustomsSnapshotStore store;
    private final JapanCustomsSnapshotHistoryStore historyStore;
    private final OfficialJapanCustomsClient client;
    private volatile JapanCustomsSnapshot lastKnownSnapshot;

    public JapanCustomsDataService(
            JapanCustomsSnapshotStore store,
            JapanCustomsSnapshotHistoryStore historyStore,
            OfficialJapanCustomsClient client
    ) {
        this.store = store;
        this.historyStore = historyStore;
        this.client = client;
    }

    public SnapshotView current() {
        JapanCustomsSnapshot snapshot = loadStored();
        if (snapshot == null) snapshot = refreshNow();
        if (snapshot == null) snapshot = lastKnownSnapshot;
        if (snapshot == null) snapshot = client.fallback();
        boolean stale = snapshot.fallback()
                || snapshot.fetchedAt().isBefore(Instant.now().minus(STALE_AFTER));
        return new SnapshotView(snapshot, stale);
    }

    @Scheduled(
            initialDelayString = "${shop.customs.japan.initial-refresh-delay:PT1M}",
            fixedDelayString = "${shop.customs.japan.refresh-interval:PT24H}"
    )
    public void refreshDaily() {
        refreshNow();
    }

    private synchronized JapanCustomsSnapshot refreshNow() {
        try {
            JapanCustomsSnapshot snapshot = client.refresh();
            lastKnownSnapshot = snapshot;
            try {
                historyStore.save(snapshot);
            } catch (RuntimeException exception) {
                log.warn("일본 세관 자료를 DB 이력에 저장하지 못했습니다: {}", exception.getMessage());
            }
            try {
                store.save(snapshot);
            } catch (RuntimeException exception) {
                log.warn("일본 세관 자료를 Redis에 저장하지 못해 실행 중 메모리에 유지합니다: {}", exception.getMessage());
            }
            return snapshot;
        } catch (RuntimeException exception) {
            log.warn("일본 세관 일일 자료 갱신에 실패해 마지막 정상 자료를 유지합니다: {}", exception.getMessage());
            return lastKnownSnapshot;
        }
    }

    private JapanCustomsSnapshot loadStored() {
        try {
            JapanCustomsSnapshot snapshot = store.find().orElse(null);
            if (snapshot != null) historyStore.saveIfEmpty(snapshot);
            if (snapshot == null) snapshot = historyStore.findLatestOfficial().orElse(null);
            if (snapshot != null) lastKnownSnapshot = snapshot;
            return snapshot;
        } catch (RuntimeException exception) {
            log.warn("저장소에서 일본 세관 자료를 읽지 못해 실행 중 자료를 사용합니다: {}", exception.getMessage());
            return lastKnownSnapshot;
        }
    }

    public record SnapshotView(JapanCustomsSnapshot snapshot, boolean stale) {}
}
