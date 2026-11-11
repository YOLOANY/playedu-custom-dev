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
package xyz.playedu.system.checks;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import xyz.playedu.common.util.IdCardCrypto;

@Component
@Slf4j
@ConditionalOnProperty(name = "playedu.security.id-card-migration-enabled", havingValue = "true")
public class IdCardMigrationRunner implements CommandLineRunner {
    private static final String PREFIX = "v1:";
    private static final int BATCH_SIZE = 500;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private IdCardCrypto idCardCrypto;

    @Override
    public void run(String... args) {
        long lastId = 0;
        int checked = 0;
        int updated = 0;
        while (true) {
            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(
                            "SELECT id, id_card, id_card_bidx FROM users WHERE id > ? ORDER BY id"
                                    + " LIMIT ?",
                            lastId,
                            BATCH_SIZE);
            if (rows.isEmpty()) {
                break;
            }
            for (Map<String, Object> row : rows) {
                long id = ((Number) row.get("id")).longValue();
                String storedValue = (String) row.get("id_card");
                String storedBidx = (String) row.get("id_card_bidx");
                String sourceValue = storedValue == null ? "" : storedValue;
                String plainText =
                        sourceValue.startsWith(PREFIX)
                                ? idCardCrypto.decrypt(sourceValue)
                                : sourceValue;
                String encrypted =
                        sourceValue.startsWith(PREFIX)
                                ? sourceValue
                                : idCardCrypto.encrypt(plainText);
                String blindIndex = idCardCrypto.blindIndex(plainText);
                if (!Objects.equals(storedValue, encrypted)
                        || !Objects.equals(storedBidx, blindIndex)) {
                    jdbcTemplate.update(
                            "UPDATE users SET id_card = ?, id_card_bidx = ? WHERE id = ?",
                            encrypted,
                            blindIndex,
                            id);
                    updated++;
                }
                checked++;
                lastId = id;
            }
        }
        log.info("ID card migration completed: {} rows checked, {} rows updated", checked, updated);
    }
}
