package com.zik00.shop.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class InquiryCreateRequest {
    @NotBlank(message = "\uBB38\uC758 \uC81C\uBAA9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 255, message = "\uBB38\uC758 \uC81C\uBAA9\uC740 255\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String title;

    @NotBlank(message = "\uBB38\uC758 \uB0B4\uC6A9\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    @Size(max = 5000, message = "\uBB38\uC758 \uB0B4\uC6A9\uC740 5000\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574\uC8FC\uC138\uC694.")
    private String content;

    @Size(max = 3, message = "\uBB38\uC758 \uC774\uBBF8\uC9C0\uB294 \uCD5C\uB300 3\uAC1C\uAE4C\uC9C0 \uCCA8\uBD80\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.")
    private List<MultipartFile> images = new ArrayList<>();

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public List<MultipartFile> getImages() {
        return images;
    }
    public void setImages(List<MultipartFile> images) {
        this.images = images == null ? new ArrayList<>() : images;
    }
}
