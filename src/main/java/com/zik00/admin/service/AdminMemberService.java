package com.zik00.admin.service;

import com.zik00.admin.dto.AdminMemberDetailResponse;
import com.zik00.admin.dto.AdminMemberSummaryResponse;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.UserRepository;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminMemberService {
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final PiiEncryptionService piiEncryptionService;

    public AdminMemberService(
            UserRepository userRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            PiiEncryptionService piiEncryptionService
    ) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.piiEncryptionService = piiEncryptionService;
    }

    public List<AdminMemberSummaryResponse> findMembers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "memberId"))
                .stream()
                .map(user -> AdminMemberSummaryResponse.from(user, piiEncryptionService::decrypt))
                .toList();
    }

    public AdminMemberDetailResponse findMember(Long memberId) {
        User user = findUser(memberId);
        return AdminMemberDetailResponse.from(
                user,
                deliveryAddressRepository.findUserAddresses(user.getMemberId()),
                piiEncryptionService::decrypt
        );
    }

    private User findUser(Long memberId) {
        return userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
