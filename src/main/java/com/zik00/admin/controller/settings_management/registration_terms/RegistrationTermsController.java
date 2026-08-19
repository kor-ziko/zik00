package com.zik00.admin.controller.settings_management.registration_terms;

import com.zik00.admin.controller.settings_management.common.SettingEntryControllerSupport;
import com.zik00.admin.service.settings_management.common.SettingEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings-management/registration-terms")
public class RegistrationTermsController extends SettingEntryControllerSupport {
    public RegistrationTermsController(SettingEntryService service) {
        super(service, "REGISTRATION_TERM", false);
    }
}
