package com.zik00.admin.service.member_management.withdrawn_member;

import com.zik00.admin.dto.member_management.withdrawn_member.WithdrawnMemberResponse;
import com.zik00.admin.repository.member_management.withdrawn_member.WithdrawnMemberRepository;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WithdrawnMemberService {
    private final WithdrawnMemberRepository repository;
    private final PiiEncryptionService encryptionService;

    public WithdrawnMemberService(WithdrawnMemberRepository repository, PiiEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    public List<WithdrawnMemberResponse> findAll() {
        return repository.findAllByMemberStatusOrderByWithdrawnAtDescMemberIdDesc("WITHDRAWN").stream()
                .map(user -> new WithdrawnMemberResponse(
                        user.getMemberId(), encryptionService.decrypt(user.getName()), user.getNickname(), user.getLoginId(),
                        encryptionService.decrypt(user.getEmail()), user.getJoinedDate(), user.getWithdrawnAt(), user.getMemberDetail()
                )).toList();
    }
}
