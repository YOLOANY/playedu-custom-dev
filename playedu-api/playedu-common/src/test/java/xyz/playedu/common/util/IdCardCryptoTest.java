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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IdCardCryptoTest {
    private static final String TEST_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
    private static final String TEST_ID = "11010519491231002X";
    private final IdCardCrypto crypto = new IdCardCrypto(TEST_KEY);

    @Test
    void encryptsWithRandomIvAndRestoresNormalizedPlaintext() {
        String first = crypto.encrypt(" 11010519491231002x ");
        String second = crypto.encrypt(TEST_ID);
        assertNotEquals(first, second);
        assertTrue(first.startsWith("v1:"));
        assertEquals(67, first.length());
        assertEquals(TEST_ID, crypto.decrypt(first));
        assertEquals(TEST_ID, crypto.decrypt(second));
    }

    @Test
    void rejectsTamperingAndLegacyPlaintext() {
        String encrypted = crypto.encrypt(TEST_ID);
        int index = encrypted.length() - 3;
        char replacement = encrypted.charAt(index) == 'A' ? 'B' : 'A';
        String tampered =
                encrypted.substring(0, index) + replacement + encrypted.substring(index + 1);
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(tampered));
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(TEST_ID));
    }

    @Test
    void createsStableNormalizedBlindIndex() {
        String index = crypto.blindIndex(" 11010519491231002x ");
        assertEquals(index, crypto.blindIndex(TEST_ID));
        assertEquals(64, index.length());
        assertTrue(index.matches("[0-9a-f]{64}"));
        assertNull(crypto.blindIndex(" "));
    }

    @Test
    void handlesEmptyValuesAndRejectsInvalidKeys() {
        assertNull(crypto.encrypt(null));
        assertEquals("", crypto.encrypt(" "));
        assertNull(crypto.decrypt(null));
        assertEquals("", crypto.decrypt(""));
        assertThrows(IllegalStateException.class, () -> new IdCardCrypto(""));
        assertThrows(IllegalStateException.class, () -> new IdCardCrypto("invalid"));
        assertThrows(IllegalStateException.class, () -> new IdCardCrypto("AAECAwQ="));
    }
}
