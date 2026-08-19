package com.zik00.shop.dto.service_intro;

import com.zik00.shop.domain.service_intro.ServiceIntroSection;

public record ServiceIntroSectionResponse(
        long id,
        String sectionType,
        String eyebrow,
        String title,
        String content,
        String detail,
        String imageUrl,
        int displayOrder
) {
    public static ServiceIntroSectionResponse from(ServiceIntroSection section) {
        return new ServiceIntroSectionResponse(
                section.getId(),
                section.getSectionType(),
                section.getEyebrow(),
                section.getTitle(),
                section.getContent(),
                section.getDetail(),
                section.getImageUrl(),
                section.getDisplayOrder()
        );
    }
}
