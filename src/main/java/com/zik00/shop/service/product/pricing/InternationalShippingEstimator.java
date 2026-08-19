package com.zik00.shop.service.product.pricing;

import com.zik00.shop.service.payment.OperatingExchangeRateService;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class InternationalShippingEstimator {
    private final OperatingExchangeRateService exchangeRateService;

    public InternationalShippingEstimator(OperatingExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    public Estimate estimate(String productName, String category, int quantity) {
        String text = (productName + " " + category).toLowerCase(Locale.ROOT);
        WeightRange unit = weightRange(text);
        int minimumWeight = Math.multiplyExact(unit.minimumGrams(), quantity);
        int maximumWeight = Math.multiplyExact(unit.maximumGrams(), quantity);
        long minimumKrw = emsJapanEstimate(minimumWeight);
        long maximumKrw = emsJapanEstimate(maximumWeight);
        return new Estimate(
                minimumWeight,
                maximumWeight,
                exchangeRateService.toJpy(minimumKrw, "KRW"),
                exchangeRateService.toJpy(maximumKrw, "KRW"),
                "상품명·카테고리 예상 중량 및 우체국 EMS 일본 비서류 공개요금 기준"
        );
    }

    private WeightRange weightRange(String text) {
        if (contains(text, "가구", "테이블", "의자", "모니터", "데스크탑", "캠핑", "유모차")) {
            return new WeightRange(5_000, 15_000);
        }
        if (contains(text, "가전", "노트북", "카메라", "게임기", "스피커", "청소기")) {
            return new WeightRange(1_500, 4_000);
        }
        if (contains(text, "신발", "슈즈", "스니커즈", "부츠", "샌들", "구두")) {
            return new WeightRange(1_000, 2_000);
        }
        if (contains(text, "의류", "셔츠", "팬츠", "원피스", "자켓", "패딩", "가방")) {
            return new WeightRange(500, 1_500);
        }
        if (contains(text, "도서", "책", "앨범", "lp", "완구", "피규어")) {
            return new WeightRange(500, 2_000);
        }
        if (contains(text, "화장품", "뷰티", "향수", "주얼리", "액세서리", "문구", "카드")) {
            return new WeightRange(300, 800);
        }
        return new WeightRange(500, 2_000);
    }

    private long emsJapanEstimate(int grams) {
        int chargedGrams = Math.max(500, ((grams + 249) / 250) * 250);
        return 23_500L + ((chargedGrams - 500L) / 250L) * 1_000L;
    }

    private boolean contains(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }

    private record WeightRange(int minimumGrams, int maximumGrams) {}

    public record Estimate(
            int minimumWeightGrams,
            int maximumWeightGrams,
            long minimumPriceJpy,
            long maximumPriceJpy,
            String basis
    ) {}
}
