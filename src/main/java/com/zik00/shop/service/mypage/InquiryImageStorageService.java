package com.zik00.shop.service.mypage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.zik00.shop.util.mypage.InquiryImagePaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
// @Suil - 사용자 문의와 관리자 답변이 공통으로 사용하는 이미지 저장 기능
public class InquiryImageStorageService {
    private static final int MAX_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> DECODE_REQUIRED_EXTENSIONS = Set.of("jpg", "png", "gif");
    private static final String IMAGE_COUNT_EXCEEDED = "이미지는 최대 3개까지 첨부할 수 있습니다.";
    private static final String IMAGE_SIZE_EXCEEDED = "이미지는 파일당 5MB 이하여야 합니다.";
    private static final String IMAGE_INVALID = "jpg, jpeg, png, gif, webp 형식의 이미지만 첨부할 수 있습니다.";
    private static final String IMAGE_SAVE_FAILED = "이미지를 저장하지 못했습니다.";

    private final Path imageDirectory;

    public InquiryImageStorageService(
            @Value("${shop.upload.inquiry-image-dir:uploads/inquiries_images}") String imageDirectory
    ) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
    }

    public List<StoredImage> store(List<MultipartFile> images) {
        List<MultipartFile> uploadedImages = images == null ? List.of() : images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
        if (uploadedImages.isEmpty()) {
            return List.of();
        }
        if (uploadedImages.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException(IMAGE_COUNT_EXCEEDED);
        }

        List<PreparedImage> preparedImages = uploadedImages.stream()
                .map(this::prepare)
                .toList();

        try {
            Files.createDirectories(imageDirectory);
            for (PreparedImage image : preparedImages) {
                Files.copy(
                        new ByteArrayInputStream(image.content()),
                        image.targetPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            deleteAfterRollback(preparedImages);
            return preparedImages.stream()
                    .map(image -> new StoredImage(image.imageUuid(), image.storedFileName()))
                    .toList();
        } catch (IOException exception) {
            deleteFiles(preparedImages);
            throw new IllegalStateException(IMAGE_SAVE_FAILED, exception);
        } catch (RuntimeException exception) {
            deleteFiles(preparedImages);
            throw exception;
        }
    }

    public Optional<ImageDownload> load(String imageUuid, String storedFileName) {
        Optional<String> safeFileName = InquiryImagePaths.extractSafeStoredFileName(imageUuid, storedFileName);
        if (safeFileName.isEmpty()) {
            return Optional.empty();
        }

        String fileName = safeFileName.get();
        Path imagePath = imageDirectory.resolve(fileName).normalize();
        if (!imagePath.startsWith(imageDirectory) || !Files.isRegularFile(imagePath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ImageDownload(
                    imagePath,
                    fileName,
                    contentTypeFor(fileName),
                    Files.size(imagePath)
            ));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private PreparedImage prepare(MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(IMAGE_SIZE_EXCEEDED);
        }

        byte[] content = readContent(image);
        String extension = detectExtension(content);
        if (extension.isBlank()) {
            throw new IllegalArgumentException(IMAGE_INVALID);
        }
        if (DECODE_REQUIRED_EXTENSIONS.contains(extension)) {
            requireDecodable(content);
        }

        String imageUuid = UUID.randomUUID().toString();
        String fileName = imageUuid + "." + extension;
        Path targetPath = imageDirectory.resolve(fileName).normalize();
        if (!targetPath.startsWith(imageDirectory)) {
            throw new IllegalArgumentException(IMAGE_INVALID);
        }
        return new PreparedImage(content, imageUuid, targetPath, fileName);
    }

    private byte[] readContent(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(IMAGE_SAVE_FAILED, exception);
        }
    }

    private String detectExtension(byte[] content) {
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        if (startsWithAscii(content, "GIF87a") || startsWithAscii(content, "GIF89a")) {
            return "gif";
        }
        if (content.length >= 12 && startsWithAscii(content, "RIFF") && asciiEquals(content, 8, "WEBP")) {
            return "webp";
        }
        return "";
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] content, String signature) {
        return asciiEquals(content, 0, signature);
    }

    private boolean asciiEquals(byte[] content, int offset, String expected) {
        if (content.length < offset + expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (content[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private void requireDecodable(byte[] content) {
        try {
            BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(content));
            if (decodedImage == null || decodedImage.getWidth() <= 0 || decodedImage.getHeight() <= 0) {
                throw new IllegalArgumentException(IMAGE_INVALID);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(IMAGE_INVALID, exception);
        }
    }

    private void deleteAfterRollback(List<PreparedImage> images) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteFiles(images);
                }
            }
        });
    }

    private void deleteFiles(List<PreparedImage> images) {
        for (PreparedImage image : images) {
            try {
                Files.deleteIfExists(image.targetPath());
            } catch (IOException | SecurityException ignored) {
                // Preserve the original failure; cleanup is best-effort.
            }
        }
    }

    private String contentTypeFor(String fileName) {
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerFileName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private record PreparedImage(
            byte[] content,
            String imageUuid,
            Path targetPath,
            String storedFileName
    ) {
    }

    public record StoredImage(String imageUuid, String storedFileName) {
    }

    public record ImageDownload(
            Path imagePath,
            String fileName,
            String contentType,
            long contentLength
    ) {
    }
}
