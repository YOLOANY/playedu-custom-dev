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
package xyz.playedu.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class CreditSemesterServiceTest {
    @Test
    void convertsSemesterDatesUsingConfiguredTimeZone() {
        CreditSemesterService service =
                new CreditSemesterService(
                        "2026-2027学年第一学期", "2026-09-01", "2027-01-15", "Asia/Shanghai");

        assertEquals("2026-2027学年第一学期", service.getName());
        assertEquals("2026-09-01", service.getStart().toString());
        assertEquals("2027-01-15", service.getEnd().toString());
        assertEquals("Asia/Shanghai", service.getTimeZone());
        assertEquals(Instant.parse("2026-08-31T16:00:00Z"), service.getStartTime().toInstant());
        assertEquals(Instant.parse("2027-01-14T16:00:00Z"), service.getEndTime().toInstant());
    }

    @Test
    void rejectsEmptyOrReversedSemesterRanges() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreditSemesterService("学期", "2026-09-01", "2026-09-01", "Asia/Shanghai"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreditSemesterService("学期", "2027-01-15", "2026-09-01", "Asia/Shanghai"));
    }

    @Test
    void rejectsInvalidDatesAndTimeZones() {
        assertThrows(
                DateTimeParseException.class,
                () -> new CreditSemesterService("学期", "2026-02-30", "2027-01-15", "Asia/Shanghai"));
        assertThrows(
                DateTimeException.class,
                () -> new CreditSemesterService("学期", "2026-09-01", "2027-01-15", "Invalid/Zone"));
    }
}
