package com.zik00.admin.controller.settings_management.company_info;
import com.zik00.admin.controller.settings_management.common.SettingEntryControllerSupport;import com.zik00.admin.service.settings_management.common.SettingEntryService;import org.springframework.web.bind.annotation.RequestMapping;import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/admin/settings-management/company-info") public class CompanyInfoController extends SettingEntryControllerSupport{public CompanyInfoController(SettingEntryService s){super(s,"COMPANY_INFO",true);}}
