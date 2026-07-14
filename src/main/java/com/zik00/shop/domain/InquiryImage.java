package com.zik00.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "inquiry_images", indexes = {
        @Index(name = "idx_inquiry_images_inquiry_id", columnList = "inquiry_id")
})
public class InquiryImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "inquiry_id", nullable = false)
    private long inquiryId;

    @Column(name = "image_uuid", length = 36, nullable = false, unique = true)
    private String imageUuid;

    @Column(name = "image_path", nullable = false)
    private String storedFileName;

    protected InquiryImage() {
    }

    public InquiryImage(long inquiryId, String imageUuid, String storedFileName) {
        this.inquiryId = inquiryId;
        this.imageUuid = imageUuid;
        this.storedFileName = storedFileName;
    }

    public long getImageId() {
        return imageId == null ? 0L : imageId;
    }
}
