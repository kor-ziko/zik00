package com.zik00.shop.dto;

import java.util.Optional;
import java.util.regex.Pattern;

import com.zik00.shop.domain.InquiryImage;

public class InquiryImageView {
    private static final Pattern SAFE_IMAGE_PATH_PATTERN = Pattern.compile(
            "^/uploads/inquiries_images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png|gif|webp)$"
    );

    private final String imageUuid;
    private final String imagePath;

    private InquiryImageView(String imageUuid, String imagePath) {
        this.imageUuid = imageUuid;
        this.imagePath = imagePath;
    }

    public static Optional<InquiryImageView> from(InquiryImage image) {
        String imagePath = normalize(image.getImagePath());
        if (!SAFE_IMAGE_PATH_PATTERN.matcher(imagePath).matches()) {
            return Optional.empty();
        }
        return Optional.of(new InquiryImageView(normalize(image.getImageUuid()), imagePath));
    }

    public String getImageUuid() {
        return imageUuid;
    }
    public String getImagePath() {
        return imagePath;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
