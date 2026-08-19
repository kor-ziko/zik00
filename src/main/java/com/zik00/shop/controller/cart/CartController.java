package com.zik00.shop.controller.cart;

import com.zik00.shop.dto.cart.CartCreateRequest;
import com.zik00.shop.dto.cart.CartItemResponse;
import com.zik00.shop.dto.cart.CartQuantityUpdateRequest;
import com.zik00.shop.dto.cart.CartResponse;
import com.zik00.shop.service.cart.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse findCart() {
        return cartService.findCart();
    }

    @GetMapping("/count")
    public long countItems() {
        return cartService.countItems();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse add(@Valid @RequestBody CartCreateRequest request) {
        return cartService.add(request);
    }

    @PatchMapping("/{itemId}")
    public CartItemResponse changeQuantity(@PathVariable long itemId,
                                           @Valid @RequestBody CartQuantityUpdateRequest request) {
        return cartService.changeQuantity(itemId, request.quantity());
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable long itemId) {
        cartService.remove(itemId);
    }
}
