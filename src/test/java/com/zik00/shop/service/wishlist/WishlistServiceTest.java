package com.zik00.shop.service.wishlist;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.wishlist.WishlistItem;
import com.zik00.shop.repository.wishlist.WishlistRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WishlistServiceTest {
    @Test
    void readsOnlyCurrentUsersWishlist() {
        User user = mock(User.class);
        when(user.getMemberId()).thenReturn(7L);
        AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
        when(authenticatedUserService.getCurrentUser()).thenReturn(user);

        WishlistItem item = mock(WishlistItem.class);
        when(item.getId()).thenReturn(3L);
        when(item.getProductId()).thenReturn("product-1");
        when(item.getProductName()).thenReturn("테스트 상품");
        when(item.getPrice()).thenReturn(10000L);
        when(item.getCurrency()).thenReturn("KRW");
        WishlistRepository repository = mock(WishlistRepository.class);
        when(repository.findByUserIdOrderByCreatedAtDescIdDesc(7L)).thenReturn(List.of(item));

        var result = new WishlistService(authenticatedUserService, repository).findItems();

        assertThat(result).extracting("productId").containsExactly("product-1");
        verify(repository).findByUserIdOrderByCreatedAtDescIdDesc(7L);
    }
}
