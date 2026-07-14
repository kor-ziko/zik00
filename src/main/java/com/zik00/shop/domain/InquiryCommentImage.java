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
@Table(name = "inquiry_comment_images", indexes = {
        @Index(name = "idx_inquiry_comment_images_comment_id", columnList = "comment_id")
})
// @Suil - 관리자 답변 댓글에 첨부한 사진을 저장
public class InquiryCommentImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_image_id")
    private Long commentImageId;

    @Column(name = "comment_id", nullable = false)
    private long commentId;

    @Column(name = "image_uuid", length = 36, nullable = false, unique = true)
    private String imageUuid;

    @Column(name = "image_path", nullable = false)
    private String storedFileName;

    protected InquiryCommentImage() {
    }

    public InquiryCommentImage(long commentId, String imageUuid, String storedFileName) {
        this.commentId = commentId;
        this.imageUuid = imageUuid;
        this.storedFileName = storedFileName;
    }

    public long getCommentImageId() {
        return commentImageId == null ? 0L : commentImageId;
    }
}
