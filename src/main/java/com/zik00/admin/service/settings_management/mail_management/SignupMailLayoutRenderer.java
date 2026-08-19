package com.zik00.admin.service.settings_management.mail_management;

import org.springframework.stereotype.Component;

@Component
public class SignupMailLayoutRenderer {
    public String render(
            String content,
            String senderName,
            String loginId,
            String mailboxNumber,
            String mobilePhone,
            String replyTo
    ) {
        String companyName = display(senderName, "ZIK:00");
        String contact = escape(replyTo);
        String contactLine = contact.isBlank() ? "" : " | e-mail: " + contact;

        return """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background:#ffffff;color:#111111;font-family:Arial,'Noto Sans KR',sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="padding:6px;">
                    <tr><td align="center">
                      <table role="presentation" width="680" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:680px;background:#ffffff;border:1px solid #d1d5db;">
                        <tr><td style="padding:40px 40px 14px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                            <tr>
                              <td style="font-size:14px;color:#111111;">%s</td>
                              <td align="right" style="font-size:12px;color:#a3a3a3;">%s에서 보내는 메일입니다.</td>
                            </tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 40px;"><div style="height:2px;background:#14b8a6;"></div></td></tr>
                        <tr><td align="center" style="padding:20px 40px 16px;font-size:23px;font-weight:700;">회원가입 완료</td></tr>
                        <tr><td style="padding:0 40px;"><div style="height:1px;background:#d1d5db;"></div></td></tr>
                        <tr><td align="center" style="padding:32px 40px 24px;color:#111111;font-size:14px;line-height:1.75;">%s</td></tr>
                        <tr><td style="padding:0 40px 32px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#fafafa;border:1px solid #e5e7eb;padding:16px 20px;">
                            %s
                            %s
                            %s
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 40px;"><div style="height:1px;background:#bfc5ca;"></div></td></tr>
                        <tr><td style="padding:36px 40px 38px;color:#a3a3a3;font-size:11px;line-height:1.8;">
                          본메일은 고객님의 메일수신 동의에 의한 발신전용 메일입니다. 자세한 문의사항은 고객센터를 이용해 주시기 바랍니다.<br><br>
                          상호: %s%s<br>
                          Copyright &copy; ZIK:00 All rights reserved.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                companyName,
                companyName,
                content == null || content.isBlank() ? "회원가입을 축하드립니다.!!" : content,
                row("아이디", loginId),
                row("사서함", mailboxNumber),
                row("휴대폰", mobilePhone),
                companyName,
                contactLine
        );
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="width:80px;padding:5px 4px;color:#111111;font-size:13px;font-weight:700;">%s</td>
                  <td style="width:20px;padding:5px 4px;color:#111111;font-size:13px;">:</td>
                  <td style="padding:5px 4px;color:#111111;font-size:13px;">%s</td>
                </tr>
                """.formatted(escape(label), display(value, "-"));
    }

    private String display(String value, String fallback) {
        String escaped = escape(value);
        return escaped.isBlank() ? fallback : escaped;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
