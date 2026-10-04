package com.helper.vavahelper.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 em hexadecimal. Usado para nunca guardar o token de reset em texto puro. */
public final class TokenHasher {

    private TokenHasher() {}

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e obrigatorio em toda JVM
            throw new IllegalStateException(e);
        }
    }
}
