package com.zik00.admin.controller.settings_management.signup_mail;
import com.zik00.admin.controller.settings_management.common.SettingEntryControllerSupport;import com.zik00.admin.service.settings_management.common.SettingEntryService;import org.springframework.web.bind.annotation.RequestMapping;import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/admin/settings-management/signup-mail") public class SignupMailController extends SettingEntryControllerSupport{public SignupMailController(SettingEntryService s){super(s,"SIGNUP_MAIL",true);}}
