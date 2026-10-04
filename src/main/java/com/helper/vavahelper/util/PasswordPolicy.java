package com.helper.vavahelper.util;

import java.nio.charset.StandardCharsets;

/** Regras de senha usadas no cadastro e no reset. */
public final class PasswordPolicy {

    public static final String REQUIREMENTS =
            "must contain at least 8 characters (max 72 bytes), 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character";

    private static final int MIN_LENGTH = 8;
    // O BCrypt so considera os primeiros 72 bytes da senha.
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) return false;
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return false;

        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else if (!Character.isWhitespace(c)) special = true;
        }
        return upper && lower && digit && special;
    }
}
