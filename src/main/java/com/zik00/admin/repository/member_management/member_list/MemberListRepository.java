package com.zik00.admin.repository.member_management.member_list;

import com.zik00.shop.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberListRepository extends JpaRepository<User, Long> {
    List<User> findAllByMemberStatusNotOrderByMemberIdAsc(String memberStatus);
}
