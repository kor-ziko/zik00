package com.zik00.shop.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;
import com.zik00.shop.domain.InquiryImage;
import com.zik00.shop.domain.Purchase;
import com.zik00.shop.domain.User;
import com.zik00.shop.dto.AddressCreateRequest;
import com.zik00.shop.dto.InquiryCommentCreateRequest;
import com.zik00.shop.dto.InquiryCreateRequest;
import com.zik00.shop.dto.InquiryImageView;
import com.zik00.shop.dto.InquiryThread;
import com.zik00.shop.dto.MypageMenuItem;
import com.zik00.shop.dto.MypageSection;
import com.zik00.shop.dto.MypageSummary;
import com.zik00.shop.dto.ProfileUpdateRequest;
import com.zik00.shop.repository.CouponRepository;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.InquiryCommentRepository;
import com.zik00.shop.repository.InquiryImageRepository;
import com.zik00.shop.repository.InquiryRepository;
import com.zik00.shop.repository.PurchaseRepository;
import com.zik00.shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class MypageService {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Path INQUIRY_IMAGE_DIR = Path.of(
            "src", "main", "resources", "uploads", "inquiries_images"
    ).toAbsolutePath().normalize();
    private static final String INQUIRY_IMAGE_URL_PREFIX = "/uploads/inquiries_images/";
    private static final int MAX_INQUIRY_IMAGE_COUNT = 3;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final String ORDER_COMPLETED = "\uC8FC\uBB38\uC644\uB8CC";
    private static final String DELIVERY = "\uBC30\uC1A1";
    private static final String INQUIRY_PENDING = "\uB2F5\uBCC0\uB300\uAE30";
    private static final String ADDRESS_NOT_FOUND = "\uBC30\uC1A1\uC9C0\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String ADDRESS_NAME_REQUIRED = "\uBC30\uC1A1\uC9C0\uBA85\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String RECEIVER_NAME_REQUIRED = "\uC218\uB839\uC778\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String RECEIVER_PHONE_REQUIRED = "\uC218\uB839\uC778 \uC804\uD654\uBC88\uD638\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String POSTAL_CODE_REQUIRED = "\uC6B0\uD3B8\uBC88\uD638\uB97C \uC870\uD68C\uD574\uC11C \uC8FC\uC18C\uB97C \uC120\uD0DD\uD574\uC8FC\uC138\uC694.";
    private static final String INQUIRY_NOT_FOUND = "\uBB38\uC758\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String INQUIRY_IMAGE_COUNT_EXCEEDED = "\uBB38\uC758 \uC774\uBBF8\uC9C0\uB294 \uCD5C\uB300 3\uAC1C\uAE4C\uC9C0 \uCCA8\uBD80\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.";
    private static final String INQUIRY_IMAGE_INVALID = "\uBB38\uC758 \uC774\uBBF8\uC9C0\uB294 jpg, jpeg, png, gif, webp \uD615\uC2DD\uB9CC \uCCA8\uBD80\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.";
    private static final String INQUIRY_IMAGE_SAVE_FAILED = "\uBB38\uC758 \uC774\uBBF8\uC9C0\uB97C \uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String USER_NOT_FOUND = "\uD68C\uC6D0 \uB370\uC774\uD130\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.";

    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final CouponRepository couponRepository;
    private final PurchaseRepository purchaseRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final InquiryImageRepository inquiryImageRepository;

    public MypageService(
            UserRepository userRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            CouponRepository couponRepository,
            PurchaseRepository purchaseRepository,
            InquiryRepository inquiryRepository,
            InquiryCommentRepository inquiryCommentRepository,
            InquiryImageRepository inquiryImageRepository
    ) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.couponRepository = couponRepository;
        this.purchaseRepository = purchaseRepository;
        this.inquiryRepository = inquiryRepository;
        this.inquiryCommentRepository = inquiryCommentRepository;
        this.inquiryImageRepository = inquiryImageRepository;
    }

    public MypageSummary getSummary() {
        User user = findCurrentUser();
        long memberId = user.getMemberId();
        return new MypageSummary(
                toInt(purchaseRepository.countUserOrdersByStatus(memberId, ORDER_COMPLETED)),
                toInt(purchaseRepository.countUserOrdersByStatus(memberId, DELIVERY)),
                toInt(inquiryRepository.countUserInquiries(memberId)),
                toInt(couponRepository.countUserCoupons(memberId)),
                user.getDepositBalance(),
                user.getRewardPoint(),
                user.getNickname()
        );
    }

    public User getCurrentUser() {
        return findCurrentUser();
    }

    public ProfileUpdateRequest getProfileUpdateRequest() {
        User user = findCurrentUser();
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(user.getName());
        request.setNickname(user.getNickname());
        request.setMobilePhone(user.getMobilePhone());
        request.setEmail(user.getEmail());
        request.setAlarmConsent(user.isAlarmConsent());
        return request;
    }

    //나중에 메일인증 한 번하고 수정할 수 있게 하기
    @Transactional
    public void updateProfile(ProfileUpdateRequest request) {
        User user = findCurrentUser();
        user.updateProfile(
                request.getName(),
                request.getNickname(),
                request.getMobilePhone(),
                request.getEmail(),
                request.isAlarmConsent()
        );
    }

    public List<DeliveryAddress> getDeliveryAddresses() {
        User user = findCurrentUser();
        return deliveryAddressRepository.findUserAddresses(user.getMemberId());
    }

    public AddressCreateRequest getAddressCreateRequest() {
        User user = findCurrentUser();
        AddressCreateRequest request = new AddressCreateRequest();
        request.setReceiverName(user.getName());
        request.setReceiverPhone(user.getMobilePhone());
        return request;
    }

    @Transactional
    public void addDeliveryAddress(AddressCreateRequest request) {
        User user = findCurrentUser();
        validateDeliveryAddressRequest(request);
        ResolvedAddress resolvedAddress = resolveAddress(request, null);
        if (request.isDefaultAddress()) {
            clearDefaultAddresses(user.getMemberId());
        }

        DeliveryAddress address = new DeliveryAddress(
                0L,
                user.getMemberId(),
                request.getAddressName(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                resolvedAddress.zipCode(),
                resolvedAddress.province(),
                resolvedAddress.detailAddress(),
                request.isDefaultAddress()
        );
        deliveryAddressRepository.save(address);
    }

    @Transactional
    public void updateDeliveryAddress(long addressId, AddressCreateRequest request) {
        User user = findCurrentUser();
        validateDeliveryAddressRequest(request);
        DeliveryAddress address = deliveryAddressRepository.findUserAddress(addressId, user.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException(ADDRESS_NOT_FOUND));
        ResolvedAddress resolvedAddress = resolveAddress(request, address);

        if (request.isDefaultAddress()) {
            clearDefaultAddresses(user.getMemberId());
        }

        address.update(
                request.getAddressName(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                resolvedAddress.zipCode(),
                resolvedAddress.province(),
                resolvedAddress.detailAddress(),
                request.isDefaultAddress()
        );
    }

    @Transactional
    public void deleteDeliveryAddress(long addressId) {
        User user = findCurrentUser();
        DeliveryAddress address = deliveryAddressRepository.findUserAddress(addressId, user.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException(ADDRESS_NOT_FOUND));
        deliveryAddressRepository.delete(address);
    }

    public List<Coupon> getCoupons() {
        User user = findCurrentUser();
        return couponRepository.findUserCoupons(user.getMemberId());
    }

    public List<Purchase> getPurchases() {
        User user = findCurrentUser();
        return purchaseRepository.findUserPurchases(user.getMemberId());
    }

    public List<InquiryThread> getInquiryThreads() {
        User user = findCurrentUser();
        List<Inquiry> inquiries = inquiryRepository.findUserInquiries(user.getMemberId());
        if (inquiries.isEmpty()) {
            return List.of();
        }

        List<Long> inquiryIds = inquiries.stream()
                .map(Inquiry::getInquiryId)
                .toList();
        Map<Long, List<InquiryComment>> commentsByInquiryId = inquiryCommentRepository
                .findByInquiryIds(inquiryIds)
                .stream()
                .collect(Collectors.groupingBy(InquiryComment::getInquiryId));
        Map<Long, List<InquiryImageView>> imagesByInquiryId = inquiryImageRepository
                .findByInquiryIds(inquiryIds)
                .stream()
                .flatMap(image -> InquiryImageView.from(image)
                        .stream()
                        .map(view -> new InquiryImageEntry(image.getInquiryId(), view)))
                .collect(Collectors.groupingBy(
                        InquiryImageEntry::inquiryId,
                        Collectors.mapping(InquiryImageEntry::image, Collectors.toList())
                ));

        return inquiries.stream()
                .map(inquiry -> new InquiryThread(
                        inquiry,
                        commentsByInquiryId.getOrDefault(inquiry.getInquiryId(), List.of()),
                        imagesByInquiryId.getOrDefault(inquiry.getInquiryId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public void addInquiry(InquiryCreateRequest request) {
        User user = findCurrentUser();
        Inquiry inquiry = new Inquiry(
                0L,
                user.getMemberId(),
                request.getTitle(),
                request.getContent(),
                INQUIRY_PENDING,
                LocalDateTime.now().format(DISPLAY_DATE_TIME)
        );
        inquiryRepository.save(inquiry);
        saveInquiryImages(inquiry.getInquiryId(), request.getImages());
    }

    @Transactional
    public void addInquiryComment(long inquiryId, InquiryCommentCreateRequest request) {
        User user = findCurrentUser();
        boolean ownedInquiry = inquiryRepository.existsUserInquiry(inquiryId, user.getMemberId());
        if (!ownedInquiry) {
            throw new IllegalArgumentException(INQUIRY_NOT_FOUND);
        }

        InquiryComment comment = new InquiryComment(
                0L,
                inquiryId,
                user.getMemberId(),
                user.getNickname(),
                request.getContent(),
                LocalDateTime.now().format(DISPLAY_DATE_TIME)
        );
        inquiryCommentRepository.save(comment);
    }

    public List<MypageMenuItem> getMenuItems(MypageSection activeSection) {
        return Arrays.stream(MypageSection.values())
                .filter(section -> section != MypageSection.HOME)
                .map(section -> new MypageMenuItem(
                        section.getTitle(),
                        section.getDescription(),
                        section.getUrl(),
                        section == activeSection
                ))
                .toList();
    }

    private User findCurrentUser() {
        return userRepository.findCurrentUser()
                .orElseThrow(() -> new IllegalStateException(USER_NOT_FOUND));
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }

    private void validateDeliveryAddressRequest(AddressCreateRequest request) {
        requireNotBlank(request.getAddressName(), ADDRESS_NAME_REQUIRED);
        requireNotBlank(request.getReceiverName(), RECEIVER_NAME_REQUIRED);
        requireNotBlank(request.getReceiverPhone(), RECEIVER_PHONE_REQUIRED);
    }

    private void requireNotBlank(String value, String message) {
        if (normalize(value).isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private ResolvedAddress resolveAddress(AddressCreateRequest request, DeliveryAddress currentAddress) {
        String zipCode = normalize(request.getZipCode());
        String province = normalize(request.getProvince());
        String baseAddress = normalize(request.getBaseAddress());
        if (!zipCode.isBlank() || !province.isBlank() || !baseAddress.isBlank()) {
            String normalizedZipCode = zipCode.replaceAll("\\D", "");
            if (normalizedZipCode.length() != 7
                    || province.isBlank()
                    || (currentAddress == null && baseAddress.isBlank())) {
                throw new IllegalArgumentException(POSTAL_CODE_REQUIRED);
            }
            return new ResolvedAddress(
                    formatPostalCode(normalizedZipCode),
                    province,
                    joinAddress(baseAddress, request.getDetailAddress())
            );
        }

        if (currentAddress != null) {
            return new ResolvedAddress(
                    currentAddress.getZipCode(),
                    currentAddress.getProvince(),
                    normalize(request.getDetailAddress())
            );
        }

        throw new IllegalArgumentException(POSTAL_CODE_REQUIRED);
    }

    private String joinAddress(String postalAddress, String userDetailAddress) {
        String normalizedPostalAddress = normalize(postalAddress);
        String normalizedUserDetailAddress = normalize(userDetailAddress);
        if (normalizedUserDetailAddress.isBlank()) {
            return normalizedPostalAddress;
        }
        return normalizedPostalAddress + " " + normalizedUserDetailAddress;
    }

    private String formatPostalCode(String value) {
        String normalizedValue = value == null ? "" : value.replaceAll("\\D", "");
        if (normalizedValue.length() != 7) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, 3) + "-" + normalizedValue.substring(3);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearDefaultAddresses(long memberId) {
        deliveryAddressRepository.findUserAddresses(memberId)
                .forEach(DeliveryAddress::clearDefaultAddress);
    }

    private void saveInquiryImages(long inquiryId, List<MultipartFile> images) {
        List<MultipartFile> uploadedImages = images == null ? List.of() : images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
        if (uploadedImages.isEmpty()) {
            return;
        }
        if (uploadedImages.size() > MAX_INQUIRY_IMAGE_COUNT) {
            throw new IllegalArgumentException(INQUIRY_IMAGE_COUNT_EXCEEDED);
        }

        List<InquiryImageFile> imageFiles = uploadedImages.stream()
                .map(this::prepareInquiryImageFile)
                .toList();

        try {
            Files.createDirectories(INQUIRY_IMAGE_DIR);
            for (InquiryImageFile imageFile : imageFiles) {
                saveImageFile(imageFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(INQUIRY_IMAGE_SAVE_FAILED, exception);
        }

        inquiryImageRepository.saveAll(imageFiles.stream()
                .map(imageFile -> new InquiryImage(inquiryId, imageFile.imageUuid(), imageFile.imageUrl()))
                .toList());
    }

    private InquiryImageFile prepareInquiryImageFile(MultipartFile image) {
        String extension = findImageExtension(image);
        String imageUuid = UUID.randomUUID().toString();
        String fileName = imageUuid + "." + extension;
        Path targetPath = INQUIRY_IMAGE_DIR.resolve(fileName).normalize();
        if (!targetPath.startsWith(INQUIRY_IMAGE_DIR)) {
            throw new IllegalArgumentException(INQUIRY_IMAGE_INVALID);
        }

        return new InquiryImageFile(image, imageUuid, targetPath, INQUIRY_IMAGE_URL_PREFIX + fileName);
    }

    private void saveImageFile(InquiryImageFile imageFile) throws IOException {
        try (InputStream inputStream = imageFile.image().getInputStream()) {
            Files.copy(inputStream, imageFile.targetPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String findImageExtension(MultipartFile image) {
        String originalFilename = normalize(image.getOriginalFilename());
        int extensionStart = originalFilename.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == originalFilename.length() - 1) {
            throw new IllegalArgumentException(INQUIRY_IMAGE_INVALID);
        }

        String extension = originalFilename.substring(extensionStart + 1).toLowerCase();
        String contentType = normalize(image.getContentType()).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(INQUIRY_IMAGE_INVALID);
        }
        return extension;
    }

    private record ResolvedAddress(String zipCode, String province, String detailAddress) {
    }

    private record InquiryImageEntry(long inquiryId, InquiryImageView image) {
    }

    private record InquiryImageFile(
            MultipartFile image,
            String imageUuid,
            Path targetPath,
            String imageUrl
    ) {
    }
}
