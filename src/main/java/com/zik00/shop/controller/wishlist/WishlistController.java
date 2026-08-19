package com.zik00.shop.controller.wishlist;

import com.zik00.shop.dto.wishlist.WishlistCreateRequest;
import com.zik00.shop.dto.wishlist.WishlistItemResponse;
import com.zik00.shop.dto.wishlist.WishlistStatusResponse;
import com.zik00.shop.service.wishlist.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<WishlistItemResponse> findItems() {
        return wishlistService.findItems();
    }

    @GetMapping("/count")
    public long countItems() {
        return wishlistService.countItems();
    }

    @GetMapping("/{productId}/status")
    public WishlistStatusResponse status(@PathVariable String productId) {
        return new WishlistStatusResponse(wishlistService.isWished(productId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistItemResponse add(@Valid @RequestBody WishlistCreateRequest request) {
        return wishlistService.add(request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String productId) {
        wishlistService.remove(productId);
    }
}
