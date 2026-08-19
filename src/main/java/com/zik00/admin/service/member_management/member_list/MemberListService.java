package com.zik00.admin.service.member_management.member_list;

import com.zik00.admin.dto.member_management.member_list.MemberListResponse;
import com.zik00.admin.repository.member_management.member_list.MemberListRepository;
import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberListService {
    private final MemberListRepository repository;
    private final PiiEncryptionService encryptionService;
    private final RedisRefreshTokenStore refreshTokenStore;

    public MemberListService(
            MemberListRepository repository,
            PiiEncryptionService encryptionService,
            RedisRefreshTokenStore refreshTokenStore
    ) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.refreshTokenStore = refreshTokenStore;
    }

    public List<MemberListResponse> findAll() {
        return repository.findAllByMemberStatusNotOrderByMemberIdAsc("WITHDRAWN").stream().map(this::toResponse).toList();
    }

    @Transactional
    public void withdraw(long memberId) {
        User user = repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if ("WITHDRAWN".equals(user.getMemberStatus())) {
            throw new IllegalStateException("이미 탈퇴 처리된 회원입니다.");
        }

        user.changeMemberStatus("WITHDRAWN");
        refreshTokenStore.revokeAllForUser(user.getAccessId());
    }

    private MemberListResponse toResponse(User user) {
        String mobile = encryptionService.decrypt(user.getMobilePhone());
        String telephone = encryptionService.decrypt(user.getTelephone());
        return new MemberListResponse(
                user.getMemberId(), encryptionService.decrypt(user.getName()), user.getNickname(), user.getLoginId(),
                encryptionService.decrypt(user.getEmail()), hasText(mobile) ? mobile : telephone, user.getMemberStatus(),
                user.getCompletedOrderCount(), user.getRewardPoint(), user.getDepositBalance(), user.getJoinedDate()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
