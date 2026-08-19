package com.zik00.shop.repository.wishlist;

import com.zik00.shop.domain.wishlist.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(long userId);

    Optional<WishlistItem> findByUserIdAndProductId(long userId, String productId);

    boolean existsByUserIdAndProductId(long userId, String productId);

    long countByUserId(long userId);

    void deleteByUserIdAndProductId(long userId, String productId);
}
