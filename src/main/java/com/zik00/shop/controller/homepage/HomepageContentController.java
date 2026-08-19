package com.zik00.shop.controller.homepage;

import com.zik00.admin.dto.Web_management.HomepageContentResponse;
import com.zik00.admin.service.Web_management.HomepageContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/homepage-content")
public class HomepageContentController {
    private final HomepageContentService service;

    public HomepageContentController(HomepageContentService service) { this.service = service; }

    @GetMapping
    public List<HomepageContentResponse> findActive() { return service.findActive(); }
}
