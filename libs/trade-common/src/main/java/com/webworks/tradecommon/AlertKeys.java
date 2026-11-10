package com.webworks.tradecommon;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class AlertKeys {

    private AlertKeys() {
    }

    public static String of(String rule, String traderId, String symbol, Instant windowStart) {
        try {
            String input = String.join("|", rule, traderId, String.valueOf(symbol), String.valueOf(windowStart));
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}