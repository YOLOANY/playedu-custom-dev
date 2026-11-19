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
package xyz.playedu.api.controller.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.playedu.common.context.FCtx;
import xyz.playedu.course.domain.Course;
import xyz.playedu.course.domain.CourseHour;
import xyz.playedu.course.domain.UserCourseHourRecord;
import xyz.playedu.course.domain.UserLatestLearn;
import xyz.playedu.course.service.CourseHourService;
import xyz.playedu.course.service.CourseService;
import xyz.playedu.course.service.UserCourseHourRecordService;
import xyz.playedu.course.service.UserCourseRecordService;
import xyz.playedu.resource.service.ResourceService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock private UserCourseHourRecordService userCourseHourRecordService;
    @Mock private CourseService courseService;
    @Mock private CourseHourService hourService;
    @Mock private UserCourseRecordService userCourseRecordService;
    @Mock private ResourceService resourceService;

    @InjectMocks private UserController controller;

    @BeforeEach
    void setUp() {
        FCtx.setId(7);
    }

    @AfterEach
    void tearDown() {
        FCtx.remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsOnlyLatestHourWhenCourseHasDuplicateRecords() {
        UserCourseHourRecord latest = record(12, 1, 12);
        UserCourseHourRecord older = record(2, 1, 2);
        when(userCourseHourRecordService.getLatestCourseIds(7, 100))
                .thenReturn(List.of(latest, older));

        Course course = new Course();
        course.setId(1);
        course.setThumb(0);
        when(courseService.chunks(eq(List.of(1)), anyList())).thenReturn(List.of(course));

        CourseHour latestHour = new CourseHour();
        latestHour.setId(12);
        CourseHour olderHour = new CourseHour();
        olderHour.setId(2);
        when(hourService.chunk(List.of(12, 2))).thenReturn(List.of(latestHour, olderHour));
        when(userCourseRecordService.chunk(eq(7), eq(List.of(1)))).thenReturn(List.of());
        when(resourceService.chunksPreSignUrlByIds(anyList())).thenReturn(Map.of());

        Map<String, Object> data = (Map<String, Object>) controller.latestLearn().getData();
        List<UserLatestLearn> learns =
                (List<UserLatestLearn>) data.get("user_latest_learns");

        assertEquals(1, learns.size());
        assertEquals(12, learns.get(0).getHourRecord().getHourId());
        assertEquals(12, learns.get(0).getLastLearnHour().getId());
    }

    private UserCourseHourRecord record(int id, int courseId, int hourId) {
        UserCourseHourRecord record = new UserCourseHourRecord();
        record.setId(id);
        record.setCourseId(courseId);
        record.setHourId(hourId);
        return record;
    }
}
