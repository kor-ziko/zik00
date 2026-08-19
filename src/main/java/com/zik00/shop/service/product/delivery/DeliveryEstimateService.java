package com.zik00.shop.service.product.delivery;

import com.zik00.shop.dto.product.delivery.DeliveryEstimateRequest;
import com.zik00.shop.dto.product.delivery.DeliveryEstimateResponse;
import com.zik00.shop.dto.product.delivery.DeliveryStageResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DeliveryEstimateService {
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest request) {
        String product = normalize(request.productName());
        String category = normalize(request.category());
        String sourceUrl = normalize(request.sourceUrl());

        DayRange domestic = domesticRange(product, category, sourceUrl);
        DayRange international = internationalRange(product, category);
        DayRange lastMile = new DayRange(1, 3);

        List<DeliveryStageResponse> stages = List.of(
                stage("SELLER_TO_KOREA", "판매처 → 한국 물류센터", domestic),
                stage("KOREA_TO_JAPAN", "한국 물류센터 → 일본 물류센터", international),
                stage("JAPAN_TO_CUSTOMER", "일본 물류센터 → 고객", lastMile)
        );
        return new DeliveryEstimateResponse(
                domestic.minimum() + international.minimum() + lastMile.minimum(),
                domestic.maximum() + international.maximum() + lastMile.maximum(),
                stages,
                "상품 카테고리, 판매처와 운송 제한 가능성을 반영한 영업일 기준 예상치"
        );
    }

    private DayRange domesticRange(String product, String category, String sourceUrl) {
        if (containsAny(product, "예약", "주문제작", "pre-order", "made to order")) {
            return new DayRange(5, 10);
        }
        if (containsAny(category, "가구", "가전", "캠핑") || containsAny(product, "대형", "세탁기", "냉장고", "가구")) {
            return new DayRange(3, 7);
        }
        if (sourceUrl.contains("kream.co.kr")) {
            return new DayRange(2, 5);
        }
        return new DayRange(2, 4);
    }

    private DayRange internationalRange(String product, String category) {
        if (containsAny(category, "향수", "뷰티", "가전")
                || containsAny(product, "향수", "스프레이", "배터리", "보조배터리", "리튬")) {
            return new DayRange(5, 9);
        }
        if (containsAny(category, "가구", "캠핑") || containsAny(product, "대형", "가구")) {
            return new DayRange(5, 10);
        }
        return new DayRange(3, 6);
    }

    private DeliveryStageResponse stage(String code, String label, DayRange range) {
        return new DeliveryStageResponse(code, label, range.minimum(), range.maximum());
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DayRange(int minimum, int maximum) {
    }
}
