package com.zik00.admin.member;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberApiController {
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    public AdminMemberApiController(
            UserRepository userRepository,
            DeliveryAddressRepository deliveryAddressRepository
    ) {
        this.userRepository = userRepository;
        this.deliveryAddressRepository = deliveryAddressRepository;
    }

    @GetMapping
    public List<AdminMemberSummaryResponse> findMembers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "memberId"))
                .stream()
                .map(AdminMemberSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{memberId}")
    public AdminMemberDetailResponse findMember(@PathVariable Long memberId) {
        User user = findUser(memberId);
        return AdminMemberDetailResponse.from(user, deliveryAddressRepository.findUserAddresses(user.getMemberId()));
    }

    private User findUser(Long memberId) {
        return userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
