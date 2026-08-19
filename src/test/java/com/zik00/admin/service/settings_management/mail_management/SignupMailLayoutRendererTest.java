package com.zik00.admin.service.settings_management.mail_management;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SignupMailLayoutRendererTest {
    private final SignupMailLayoutRenderer renderer = new SignupMailLayoutRenderer();

    @Test
    void rendersFixedSignupLayoutWithMemberInformation() {
        String html = renderer.render(
                "<p>가입을 축하합니다.</p>", "ZIK:00", "member01", "ZK000001",
                "010-1234-5678", "support@example.com"
        );

        assertTrue(html.contains("회원가입 완료"));
        assertTrue(html.contains("가입을 축하합니다."));
        assertTrue(html.contains("member01"));
        assertTrue(html.contains("010-1234-5678"));
        assertTrue(html.contains("ZK000001"));
        assertTrue(html.contains("support@example.com"));
    }

    @Test
    void escapesMemberValuesButKeepsManagedContentHtml() {
        String html = renderer.render(
                "<strong>안내</strong>", "ZIK:00", "<script>", "ZK000001", "", ""
        );

        assertTrue(html.contains("<strong>안내</strong>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
    }
}
