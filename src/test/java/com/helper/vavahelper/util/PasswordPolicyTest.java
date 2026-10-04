package com.helper.vavahelper.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertTrue(PasswordPolicy.isValid("Abcdef1!"));
        assertTrue(PasswordPolicy.isValid("Senha-Forte#2026"));
    }

    @Test
    void rejectsWeakPasswords() {
        assertFalse(PasswordPolicy.isValid(null));
        assertFalse(PasswordPolicy.isValid(""));
        assertFalse(PasswordPolicy.isValid("123"));
        assertFalse(PasswordPolicy.isValid("Abc1!"));          // curta
        assertFalse(PasswordPolicy.isValid("abcdefg1!"));       // sem maiuscula
        assertFalse(PasswordPolicy.isValid("ABCDEFG1!"));       // sem minuscula
        assertFalse(PasswordPolicy.isValid("Abcdefgh!"));       // sem numero
        assertFalse(PasswordPolicy.isValid("Abcdefg12"));       // sem especial
    }

    @Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        assertFalse(PasswordPolicy.isValid("Aa1!" + "x".repeat(70)));
    }
}
