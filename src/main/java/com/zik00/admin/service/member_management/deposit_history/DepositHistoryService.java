package com.zik00.admin.service.member_management.deposit_history;

import com.zik00.admin.dto.member_management.deposit_history.DepositHistoryResponse;
import com.zik00.admin.repository.member_management.deposit_history.DepositHistoryMemberRepository;
import com.zik00.admin.repository.member_management.deposit_history.DepositHistoryRepository;
import com.zik00.shop.domain.User;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepositHistoryService {
    private final DepositHistoryRepository historyRepository;
    private final DepositHistoryMemberRepository memberRepository;
    private final PiiEncryptionService encryptionService;

    public DepositHistoryService(DepositHistoryRepository historyRepository, DepositHistoryMemberRepository memberRepository,
                                 PiiEncryptionService encryptionService) {
        this.historyRepository = historyRepository;
        this.memberRepository = memberRepository;
        this.encryptionService = encryptionService;
    }

    public List<DepositHistoryResponse> findAll() {
        Map<Long, User> users = memberRepository.findAll().stream().collect(Collectors.toMap(User::getMemberId, Function.identity()));
        return historyRepository.findAllByOrderByCreatedAtDescIdDesc().stream().map(item -> {
            User user = users.get(item.getMemberId());
            return new DepositHistoryResponse(item.getId(), item.getMemberId(), user == null ? "탈퇴 회원" : nameOf(user),
                    user == null ? "-" : user.getLoginId(), item.getTransactionType(), item.getAmount(), item.getBalanceAfter(),
                    item.getDescription(), item.getCreatedAt());
        }).toList();
    }

    private String nameOf(User user) {
        String name = encryptionService.decrypt(user.getName());
        return name == null || name.isBlank() ? user.getNickname() : name;
    }
}
