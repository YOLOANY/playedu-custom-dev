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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

public final class IdCardValidator {
    private static final Pattern FORMAT =
            Pattern.compile(
                    "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9X]$");
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {
        '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'
    };

    private IdCardValidator() {}

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String idCard = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(idCard).matches()) {
            return false;
        }
        try {
            LocalDate.of(
                    Integer.parseInt(idCard.substring(6, 10)),
                    Integer.parseInt(idCard.substring(10, 12)),
                    Integer.parseInt(idCard.substring(12, 14)));
        } catch (DateTimeException e) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        return idCard.charAt(17) == CHECK_CODES[sum % 11];
    }
}
