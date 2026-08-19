package com.zik00.admin.service.Web_management;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class HomepageImageStorageService {
    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private final Path imageDirectory;

    public HomepageImageStorageService(
            @Value("${shop.upload.homepage-image-dir:uploads/homepage_images}") String imageDirectory
    ) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("이미지 파일을 선택해주세요.");
        if (image.getSize() > MAX_IMAGE_SIZE) throw new IllegalArgumentException("이미지는 5MB 이하여야 합니다.");

        try {
            byte[] content = image.getBytes();
            String extension = detectExtension(content);
            if (extension.isBlank()) throw new IllegalArgumentException("jpg, png, gif, webp 이미지만 등록할 수 있습니다.");

            Files.createDirectories(imageDirectory);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = imageDirectory.resolve(fileName).normalize();
            if (!target.startsWith(imageDirectory)) throw new IllegalArgumentException("올바르지 않은 이미지입니다.");
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/api/homepage-images/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", exception);
        }
    }

    public Optional<StoredImage> load(String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif|webp)")) return Optional.empty();
        Path path = imageDirectory.resolve(fileName).normalize();
        if (!path.startsWith(imageDirectory) || !Files.isRegularFile(path)) return Optional.empty();
        try {
            return Optional.of(new StoredImage(path, contentType(fileName), Files.size(path)));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private String detectExtension(byte[] content) {
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) return "jpg";
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (startsWithAscii(content, "GIF87a") || startsWithAscii(content, "GIF89a")) return "gif";
        if (content.length >= 12 && startsWithAscii(content, "RIFF") && asciiEquals(content, 8, "WEBP")) return "webp";
        return "";
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) return false;
        }
        return true;
    }

    private boolean startsWithAscii(byte[] content, String signature) { return asciiEquals(content, 0, signature); }

    private boolean asciiEquals(byte[] content, int offset, String expected) {
        if (content.length < offset + expected.length()) return false;
        for (int index = 0; index < expected.length(); index++) {
            if (content[offset + index] != (byte) expected.charAt(index)) return false;
        }
        return true;
    }

    private String contentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/webp";
    }

    public record StoredImage(Path path, String contentType, long contentLength) {}
}
