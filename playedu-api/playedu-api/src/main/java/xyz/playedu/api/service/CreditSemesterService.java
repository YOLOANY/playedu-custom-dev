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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreditSemesterService {
    private final String name;
    private final LocalDate start;
    private final LocalDate end;
    private final ZoneId zoneId;

    public CreditSemesterService(
            @Value("${playedu.credit.semester-name:2026-2027学年第一学期}") String name,
            @Value("${playedu.credit.semester-start:2026-09-01}") String start,
            @Value("${playedu.credit.semester-end:2027-01-15}") String end,
            @Value("${playedu.credit.time-zone:Asia/Shanghai}") String timeZone) {
        this.name = name;
        this.start = LocalDate.parse(start);
        this.end = LocalDate.parse(end);
        this.zoneId = ZoneId.of(timeZone);
        if (!this.end.isAfter(this.start)) {
            throw new IllegalArgumentException("Credit semester end must be after start");
        }
    }

    public String getName() {
        return name;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public String getTimeZone() {
        return zoneId.getId();
    }

    public Date getStartTime() {
        return Date.from(start.atStartOfDay(zoneId).toInstant());
    }

    public Date getEndTime() {
        return Date.from(end.atStartOfDay(zoneId).toInstant());
    }
}
