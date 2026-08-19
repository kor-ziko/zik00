package com.zik00.admin.repository.member_management.withdrawn_member;

import com.zik00.shop.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnMemberRepository extends JpaRepository<User, Long> {
    List<User> findAllByMemberStatusOrderByWithdrawnAtDescMemberIdDesc(String memberStatus);
}
