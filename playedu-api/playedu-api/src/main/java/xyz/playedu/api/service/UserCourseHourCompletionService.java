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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.playedu.api.event.UserCourseHourFinishedEvent;
import xyz.playedu.course.service.UserCourseHourRecordService;

@Service
public class UserCourseHourCompletionService {
    @Autowired private UserCourseHourRecordService userCourseHourRecordService;

    @Autowired private ApplicationContext ctx;

    @Transactional(rollbackFor = Exception.class)
    public void complete(
            Integer userId,
            Integer courseId,
            Integer hourId,
            Integer duration,
            Integer totalDuration) {
        boolean isFinished =
                userCourseHourRecordService.storeOrUpdate(
                        userId, courseId, hourId, duration, totalDuration);
        if (isFinished) {
            ctx.publishEvent(new UserCourseHourFinishedEvent(this, userId, courseId, hourId));
        }
    }
}
