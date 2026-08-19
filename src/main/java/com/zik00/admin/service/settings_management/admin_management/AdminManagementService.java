package com.zik00.admin.service.settings_management.admin_management;

import com.zik00.admin.domain.AdminUser;
import com.zik00.admin.dto.settings_management.admin_management.AdminManagementRequest;
import com.zik00.admin.dto.settings_management.admin_management.AdminManagementResponse;
import com.zik00.admin.repository.AdminUserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class AdminManagementService {
    private final AdminUserRepository repository;private final PasswordEncoder encoder;
    public AdminManagementService(AdminUserRepository repository,PasswordEncoder encoder){this.repository=repository;this.encoder=encoder;}
    public List<AdminManagementResponse> findAll(){return repository.findAll(Sort.by("adminId")).stream().map(AdminManagementResponse::from).toList();}
    @Transactional public AdminManagementResponse create(AdminManagementRequest request){if(repository.findByLoginId(request.loginId().trim()).isPresent())throw bad("이미 사용 중인 아이디입니다.");if(request.password()==null||request.password().length()<8)throw bad("비밀번호는 8자 이상이어야 합니다.");return AdminManagementResponse.from(repository.save(new AdminUser(request.loginId().trim(),encoder.encode(request.password()),request.name().trim(),request.active())));}
    @Transactional public AdminManagementResponse update(long id,AdminManagementRequest request){AdminUser user=find(id);if(!user.getLoginId().equals(request.loginId().trim()))throw bad("관리자 아이디는 변경할 수 없습니다.");user.update(request.name().trim(),request.active());if(request.password()!=null&&!request.password().isBlank()){if(request.password().length()<8)throw bad("비밀번호는 8자 이상이어야 합니다.");user.changePassword(encoder.encode(request.password()));}return AdminManagementResponse.from(user);}
    @Transactional public void delete(long id,long currentId){if(id==currentId)throw bad("현재 로그인한 관리자 계정은 삭제할 수 없습니다.");repository.delete(find(id));}
    private AdminUser find(long id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"관리자를 찾을 수 없습니다."));}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
