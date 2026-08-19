package com.zik00.shop.service.search.provider;

import com.zik00.shop.domain.search.DiscoveredProduct;

import java.util.List;

public interface ProductSearchProvider {
    boolean isEnabled();
    List<DiscoveredProduct> search(String query, String category);
    DiscoveredProduct resolveMerchant(DiscoveredProduct product);
}
