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

class IdCardValidatorTest {
    @Test
    void acceptsValidEighteenDigitId() {
        assertTrue(IdCardValidator.isValid("11010519491231002X"));
        assertTrue(IdCardValidator.isValid(" 11010519491231002x "));
    }

    @Test
    void rejectsInvalidFormatsDatesAndChecksums() {
        assertFalse(IdCardValidator.isValid(null));
        assertFalse(IdCardValidator.isValid(""));
        assertFalse(IdCardValidator.isValid("110105491231002"));
        assertFalse(IdCardValidator.isValid("110105194912310021"));
        assertFalse(IdCardValidator.isValid("11010519490230002X"));
        assertFalse(IdCardValidator.isValid("11010519491301002X"));
        assertFalse(IdCardValidator.isValid("11010519491231A02X"));
    }
}
