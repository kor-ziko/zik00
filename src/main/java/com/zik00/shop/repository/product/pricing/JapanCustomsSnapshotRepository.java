package com.zik00.shop.repository.product.pricing;

import com.zik00.shop.domain.product.pricing.JapanCustomsSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JapanCustomsSnapshotRepository extends JpaRepository<JapanCustomsSnapshotEntity, Long> {
    Optional<JapanCustomsSnapshotEntity> findTopByFallbackFalseOrderByFetchedAtDesc();
}
