package com.zik00.shop.service.cart;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.cart.CartItem;
import com.zik00.shop.dto.cart.CartCreateRequest;
import com.zik00.shop.dto.cart.CartItemResponse;
import com.zik00.shop.dto.cart.CartResponse;
import com.zik00.shop.repository.cart.CartRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class CartService {
    private final AuthenticatedUserService authenticatedUserService;
    private final CartRepository repository;
    private final ObjectMapper objectMapper;

    public CartService(AuthenticatedUserService authenticatedUserService,
                       CartRepository repository, ObjectMapper objectMapper) {
        this.authenticatedUserService = authenticatedUserService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public CartResponse findCart() {
        long userId = currentUserId();
        return new CartResponse(
                repository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(this::toResponse).toList(),
                repository.countQuantityByUserId(userId)
        );
    }

    public long countItems() {
        return repository.countQuantityByUserId(currentUserId());
    }

    @Transactional
    public CartItemResponse add(CartCreateRequest request) {
        long userId = currentUserId();
        String optionData = writeOptions(request.selectedOptions());
        String optionKey = hash(optionData);
        CartItem item = repository.findByUserIdAndProductIdAndOptionKey(userId, request.productId(), optionKey)
                .orElseGet(() -> new CartItem(userId, request.productId(), request.productName(), request.brand(),
                        request.imageUrl(), request.unitPrice(), request.currency(), request.sourceUrl(),
                        optionData, optionKey, request.quantity()));
        if (item.getId() != null) item.addQuantity(request.quantity());
        item.updateSnapshot(request.productName(), request.brand(), request.imageUrl(), request.unitPrice(),
                request.currency(), request.sourceUrl());
        return toResponse(repository.save(item));
    }

    @Transactional
    public CartItemResponse changeQuantity(long itemId, int quantity) {
        CartItem item = findOwnedItem(itemId);
        item.changeQuantity(quantity);
        return toResponse(item);
    }

    @Transactional
    public void remove(long itemId) {
        repository.delete(findOwnedItem(itemId));
    }

    private CartItem findOwnedItem(long itemId) {
        return repository.findByIdAndUserId(itemId, currentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(item.getId(), item.getProductId(), item.getProductName(), item.getBrand(),
                item.getImageUrl(), item.getUnitPrice(), item.getCurrency(), item.getSourceUrl(),
                readOptions(item.getOptionData()), item.getQuantity(), item.getCreatedAt());
    }

    private String writeOptions(Map<String, String> options) {
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(options == null ? Map.of() : options));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("상품 옵션을 저장할 수 없습니다.", exception);
        }
    }

    private Map<String, String> readOptions(String value) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(value, Map.class);
            TreeMap<String, String> result = new TreeMap<>();
            parsed.forEach((key, item) -> result.put(String.valueOf(key), String.valueOf(item)));
            return result;
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("옵션 식별자를 만들 수 없습니다.", exception);
        }
    }

    private long currentUserId() {
        User user = authenticatedUserService.getCurrentUser();
        return user.getMemberId();
    }
}
