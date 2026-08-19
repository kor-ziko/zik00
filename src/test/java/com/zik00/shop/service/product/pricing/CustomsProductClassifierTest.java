package com.zik00.shop.service.product.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomsProductClassifierTest {
    @Test
    void marksKnitClothingAsGeneralTariffAndSmallValueExemptionExcluded() {
        CustomsProductClassifier.Classification result = CustomsProductClassifier.rules(
                "폴로 랄프 로렌 케이블 니트 스웨터", "패션의류 > 여성 상의"
        );

        assertThat(result.simplifiedTariffGroup()).isEqualTo("4");
        assertThat(result.generalTariffRequired()).isTrue();
        assertThat(result.smallValueExemptionExcluded()).isTrue();
        assertThat(result.hsCodeCandidate()).isEqualTo("6110");
        assertThat(result.method()).isEqualTo("RULE");
    }

    @Test
    void mapsPaperStationeryToDutyFreeSimplifiedGroup() {
        CustomsProductClassifier.Classification result = CustomsProductClassifier.rules(
                "캐릭터 메모 노트", "생활잡화 > 문구 > 종이"
        );

        assertThat(result.simplifiedTariffGroup()).isEqualTo("6");
        assertThat(result.generalTariffRequired()).isFalse();
    }
}
