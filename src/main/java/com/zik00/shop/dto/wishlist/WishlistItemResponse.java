package com.zik00.shop.dto.wishlist;

import com.zik00.shop.domain.wishlist.WishlistItem;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        long id, String productId, String productName, String brand, String imageUrl,
        long price, String currency, String sourceUrl, LocalDateTime createdAt
) {
    public static WishlistItemResponse from(WishlistItem item) {
        return new WishlistItemResponse(item.getId(), item.getProductId(), item.getProductName(),
                item.getBrand(), item.getImageUrl(), item.getPrice(), item.getCurrency(),
                item.getSourceUrl(), item.getCreatedAt());
    }
}
