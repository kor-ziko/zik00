package com.zik00.admin.controller.Web_management.footer_copyright;

import com.zik00.admin.dto.Web_management.HomepageContentRequest;
import com.zik00.admin.dto.Web_management.HomepageContentResponse;
import com.zik00.admin.service.Web_management.HomepageContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/web-management/footer-copyrights")
public class AdminFooterCopyrightController {
    private static final String TYPE="FOOTER_COPYRIGHT"; private final HomepageContentService service;
    public AdminFooterCopyrightController(HomepageContentService service){this.service=service;}
    @GetMapping public List<HomepageContentResponse> findAll(){return service.findByType(TYPE);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public HomepageContentResponse create(@Valid @RequestBody HomepageContentRequest r){return service.create(TYPE,r);}
    @PutMapping("/{id}") public HomepageContentResponse update(@PathVariable long id,@Valid @RequestBody HomepageContentRequest r){return service.update(TYPE,id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id){service.delete(TYPE,id);}
}
