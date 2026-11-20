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
package xyz.playedu.course.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import xyz.playedu.common.mapper.UserMapper;
import xyz.playedu.course.mapper.UserCreditRecordMapper;

@SpringBootTest(
        classes = UserCreditRecordConcurrencyIntegrationTest.TestApplication.class,
        properties = {
            "spring.main.web-application-type=none",
            "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/playedu_credit_learning_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
            "spring.datasource.username=${CREDIT_TEST_DB_USERNAME:root}",
            "spring.datasource.password=${CREDIT_TEST_DB_PASSWORD}",
            "spring.datasource.hikari.maximum-pool-size=10",
            "mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml",
            "mybatis-plus.global-config.banner=false"
        })
class UserCreditRecordConcurrencyIntegrationTest {

    @Autowired private UserCreditRecordServiceImpl creditRecordService;

    @Autowired private UserCreditRecordMapper creditRecordMapper;

    @Autowired private UserMapper userMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetCreditData() {
        jdbcTemplate.update("DELETE FROM user_credit_records");
        jdbcTemplate.update("UPDATE users SET credit1 = 0 WHERE id = 10");
    }

    @Test
    void grantsCreditOnce() {
        boolean granted = creditRecordService.grantCourseHourCredit(10, 20, 30);

        assertTrue(granted);
        assertEquals(5, userMapper.selectById(10).getCredit1());
        assertEquals(1L, creditRecordMapper.selectCount(null));
    }

    private boolean grantAfterStart(CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        return creditRecordService.grantCourseHourCredit(10, 20, 30);
    }

    @Test
    void grantsOnlyOnceWhenTwoThreadsFinishSameHour() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first =
                    executor.submit(() -> grantAfterStart(ready, start));
            Future<Boolean> second =
                    executor.submit(() -> grantAfterStart(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean firstGranted = first.get(10, TimeUnit.SECONDS);
            boolean secondGranted = second.get(10, TimeUnit.SECONDS);

            assertNotEquals(firstGranted, secondGranted);
            assertEquals(5, userMapper.selectById(10).getCredit1());
            assertEquals(1L, creditRecordMapper.selectCount(null));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {"xyz.playedu.common.mapper", "xyz.playedu.course.mapper"})
    @Import(UserCreditRecordServiceImpl.class)
    static class TestApplication {}
}
