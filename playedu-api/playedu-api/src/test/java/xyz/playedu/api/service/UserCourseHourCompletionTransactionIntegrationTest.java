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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import xyz.playedu.api.listener.UserCourseHourFinishedListener;
import xyz.playedu.api.listener.UserCreditRecordListener;
import xyz.playedu.common.util.IdCardCrypto;
import xyz.playedu.course.service.CourseHourService;
import xyz.playedu.course.service.impl.UserCourseHourRecordServiceImpl;
import xyz.playedu.course.service.impl.UserCourseRecordServiceImpl;
import xyz.playedu.course.service.impl.UserCreditRecordServiceImpl;

@SpringBootTest(
        classes = UserCourseHourCompletionTransactionIntegrationTest.TestApplication.class,
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
class UserCourseHourCompletionTransactionIntegrationTest {

    @Autowired private UserCourseHourCompletionService completionService;

    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean private CourseHourService courseHourService;

    @MockBean private IdCardCrypto idCardCrypto;

    @SpyBean private UserCourseRecordServiceImpl courseRecordService;

    @SpyBean private UserCreditRecordServiceImpl creditRecordService;

    @BeforeEach
    void resetLearningData() {
        jdbcTemplate.update("DELETE FROM user_credit_records");
        jdbcTemplate.update("DELETE FROM user_course_hour_records");
        jdbcTemplate.update("DELETE FROM user_course_records");
        jdbcTemplate.update("UPDATE users SET credit1 = 0 WHERE id = 10");
        when(courseHourService.getCountByCourseId(20)).thenReturn(1);
    }

    @Test
    void commitsCompletionProgressCreditAndBalanceTogether() {
        completionService.complete(10, 20, 30, 600, 600);

        assertEquals(1L, count("user_course_hour_records"));
        assertEquals(1L, count("user_course_records"));
        assertEquals(1L, count("user_credit_records"));
        assertEquals(5, creditBalance());
        assertEquals(
                10000,
                jdbcTemplate.queryForObject(
                        "SELECT progress FROM user_course_records WHERE user_id = 10 AND course_id = 20",
                        Integer.class));
    }

    @Test
    void rollsBackWhenProgressUpdateFailsAfterWriting() {
        doAnswer(
                        invocation -> {
                            invocation.callRealMethod();
                            throw new IllegalStateException("progress failure after write");
                        })
                .when(courseRecordService)
                .storeOrUpdate(10, 20, 1, 1);

        assertThrows(
                IllegalStateException.class,
                () -> completionService.complete(10, 20, 30, 600, 600));

        assertAllWritesRolledBack();
    }

    @Test
    void rollsBackWhenCreditProcessingFailsAfterWriting() {
        doAnswer(
                        invocation -> {
                            invocation.callRealMethod();
                            throw new IllegalStateException("credit failure after write");
                        })
                .when(creditRecordService)
                .grantCourseHourCredit(10, 20, 30);

        assertThrows(
                IllegalStateException.class,
                () -> completionService.complete(10, 20, 30, 600, 600));

        assertAllWritesRolledBack();
    }

    private void assertAllWritesRolledBack() {
        assertEquals(0L, count("user_course_hour_records"));
        assertEquals(0L, count("user_course_records"));
        assertEquals(0L, count("user_credit_records"));
        assertEquals(0, creditBalance());
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private Integer creditBalance() {
        return jdbcTemplate.queryForObject(
                "SELECT credit1 FROM users WHERE id = 10", Integer.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {"xyz.playedu.common.mapper", "xyz.playedu.course.mapper"})
    @Import({
        UserCourseHourCompletionService.class,
        UserCourseHourRecordServiceImpl.class,
        UserCourseRecordServiceImpl.class,
        UserCreditRecordServiceImpl.class,
        UserCourseHourFinishedListener.class,
        UserCreditRecordListener.class
    })
    static class TestApplication {}
}
