package com.zik00.shop.service.wishlist;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.wishlist.WishlistItem;
import com.zik00.shop.dto.wishlist.WishlistCreateRequest;
import com.zik00.shop.dto.wishlist.WishlistItemResponse;
import com.zik00.shop.repository.wishlist.WishlistRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WishlistService {
    private final AuthenticatedUserService authenticatedUserService;
    private final WishlistRepository repository;

    public WishlistService(AuthenticatedUserService authenticatedUserService, WishlistRepository repository) {
        this.authenticatedUserService = authenticatedUserService;
        this.repository = repository;
    }

    public List<WishlistItemResponse> findItems() {
        return repository.findByUserIdOrderByCreatedAtDescIdDesc(currentUserId()).stream()
                .map(WishlistItemResponse::from)
                .toList();
    }

    public boolean isWished(String productId) {
        return repository.existsByUserIdAndProductId(currentUserId(), productId);
    }

    public long countItems() {
        return repository.countByUserId(currentUserId());
    }

    @Transactional
    public WishlistItemResponse add(WishlistCreateRequest request) {
        long userId = currentUserId();
        WishlistItem item = repository.findByUserIdAndProductId(userId, request.productId()).orElseGet(() ->
                new WishlistItem(userId, request.productId(), request.productName(), request.brand(),
                        request.imageUrl(), request.price(), request.currency(), request.sourceUrl()));
        item.updateSnapshot(request.productId(), request.productName(), request.brand(), request.imageUrl(),
                request.price(), request.currency(), request.sourceUrl());
        return WishlistItemResponse.from(repository.save(item));
    }

    @Transactional
    public void remove(String productId) {
        repository.deleteByUserIdAndProductId(currentUserId(), productId);
    }

    private long currentUserId() {
        User user = authenticatedUserService.getCurrentUser();
        return user.getMemberId();
    }
}
