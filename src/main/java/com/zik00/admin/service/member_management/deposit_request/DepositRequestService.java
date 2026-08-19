package com.zik00.admin.service.member_management.deposit_request;

import com.zik00.admin.domain.member_management.deposit_history.DepositHistory;
import com.zik00.admin.domain.member_management.deposit_request.DepositRequest;
import com.zik00.admin.dto.member_management.deposit_request.DepositRequestProcessRequest;
import com.zik00.admin.dto.member_management.deposit_request.DepositRequestResponse;
import com.zik00.admin.repository.member_management.deposit_history.DepositHistoryRepository;
import com.zik00.admin.repository.member_management.deposit_request.DepositRequestMemberRepository;
import com.zik00.admin.repository.member_management.deposit_request.DepositRequestRepository;
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
public class DepositRequestService {
    private final DepositRequestRepository requestRepository;
    private final DepositRequestMemberRepository memberRepository;
    private final DepositHistoryRepository historyRepository;
    private final PiiEncryptionService encryptionService;

    public DepositRequestService(DepositRequestRepository requestRepository, DepositRequestMemberRepository memberRepository,
                                 DepositHistoryRepository historyRepository, PiiEncryptionService encryptionService) {
        this.requestRepository = requestRepository;
        this.memberRepository = memberRepository;
        this.historyRepository = historyRepository;
        this.encryptionService = encryptionService;
    }

    public List<DepositRequestResponse> findAll() {
        Map<Long, User> users = memberRepository.findAll().stream().collect(Collectors.toMap(User::getMemberId, Function.identity()));
        return requestRepository.findAllByOrderByRequestedAtDescIdDesc().stream().map(item -> toResponse(item, users.get(item.getMemberId()))).toList();
    }

    @Transactional
    public DepositRequestResponse approve(Long id, DepositRequestProcessRequest payload) {
        DepositRequest request = findRequest(id);
        User user = findMember(request.getMemberId());
        try {
            request.approve(payload.memo());
            user.adjustDepositBalance(request.getAmount());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        historyRepository.save(new DepositHistory(user.getMemberId(), "CHARGE", request.getAmount(), user.getDepositBalance(),
                "예치금 신청 승인", request.getId()));
        return toResponse(request, user);
    }

    @Transactional
    public DepositRequestResponse reject(Long id, DepositRequestProcessRequest payload) {
        DepositRequest request = findRequest(id);
        try {
            request.reject(payload.memo());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return toResponse(request, findMember(request.getMemberId()));
    }

    private DepositRequest findRequest(Long id) {
        return requestRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예치금 신청을 찾을 수 없습니다."));
    }

    private User findMember(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private DepositRequestResponse toResponse(DepositRequest item, User user) {
        return new DepositRequestResponse(item.getId(), item.getMemberId(), user == null ? "탈퇴 회원" : nameOf(user),
                user == null ? "-" : user.getLoginId(), item.getAmount(), item.getDepositorName(), item.getStatus(),
                item.getAdminMemo(), item.getRequestedAt(), item.getProcessedAt());
    }

    private String nameOf(User user) {
        String name = encryptionService.decrypt(user.getName());
        return name == null || name.isBlank() ? user.getNickname() : name;
    }
}
