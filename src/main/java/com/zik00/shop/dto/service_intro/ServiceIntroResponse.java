package com.zik00.shop.dto.service_intro;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceIntroResponse(
        List<ServiceIntroSectionResponse> sections,
        LocalDateTime updatedAt
) {
}
