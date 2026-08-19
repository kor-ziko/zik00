package com.zik00.shop.service.cart;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.cart.CartItem;
import com.zik00.shop.dto.cart.CartCreateRequest;
import com.zik00.shop.repository.cart.CartRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {
    @Test
    void addsQuantityWhenSameProductAndOptionsAlreadyExist() {
        User user = mock(User.class);
        when(user.getMemberId()).thenReturn(11L);
        AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
        when(authenticatedUserService.getCurrentUser()).thenReturn(user);

        CartItem existing = mock(CartItem.class);
        when(existing.getId()).thenReturn(9L);
        when(existing.getProductId()).thenReturn("shoe-1");
        when(existing.getProductName()).thenReturn("운동화");
        when(existing.getUnitPrice()).thenReturn(56000L);
        when(existing.getCurrency()).thenReturn("KRW");
        when(existing.getOptionData()).thenReturn("{\"사이즈\":\"250\"}");
        when(existing.getQuantity()).thenReturn(3);

        CartRepository repository = mock(CartRepository.class);
        when(repository.findByUserIdAndProductIdAndOptionKey(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq("shoe-1"), anyString())).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        CartCreateRequest request = new CartCreateRequest("shoe-1", "운동화", "브랜드", "/shoe.webp",
                56000L, "KRW", null, Map.of("사이즈", "250"), 2);
        var result = new CartService(authenticatedUserService, repository, new ObjectMapper()).add(request);

        verify(existing).addQuantity(2);
        assertThat(result.id()).isEqualTo(9L);
    }
}
