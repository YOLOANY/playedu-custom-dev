/*
 * Copyright (C) 2023 杭州白书科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.playedu.common.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IdCardCrypto {
    private static final String PREFIX = "v1:";
    private static final String BIDX_DOMAIN = "playedu-id-card-bidx-v1\0";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] keyBytes;

    public IdCardCrypto(@Value("${playedu.security.id-card-key}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("ID card encryption key is required");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ID card encryption key must be valid Base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("ID card encryption key must decode to 32 bytes");
        }
        this.keyBytes = keyBytes;
    }

    private byte[] randomIv() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        cipher.init(mode, key, spec);
        return cipher;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public String encrypt(String plainText) {
        String normalized = normalize(plainText);
        if (normalized == null || normalized.isEmpty()) {
            return normalized;
        }
        try {
            byte[] iv = randomIv();
            byte[] encrypted =
                    cipher(Cipher.ENCRYPT_MODE, iv)
                            .doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            byte[] combined =
                    ByteBuffer.allocate(iv.length + encrypted.length)
                            .put(iv)
                            .put(encrypted)
                            .array();
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt ID card", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!cipherText.startsWith(PREFIX)) {
            throw new IllegalStateException("Unrecognized ID card ciphertext");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            if (decoded.length <= IV_LENGTH + GCM_TAG_LENGTH / 8) {
                throw new IllegalStateException("Invalid ID card ciphertext");
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            byte[] plainText = cipher(Cipher.DECRYPT_MODE, iv).doFinal(encrypted);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt ID card", e);
        }
    }

    public String blindIndex(String plainText) {
        String normalized = normalize(plainText);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] value = mac.doFinal((BIDX_DOMAIN + normalized).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to index ID card", e);
        }
    }
}
