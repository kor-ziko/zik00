package com.zik00.shop.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class InquiryImagePaths {
    private static final String IMAGE_URL_PREFIX = "/mypage/inquiries/images/";
    private static final Pattern SAFE_IMAGE_UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern SAFE_STORED_IMAGE_FILE_NAME_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png|gif|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    private InquiryImagePaths() {
    }

    public static boolean isSafeUuid(String imageUuid) {
        return SAFE_IMAGE_UUID_PATTERN.matcher(normalize(imageUuid)).matches();
    }

    public static Optional<String> extractSafeStoredFileName(String imageUuid, String storedFileName) {
        String normalizedImageUuid = normalize(imageUuid);
        if (!isSafeUuid(normalizedImageUuid)) {
            return Optional.empty();
        }

        String normalizedPath = normalize(storedFileName).replace('\\', '/');
        String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (!SAFE_STORED_IMAGE_FILE_NAME_PATTERN.matcher(fileName).matches()
                || !lowerFileName.startsWith(normalizedImageUuid.toLowerCase(Locale.ROOT) + ".")) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }

    public static String buildViewUrl(String imageUuid) {
        return IMAGE_URL_PREFIX + normalize(imageUuid);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
