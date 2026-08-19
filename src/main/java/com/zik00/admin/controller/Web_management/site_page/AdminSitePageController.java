package com.zik00.admin.controller.Web_management.site_page;

import com.zik00.admin.dto.Web_management.site_page.SitePageResponse;
import com.zik00.admin.service.Web_management.site_page.SitePageCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/web-management/site-pages")
public class AdminSitePageController {
    private final SitePageCatalogService service;

    public AdminSitePageController(SitePageCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<SitePageResponse> findAll() {
        return service.findAll();
    }
}
