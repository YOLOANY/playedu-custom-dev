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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.mapper.UserMapper;
import xyz.playedu.course.domain.Course;
import xyz.playedu.course.domain.CourseHour;
import xyz.playedu.course.domain.UserCreditRecord;
import xyz.playedu.course.mapper.CourseHourMapper;
import xyz.playedu.course.mapper.CourseMapper;
import xyz.playedu.course.mapper.UserCreditRecordMapper;

@ExtendWith(MockitoExtension.class)
class UserCreditRecordServiceImplTest {
    @Mock private CourseMapper courseMapper;
    @Mock private CourseHourMapper courseHourMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserCreditRecordMapper userCreditRecordMapper;

    @Spy @InjectMocks private UserCreditRecordServiceImpl service;

    private Course course;
    private CourseHour hour;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(20);
        course.setTitle("数据结构");
        hour = new CourseHour();
        hour.setId(30);
        hour.setCourseId(20);
        hour.setTitle("线性表");
        hour.setCredit(3);
        when(courseMapper.selectById(20)).thenReturn(course);
        when(courseHourMapper.selectById(30)).thenReturn(hour);
    }

    @Test
    void grantsCreditAndStoresSnapshots() {
        doReturn(true).when(service).save(any(UserCreditRecord.class));
        when(userMapper.incrementCredit(10, 3)).thenReturn(1);

        assertTrue(service.grantCourseHourCredit(10, 20, 30));

        ArgumentCaptor<UserCreditRecord> captor = ArgumentCaptor.forClass(UserCreditRecord.class);
        verify(service).save(captor.capture());
        UserCreditRecord record = captor.getValue();
        assertEquals(10, record.getUserId());
        assertEquals(20, record.getCourseId());
        assertEquals(30, record.getHourId());
        assertEquals(3, record.getCredit());
        assertEquals("COURSE_HOUR", record.getSourceType());
        assertEquals("数据结构", record.getCourseTitle());
        assertEquals("线性表", record.getHourTitle());
        verify(userMapper).incrementCredit(10, 3);
    }

    @Test
    void grantsZeroCreditWithZeroValuedLedgerRecord() {
        hour.setCredit(null);
        doReturn(true).when(service).save(any(UserCreditRecord.class));
        when(userMapper.incrementCredit(10, 0)).thenReturn(1);

        assertTrue(service.grantCourseHourCredit(10, 20, 30));

        ArgumentCaptor<UserCreditRecord> captor = ArgumentCaptor.forClass(UserCreditRecord.class);
        verify(service).save(captor.capture());
        assertEquals(0, captor.getValue().getCredit());
        verify(userMapper).incrementCredit(10, 0);
    }

    @Test
    void ignoresOnlyTheCreditUniqueKeyConflict() {
        DuplicateKeyException duplicate =
                new DuplicateKeyException(
                        "insert failed",
                        new SQLException("Duplicate entry for key uk_user_credit_user_hour"));
        doThrow(duplicate).when(service).save(any(UserCreditRecord.class));

        assertFalse(service.grantCourseHourCredit(10, 20, 30));

        verify(userMapper, never()).incrementCredit(anyInt(), anyInt());
    }

    @Test
    void propagatesOtherUniqueKeyConflicts() {
        doThrow(new DuplicateKeyException("Duplicate entry for key another_unique_key"))
                .when(service)
                .save(any(UserCreditRecord.class));

        assertThrows(
                DuplicateKeyException.class,
                () -> service.grantCourseHourCredit(10, 20, 30));
        verify(userMapper, never()).incrementCredit(anyInt(), anyInt());
    }

    @Test
    void failsWhenStudentBalanceIsNotUpdated() {
        doReturn(true).when(service).save(any(UserCreditRecord.class));
        when(userMapper.incrementCredit(10, 3)).thenReturn(0);

        assertThrows(
                ServiceException.class, () -> service.grantCourseHourCredit(10, 20, 30));
    }
}
