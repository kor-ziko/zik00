package com.zik00.shop.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;
import com.zik00.shop.domain.Purchase;
import com.zik00.shop.domain.User;
import com.zik00.shop.dto.AddressCreateRequest;
import com.zik00.shop.dto.InquiryCommentCreateRequest;
import com.zik00.shop.dto.InquiryCreateRequest;
import com.zik00.shop.dto.InquiryThread;
import com.zik00.shop.dto.MypageMenuItem;
import com.zik00.shop.dto.MypageSection;
import com.zik00.shop.dto.MypageSummary;
import com.zik00.shop.dto.ProfileUpdateRequest;
import com.zik00.shop.repository.CouponRepository;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.InquiryCommentRepository;
import com.zik00.shop.repository.InquiryRepository;
import com.zik00.shop.repository.PurchaseRepository;
import com.zik00.shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MypageService {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ORDER_COMPLETED = "\uC8FC\uBB38\uC644\uB8CC";
    private static final String DELIVERY = "\uBC30\uC1A1";
    private static final String INQUIRY_PENDING = "\uB2F5\uBCC0\uB300\uAE30";
    private static final String ADDRESS_NOT_FOUND = "\uBC30\uC1A1\uC9C0\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String INQUIRY_NOT_FOUND = "\uBB38\uC758\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String USER_NOT_FOUND = "\uD68C\uC6D0 \uB370\uC774\uD130\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.";

    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final CouponRepository couponRepository;
    private final PurchaseRepository purchaseRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;

    public MypageService(
            UserRepository userRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            CouponRepository couponRepository,
            PurchaseRepository purchaseRepository,
            InquiryRepository inquiryRepository,
            InquiryCommentRepository inquiryCommentRepository
    ) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.couponRepository = couponRepository;
        this.purchaseRepository = purchaseRepository;
        this.inquiryRepository = inquiryRepository;
        this.inquiryCommentRepository = inquiryCommentRepository;
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
        request.setZipCode(user.getZipCode());
        request.setProvince(user.getProvince());
        request.setDetailAddress(user.getDetailAddress());
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
                request.getZipCode(),
                request.getProvince(),
                request.getDetailAddress(),
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
        request.setZipCode(user.getZipCode());
        request.setProvince(user.getProvince());
        request.setDetailAddress(user.getDetailAddress());
        return request;
    }

    @Transactional
    public void addDeliveryAddress(AddressCreateRequest request) {
        User user = findCurrentUser();
        if (request.isDefaultAddress()) {
            clearDefaultAddresses(user.getMemberId());
        }

        DeliveryAddress address = new DeliveryAddress(
                0L,
                user.getMemberId(),
                request.getAddressName(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getZipCode(),
                request.getProvince(),
                request.getDetailAddress(),
                request.isDefaultAddress()
        );
        deliveryAddressRepository.save(address);
    }

    @Transactional
    public void updateDeliveryAddress(long addressId, AddressCreateRequest request) {
        User user = findCurrentUser();
        DeliveryAddress address = deliveryAddressRepository.findUserAddress(addressId, user.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException(ADDRESS_NOT_FOUND));

        if (request.isDefaultAddress()) {
            clearDefaultAddresses(user.getMemberId());
        }

        address.update(
                request.getAddressName(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getZipCode(),
                request.getProvince(),
                request.getDetailAddress(),
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

        return inquiries.stream()
                .map(inquiry -> new InquiryThread(
                        inquiry,
                        commentsByInquiryId.getOrDefault(inquiry.getInquiryId(), List.of())
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

    private void clearDefaultAddresses(long memberId) {
        deliveryAddressRepository.findUserAddresses(memberId)
                .forEach(DeliveryAddress::clearDefaultAddress);
    }
}
