package com.zik00.admin.service.member_management.reward_point;

import com.zik00.admin.domain.member_management.reward_point.RewardPointHistory;
import com.zik00.admin.dto.member_management.reward_point.RewardPointAdjustmentRequest;
import com.zik00.admin.dto.member_management.reward_point.RewardPointHistoryResponse;
import com.zik00.admin.dto.member_management.reward_point.RewardPointMemberResponse;
import com.zik00.admin.repository.member_management.reward_point.RewardPointHistoryRepository;
import com.zik00.admin.repository.member_management.reward_point.RewardPointMemberRepository;
import com.zik00.shop.domain.User;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RewardPointService {
    private final RewardPointMemberRepository memberRepository;
    private final RewardPointHistoryRepository historyRepository;
    private final PiiEncryptionService encryptionService;

    public RewardPointService(RewardPointMemberRepository memberRepository, RewardPointHistoryRepository historyRepository, PiiEncryptionService encryptionService) {
        this.memberRepository = memberRepository;
        this.historyRepository = historyRepository;
        this.encryptionService = encryptionService;
    }

    public List<RewardPointMemberResponse> findMembers() {
        return memberRepository.findAllByMemberStatusNotOrderByMemberIdAsc("WITHDRAWN").stream()
                .map(user -> new RewardPointMemberResponse(user.getMemberId(), nameOf(user), user.getNickname(), user.getLoginId(), user.getRewardPoint()))
                .toList();
    }

    public List<RewardPointHistoryResponse> findHistories() {
        Map<Long, User> users = memberRepository.findAll().stream().collect(Collectors.toMap(User::getMemberId, Function.identity()));
        return historyRepository.findAllByOrderByCreatedAtDescIdDesc().stream().map(history -> toResponse(history, users.get(history.getMemberId()))).toList();
    }

    @Transactional
    public RewardPointHistoryResponse adjust(RewardPointAdjustmentRequest request) {
        if (request.amount() == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 포인트를 입력해주세요.");
        User user = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        try {
            user.adjustRewardPoint(request.amount());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        RewardPointHistory saved = historyRepository.save(new RewardPointHistory(
                user.getMemberId(), request.amount(), user.getRewardPoint(), request.reason().trim()
        ));
        return toResponse(saved, user);
    }

    private RewardPointHistoryResponse toResponse(RewardPointHistory history, User user) {
        return new RewardPointHistoryResponse(history.getId(), history.getMemberId(), user == null ? "탈퇴 회원" : nameOf(user),
                user == null ? "-" : user.getLoginId(), history.getAmount(), history.getBalanceAfter(), history.getReason(), history.getCreatedAt());
    }

    private String nameOf(User user) {
        String name = encryptionService.decrypt(user.getName());
        return name == null || name.isBlank() ? user.getNickname() : name;
    }
}
