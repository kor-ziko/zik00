package com.zik00.shop.util;

public final class PhoneNumberFormatter {
    private PhoneNumberFormatter() {
    }

    public static String formatTelephone(String value) {
        String digits = digits(value);
        if (digits.startsWith("02") && digits.length() == 9) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
        }
        if (digits.startsWith("02") && digits.length() == 10) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return value == null ? "" : value.trim();
    }

    public static String formatMobilePhone(String value) {
        String digits = digits(value);
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        return value == null ? "" : value.trim();
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
