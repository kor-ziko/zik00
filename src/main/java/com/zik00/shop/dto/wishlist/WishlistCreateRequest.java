package com.zik00.shop.dto.wishlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record WishlistCreateRequest(
        @NotBlank @Size(max = 255) String productId,
        @NotBlank @Size(max = 500) String productName,
        @Size(max = 200) String brand,
        @Size(max = 1500) String imageUrl,
        @PositiveOrZero long price,
        @NotBlank @Size(max = 10) String currency,
        @Size(max = 2000) String sourceUrl
) {
}
