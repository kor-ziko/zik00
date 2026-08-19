package com.zik00.shop.service.product.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.zik00.shop.dto.product.delivery.DeliveryEstimateRequest;
import org.junit.jupiter.api.Test;

class DeliveryEstimateServiceTest {
    private final DeliveryEstimateService service = new DeliveryEstimateService();

    @Test
    void estimatesThreeDeliveryStagesForRegularProduct() {
        var result = service.estimate(new DeliveryEstimateRequest(
                "코튼 셔츠", "패션의류 > 남성의류", "https://example.com/product/1"));

        assertThat(result.stages()).hasSize(3);
        assertThat(result.minimumDays()).isEqualTo(6);
        assertThat(result.maximumDays()).isEqualTo(13);
        assertThat(result.stages().get(0).label()).isEqualTo("판매처 → 한국 물류센터");
    }

    @Test
    void allowsMoreTimeForRestrictedProduct() {
        var result = service.estimate(new DeliveryEstimateRequest(
                "리튬 배터리 포함 기기", "가전", "https://example.com/product/2"));

        assertThat(result.stages().get(1).minimumDays()).isEqualTo(5);
        assertThat(result.maximumDays()).isEqualTo(19);
    }
}
