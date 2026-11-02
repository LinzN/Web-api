/*
 * Copyright (c) 2026 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.linzn.webapi.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Password hashing (PBKDF2-HMAC-SHA256), random tokens and constant time comparison.
 */
public final class Secrets {

    /**
     * OWASP recommendation for PBKDF2-HMAC-SHA256 (2023).
     */
    public static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    /* compared against when the user does not exist, so the answer takes as long as for a real user */
    private static final String DUMMY_SALT = token(16);
    private static final String DUMMY_HASH = hash("dummy password", DUMMY_SALT, ITERATIONS);

    private Secrets() {
    }

    /**
     * @return random bytes as URL-safe base64 without padding
     */
    public static String token(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Short code that is easy to type: 4 groups of 4 characters without look-alikes (0/O, 1/l/I).
     */
    public static String code() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) {
                builder.append('-');
            }
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    public static String hash(String password, String salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    /**
     * @return true if the password matches; takes the same time when the user is unknown ({@code hash} null)
     */
    public static boolean verify(String password, String salt, int iterations, String hash) {
        if (hash == null || salt == null) {
            equal(hash(password, DUMMY_SALT, ITERATIONS), DUMMY_HASH);
            return false;
        }
        return equal(hash(password, salt, iterations), hash);
    }

    /**
     * SHA-256 of a token, stored instead of the token itself (session ids, API tokens, codes).
     */
    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean equal(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
