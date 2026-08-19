package com.zik00.shop.repository.cart;

import com.zik00.shop.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdOrderByCreatedAtDescIdDesc(long userId);

    Optional<CartItem> findByUserIdAndProductIdAndOptionKey(long userId, String productId, String optionKey);

    Optional<CartItem> findByIdAndUserId(long id, long userId);

    List<CartItem> findByIdInAndUserId(Collection<Long> ids, long userId);

    @Query("select coalesce(sum(c.quantity), 0) from CartItem c where c.userId = :userId")
    long countQuantityByUserId(@Param("userId") long userId);
}
