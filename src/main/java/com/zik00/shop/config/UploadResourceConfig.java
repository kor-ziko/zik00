package com.zik00.shop.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private static final Path INQUIRY_IMAGE_DIR = Path.of(
            "src", "main", "resources", "uploads", "inquiries_images"
    ).toAbsolutePath().normalize();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/inquiries_images/**")
                .addResourceLocations(INQUIRY_IMAGE_DIR.toUri().toString());
    }
}
