package com.zik00.shop.service.mypage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;
import com.zik00.shop.domain.InquiryImage;
import com.zik00.shop.domain.Purchase;
import com.zik00.shop.domain.User;
import com.zik00.shop.dto.mypage.AddressCreateRequest;
import com.zik00.shop.dto.mypage.CouponResponse;
import com.zik00.shop.dto.mypage.DeliveryAddressResponse;
import com.zik00.shop.dto.mypage.InquiryCommentCreateRequest;
import com.zik00.shop.dto.mypage.InquiryCommentView;
import com.zik00.shop.dto.mypage.InquiryCreateRequest;
import com.zik00.shop.dto.mypage.InquiryImageView;
import com.zik00.shop.dto.mypage.InquiryThread;
import com.zik00.shop.dto.mypage.InquiryView;
import com.zik00.shop.dto.mypage.MypageMenuItem;
import com.zik00.shop.dto.mypage.MypageProfileResponse;
import com.zik00.shop.dto.mypage.MypageSection;
import com.zik00.shop.dto.mypage.MypageSummary;
import com.zik00.shop.dto.mypage.ProfileUpdateRequest;
import com.zik00.shop.dto.mypage.PurchaseResponse;
import com.zik00.shop.repository.CouponRepository;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.InquiryCommentImageRepository;
import com.zik00.shop.repository.InquiryCommentRepository;
import com.zik00.shop.repository.InquiryImageRepository;
import com.zik00.shop.repository.InquiryRepository;
import com.zik00.shop.repository.PurchaseRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.util.PhoneNumberFormatter;
import com.zik00.shop.util.mypage.InquiryImagePaths;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class MypageService {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ORDER_COMPLETED = "\uC8FC\uBB38\uC644\uB8CC";
    private static final String DELIVERY = "\uBC30\uC1A1";
    private static final String ADDRESS_NOT_FOUND = "\uBC30\uC1A1\uC9C0\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String ADDRESS_NAME_REQUIRED = "\uBC30\uC1A1\uC9C0\uBA85\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String RECEIVER_NAME_REQUIRED = "\uC218\uB839\uC778\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String RECEIVER_PHONE_REQUIRED = "\uC218\uB839\uC778 \uC804\uD654\uBC88\uD638\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694.";
    private static final String POSTAL_CODE_REQUIRED = "\uC6B0\uD3B8\uBC88\uD638\uB97C \uC870\uD68C\uD574\uC11C \uC8FC\uC18C\uB97C \uC120\uD0DD\uD574\uC8FC\uC138\uC694.";
    private static final String INQUIRY_NOT_FOUND = "\uBB38\uC758\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String USER_NOT_FOUND = "\uD68C\uC6D0 \uB370\uC774\uD130\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.";

    private final AuthenticatedUserService authenticatedUserService;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final CouponRepository couponRepository;
    private final PurchaseRepository purchaseRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final InquiryCommentImageRepository inquiryCommentImageRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final InquiryImageStorageService inquiryImageStorageService;

    public MypageService(
            AuthenticatedUserService authenticatedUserService,
            DeliveryAddressRepository deliveryAddressRepository,
            CouponRepository couponRepository,
            PurchaseRepository purchaseRepository,
            InquiryRepository inquiryRepository,
            InquiryCommentRepository inquiryCommentRepository,
            InquiryCommentImageRepository inquiryCommentImageRepository,
            InquiryImageRepository inquiryImageRepository,
            InquiryImageStorageService inquiryImageStorageService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.couponRepository = couponRepository;
        this.purchaseRepository = purchaseRepository;
        this.inquiryRepository = inquiryRepository;
        this.inquiryCommentRepository = inquiryCommentRepository;
        this.inquiryCommentImageRepository = inquiryCommentImageRepository;
        this.inquiryImageRepository = inquiryImageRepository;
        this.inquiryImageStorageService = inquiryImageStorageService;
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

    public DashboardData getDashboard() {
        User user = findCurrentUser();
        long memberId = user.getMemberId();
        List<Purchase> purchaseEntities = purchaseRepository.findUserPurchases(memberId);
        List<Coupon> couponEntities = couponRepository.findUserCoupons(memberId);
        MypageSummary summary = new MypageSummary(
                countOrdersByStatus(purchaseEntities, ORDER_COMPLETED),
                countOrdersByStatus(purchaseEntities, DELIVERY),
                toInt(inquiryRepository.countUserInquiries(memberId)),
                couponEntities.size(),
                user.getDepositBalance(),
                user.getRewardPoint(),
                user.getNickname()
        );
        List<PurchaseResponse> purchases = purchaseEntities.stream()
                .map(PurchaseResponse::from)
                .toList();
        List<CouponResponse> coupons = couponEntities.stream()
                .map(CouponResponse::from)
                .toList();
        return new DashboardData(summary, MypageProfileResponse.from(user), purchases, coupons);
    }

    public MypageProfileResponse getCurrentUser() {
        return MypageProfileResponse.from(findCurrentUser());
    }

    // 전화번호, email format 안맞게 적으면 저장 안되게 해야함
    public ProfileUpdateRequest getProfileUpdateRequest() {
        User user = findCurrentUser();
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(user.getName());
        request.setNickname(user.getNickname());
        request.setTelephone(user.getTelephone());
        request.setMobilePhone(user.getMobilePhone());
        request.setEmail(user.getEmail());
        request.setAlarmConsent(user.isAlarmConsent());
        return request;
    }

    @Transactional
    public void updateProfile(ProfileUpdateRequest request) {
        User user = findCurrentUser();
        user.updateProfile(
                request.getName(),
                request.getNickname(),
                PhoneNumberFormatter.formatTelephone(request.getTelephone()),
                PhoneNumberFormatter.formatMobilePhone(request.getMobilePhone()),
                request.getEmail(),
                request.isAlarmConsent()
        );
    }

    public List<DeliveryAddressResponse> getDeliveryAddresses() {
        User user = findCurrentUser();
        return deliveryAddressRepository.findUserAddresses(user.getMemberId())
                .stream()
                .map(DeliveryAddressResponse::from)
                .toList();
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

    public List<CouponResponse> getCoupons() {
        User user = findCurrentUser();
        return couponRepository.findUserCoupons(user.getMemberId())
                .stream()
                .map(CouponResponse::from)
                .toList();
    }

    public List<PurchaseResponse> getPurchases() {
        User user = findCurrentUser();
        return purchaseRepository.findUserPurchases(user.getMemberId())
                .stream()
                .map(PurchaseResponse::from)
                .toList();
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
        // @Suil - 관리자 답변 사진을 댓글별로 묶어 사용자 화면에 전달
        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryIds(inquiryIds);
        List<Long> commentIds = comments.stream()
                .map(InquiryComment::getCommentId)
                .toList();
        Map<Long, List<InquiryImageView>> commentImagesByCommentId = commentIds.isEmpty()
                ? Map.of()
                : inquiryCommentImageRepository.findByCommentIds(commentIds)
                        .stream()
                        .flatMap(image -> InquiryImageView.from(image)
                                .stream()
                                .map(view -> new InquiryImageEntry(image.getCommentId(), view)))
                        .collect(Collectors.groupingBy(
                                InquiryImageEntry::ownerId,
                                Collectors.mapping(InquiryImageEntry::image, Collectors.toList())
                        ));
        Map<Long, List<InquiryCommentView>> commentsByInquiryId = comments.stream()
                .map(comment -> new InquiryCommentEntry(
                        comment.getInquiryId(),
                        new InquiryCommentView(
                                comment,
                                commentImagesByCommentId.getOrDefault(comment.getCommentId(), List.of())
                        )
                ))
                .collect(Collectors.groupingBy(
                        InquiryCommentEntry::inquiryId,
                        Collectors.mapping(InquiryCommentEntry::comment, Collectors.toList())
                ));
        Map<Long, List<InquiryImageView>> imagesByInquiryId = inquiryImageRepository
                .findByInquiryIds(inquiryIds)
                .stream()
                .flatMap(image -> InquiryImageView.from(image)
                        .stream()
                        .map(view -> new InquiryImageEntry(image.getInquiryId(), view)))
                .collect(Collectors.groupingBy(
                        InquiryImageEntry::ownerId,
                        Collectors.mapping(InquiryImageEntry::image, Collectors.toList())
                ));

        return inquiries.stream()
                .map(inquiry -> new InquiryThread(
                        InquiryView.from(inquiry),
                        commentsByInquiryId.getOrDefault(inquiry.getInquiryId(), List.of()),
                        imagesByInquiryId.getOrDefault(inquiry.getInquiryId(), List.of())
                ))
                .toList();
    }

    public Optional<InquiryImageDownload> getInquiryImageDownload(String imageUuid) {
        // @Suil - 사용자 문의 원본 사진과 관리자 답변 사진을 함께 조회
        String normalizedImageUuid = InquiryImagePaths.normalize(imageUuid);
        if (!InquiryImagePaths.isSafeUuid(normalizedImageUuid)) {
            return Optional.empty();
        }

        User user = findCurrentUser();
        Optional<InquiryImageDownload> inquiryImage = inquiryImageRepository
                .findUserImageByUuid(normalizedImageUuid, user.getMemberId())
                .flatMap(image -> toInquiryImageDownload(image.getImageUuid(), image.getStoredFileName()));
        if (inquiryImage.isPresent()) {
            return inquiryImage;
        }
        return inquiryCommentImageRepository.findUserImageByUuid(normalizedImageUuid, user.getMemberId())
                .flatMap(image -> toInquiryImageDownload(image.getImageUuid(), image.getStoredFileName()));
    }

    // @Suil - 새 문의를 답변대기 상태로 등록
    @Transactional
    public void addInquiry(InquiryCreateRequest request) {
        User user = findCurrentUser();
        Inquiry inquiry = new Inquiry(
                0L,
                user.getMemberId(),
                request.getTitle(),
                request.getContent(),
                false,
                LocalDateTime.now().format(DISPLAY_DATE_TIME)
        );
        inquiryRepository.save(inquiry);
        saveInquiryImages(inquiry.getInquiryId(), request.getImages());
    }

    @Transactional
    public void addInquiryComment(long inquiryId, InquiryCommentCreateRequest request) {
        User user = findCurrentUser();
        Inquiry inquiry = inquiryRepository.findUserInquiry(inquiryId, user.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException(INQUIRY_NOT_FOUND));

        InquiryComment comment = new InquiryComment(
                0L,
                inquiryId,
                user.getMemberId(),
                user.getNickname(),
                request.getContent(),
                LocalDateTime.now().format(DISPLAY_DATE_TIME)
        );
        inquiryCommentRepository.save(comment);
        // @Suil - 사용자의 추가 댓글 등록 시 문의를 다시 답변대기로 변경
        inquiry.markPending();
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
        return authenticatedUserService.getCurrentUser();
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }

    private int countOrdersByStatus(List<Purchase> purchases, String status) {
        return Math.toIntExact(purchases.stream()
                .filter(purchase -> purchase.getOrderStatus() != null
                        && purchase.getOrderStatus().contains(status))
                .count());
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
        // @Suil - 공용 문의 이미지 저장 기능을 사용
        inquiryImageRepository.saveAll(inquiryImageStorageService.store(images)
                .stream()
                .map(image -> new InquiryImage(inquiryId, image.imageUuid(), image.storedFileName()))
                .toList());
    }

    private Optional<InquiryImageDownload> toInquiryImageDownload(String imageUuid, String storedFileName) {
        return inquiryImageStorageService.load(imageUuid, storedFileName)
                .map(download -> new InquiryImageDownload(
                        download.imagePath(),
                        download.fileName(),
                        download.contentType(),
                        download.contentLength()
                ));
    }

    private record ResolvedAddress(String zipCode, String province, String detailAddress) {
    }

    private record InquiryImageEntry(long ownerId, InquiryImageView image) {
    }

    private record InquiryCommentEntry(long inquiryId, InquiryCommentView comment) {
    }

    public record InquiryImageDownload(
            Path imagePath,
            String fileName,
            String contentType,
            long contentLength
    ) {
    }

    public record DashboardData(
            MypageSummary summary,
            MypageProfileResponse profile,
            List<PurchaseResponse> recentOrders,
            List<CouponResponse> coupons
    ) {
    }
}
