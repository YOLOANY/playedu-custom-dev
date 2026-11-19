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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.mapper.UserMapper;
import xyz.playedu.common.types.paginate.PaginationResult;
import xyz.playedu.course.domain.Course;
import xyz.playedu.course.domain.CourseHour;
import xyz.playedu.course.domain.UserCreditRecord;
import xyz.playedu.course.mapper.CourseHourMapper;
import xyz.playedu.course.mapper.CourseMapper;
import xyz.playedu.course.mapper.UserCreditRecordMapper;
import xyz.playedu.course.service.UserCreditRecordService;

@Service
public class UserCreditRecordServiceImpl
        extends ServiceImpl<UserCreditRecordMapper, UserCreditRecord>
        implements UserCreditRecordService {
    private static final String CREDIT_UNIQUE_KEY = "uk_user_credit_user_hour";

    @Autowired private CourseMapper courseMapper;

    @Autowired private CourseHourMapper courseHourMapper;

    @Autowired private UserMapper userMapper;

    @Override
    public boolean grantCourseHourCredit(Integer userId, Integer courseId, Integer hourId) {
        Course course = courseMapper.selectById(courseId);
        CourseHour hour = courseHourMapper.selectById(hourId);
        if (course == null || hour == null || !courseId.equals(hour.getCourseId())) {
            throw new ServiceException("课时或课程不存在");
        }

        UserCreditRecord record = new UserCreditRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setHourId(hourId);
        record.setSourceType("COURSE_HOUR");
        record.setCredit(hour.getCredit() == null ? 0 : hour.getCredit());
        record.setCourseTitle(course.getTitle());
        record.setHourTitle(hour.getTitle());
        record.setCreatedAt(new Date());

        try {
            if (!save(record)) {
                throw new ServiceException("学分流水保存失败");
            }
        } catch (DuplicateKeyException e) {
            if (isCreditDuplicate(e)) {
                return false;
            }
            throw e;
        }

        if (userMapper.incrementCredit(userId, record.getCredit()) != 1) {
            throw new ServiceException("学员学分更新失败");
        }
        return true;
    }

    @Override
    public PaginationResult<UserCreditRecord> paginate(Integer userId, int page, int size) {
        int safePage = Math.min(Math.max(page, 1), 100000);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PaginationResult<UserCreditRecord> result = new PaginationResult<>();
        result.setData(
                baseMapper.paginate(userId, (safePage - 1) * safeSize, safeSize));
        result.setTotal(baseMapper.paginateCount(userId));
        return result;
    }

    @Override
    public Long sumCredit(Integer userId, Date startAt, Date endAt) {
        return baseMapper.sumCredit(userId, startAt, endAt);
    }

    private boolean isCreditDuplicate(DuplicateKeyException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(CREDIT_UNIQUE_KEY)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
