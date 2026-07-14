package com.zik00.shop.dto;

import java.util.Optional;

import com.zik00.shop.domain.InquiryCommentImage;
import com.zik00.shop.domain.InquiryImage;
import com.zik00.shop.util.InquiryImagePaths;
import lombok.Getter;

@Getter
public class InquiryImageView {
    private final String imageUuid;
    private final String imageUrl;

    private InquiryImageView(String imageUuid, String imageUrl) {
        this.imageUuid = imageUuid;
        this.imageUrl = imageUrl;
    }

    public static Optional<InquiryImageView> from(InquiryImage image) {
        String imageUuid = InquiryImagePaths.normalize(image.getImageUuid());
        return InquiryImagePaths.extractSafeStoredFileName(imageUuid, image.getStoredFileName())
                .map(fileName -> new InquiryImageView(imageUuid, InquiryImagePaths.buildViewUrl(imageUuid)));
    }

    public static Optional<InquiryImageView> from(InquiryCommentImage image) {
        // @Suil - 관리자 답변 사진을 사용자 화면용 URL로 변환
        String imageUuid = InquiryImagePaths.normalize(image.getImageUuid());
        return InquiryImagePaths.extractSafeStoredFileName(imageUuid, image.getStoredFileName())
                .map(fileName -> new InquiryImageView(imageUuid, InquiryImagePaths.buildViewUrl(imageUuid)));
    }

}
