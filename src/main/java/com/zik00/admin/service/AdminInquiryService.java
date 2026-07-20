package com.zik00.admin.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.zik00.admin.dto.AdminInquiryCommentResponse;
import com.zik00.admin.dto.AdminInquiryDetailResponse;
import com.zik00.admin.dto.AdminInquiryImageResponse;
import com.zik00.admin.dto.AdminInquirySummaryResponse;
import com.zik00.admin.dto.AdminSession;
import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;
import com.zik00.shop.domain.InquiryCommentImage;
import com.zik00.shop.domain.InquiryImage;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.InquiryCommentImageRepository;
import com.zik00.shop.repository.InquiryCommentRepository;
import com.zik00.shop.repository.InquiryImageRepository;
import com.zik00.shop.repository.InquiryRepository;
import com.zik00.shop.repository.UserRepository;
import com.zik00.shop.service.mypage.InquiryImageStorageService;
import com.zik00.shop.util.mypage.InquiryImagePaths;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// @dev - 관리자 문의 상세 조회 및 답변 여부 처리
@Service
@Transactional(readOnly = true)
public class AdminInquiryService {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ADMIN_IMAGE_URL_PREFIX = "/api/admin/inquiries/images/";
    private static final int MAX_REPLY_LENGTH = 2_000;

    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final InquiryCommentImageRepository inquiryCommentImageRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final UserRepository userRepository;
    private final InquiryImageStorageService inquiryImageStorageService;

    public AdminInquiryService(
            InquiryRepository inquiryRepository,
            InquiryCommentRepository inquiryCommentRepository,
            InquiryCommentImageRepository inquiryCommentImageRepository,
            InquiryImageRepository inquiryImageRepository,
            UserRepository userRepository,
            InquiryImageStorageService inquiryImageStorageService
    ) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryCommentRepository = inquiryCommentRepository;
        this.inquiryCommentImageRepository = inquiryCommentImageRepository;
        this.inquiryImageRepository = inquiryImageRepository;
        this.userRepository = userRepository;
        this.inquiryImageStorageService = inquiryImageStorageService;
    }

    public List<AdminInquirySummaryResponse> findInquiries() {
        List<Inquiry> inquiries = inquiryRepository.findAllByOrderByInquiryIdDesc();
        if (inquiries.isEmpty()) {
            return List.of();
        }

        List<Long> inquiryIds = inquiries.stream().map(Inquiry::getInquiryId).toList();
        Map<Long, String> memberNames = userRepository.findAllById(
                        inquiries.stream().map(Inquiry::getMemberId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(User::getMemberId, User::getName));
        Map<Long, Long> commentCounts = inquiryCommentRepository.countByInquiryIds(inquiryIds).stream()
                .collect(Collectors.toMap(
                        InquiryCommentRepository.InquiryItemCount::getInquiryId,
                        InquiryCommentRepository.InquiryItemCount::getItemCount
                ));
        Map<Long, Long> imageCounts = inquiryImageRepository.countByInquiryIds(inquiryIds).stream()
                .collect(Collectors.toMap(
                        InquiryImageRepository.InquiryItemCount::getInquiryId,
                        InquiryImageRepository.InquiryItemCount::getItemCount
                ));

        return inquiries
                .stream()
                .map(inquiry -> new AdminInquirySummaryResponse(
                        inquiry.getInquiryId(),
                        inquiry.getMemberId(),
                        memberNames.getOrDefault(inquiry.getMemberId(), "탈퇴회원"),
                        inquiry.getTitle(),
                        inquiry.isStatus(),
                        inquiry.getCreatedAt(),
                        commentCounts.getOrDefault(inquiry.getInquiryId(), 0L),
                        imageCounts.getOrDefault(inquiry.getInquiryId(), 0L)
                ))
                .toList();
    }

    public AdminInquiryDetailResponse findInquiry(long inquiryId) {
        Inquiry inquiry = findInquiryEntity(inquiryId);
        User member = userRepository.findById(inquiry.getMemberId()).orElse(null);
        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryIds(List.of(inquiryId));
        List<Long> commentIds = comments.stream().map(InquiryComment::getCommentId).toList();
        Map<Long, List<AdminInquiryImageResponse>> commentImages = commentIds.isEmpty()
                ? Map.of()
                : inquiryCommentImageRepository.findByCommentIds(commentIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                InquiryCommentImage::getCommentId,
                                Collectors.mapping(this::toImageResponse, Collectors.toList())
                        ));

        return new AdminInquiryDetailResponse(
                inquiry.getInquiryId(),
                inquiry.getMemberId(),
                member == null ? "탈퇴회원" : member.getName(),
                member == null ? null : member.getNickname(),
                member == null ? null : member.getEmail(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.isStatus(),
                inquiry.getCreatedAt(),
                inquiryImageRepository.findByInquiryIds(List.of(inquiryId))
                        .stream()
                        .map(this::toImageResponse)
                        .toList(),
                comments.stream()
                        .map(comment -> new AdminInquiryCommentResponse(
                                comment.getCommentId(),
                                comment.getWriterType(),
                                comment.getWriterName(),
                                comment.getContent(),
                                comment.getCreatedAt(),
                                commentImages.getOrDefault(comment.getCommentId(), List.of())
                        ))
                        .toList()
        );
    }

    @Transactional
    public AdminInquiryDetailResponse reply(
            long inquiryId,
            String content,
            List<MultipartFile> images,
            AdminSession adminSession
    ) {
        Inquiry inquiry = findInquiryEntity(inquiryId);
        String normalizedContent = normalizeReply(content);
        InquiryComment reply = InquiryComment.adminReply(
                inquiryId,
                adminSession.adminId(),
                adminSession.name(),
                normalizedContent,
                LocalDateTime.now().format(DISPLAY_DATE_TIME)
        );
        inquiryCommentRepository.save(reply);

        try {
            inquiryCommentImageRepository.saveAll(inquiryImageStorageService.store(images)
                    .stream()
                    .map(image -> new InquiryCommentImage(
                            reply.getCommentId(),
                            image.imageUuid(),
                            image.storedFileName()
                    ))
                    .toList());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        inquiry.markAnswered();
        return findInquiry(inquiryId);
    }

    public Optional<InquiryImageStorageService.ImageDownload> getImageDownload(String imageUuid) {
        String normalizedImageUuid = InquiryImagePaths.normalize(imageUuid);
        if (!InquiryImagePaths.isSafeUuid(normalizedImageUuid)) {
            return Optional.empty();
        }

        Optional<InquiryImageStorageService.ImageDownload> inquiryImage = inquiryImageRepository
                .findByImageUuid(normalizedImageUuid)
                .flatMap(image -> inquiryImageStorageService.load(image.getImageUuid(), image.getStoredFileName()));
        if (inquiryImage.isPresent()) {
            return inquiryImage;
        }
        return inquiryCommentImageRepository.findByImageUuid(normalizedImageUuid)
                .flatMap(image -> inquiryImageStorageService.load(image.getImageUuid(), image.getStoredFileName()));
    }

    private Inquiry findInquiryEntity(long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."));
    }

    private String normalizeReply(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 내용을 입력해주세요.");
        }
        if (normalized.length() > MAX_REPLY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변은 2,000자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private AdminInquiryImageResponse toImageResponse(InquiryImage image) {
        return new AdminInquiryImageResponse(
                image.getImageUuid(),
                ADMIN_IMAGE_URL_PREFIX + image.getImageUuid()
        );
    }

    private AdminInquiryImageResponse toImageResponse(InquiryCommentImage image) {
        return new AdminInquiryImageResponse(
                image.getImageUuid(),
                ADMIN_IMAGE_URL_PREFIX + image.getImageUuid()
        );
    }
}
