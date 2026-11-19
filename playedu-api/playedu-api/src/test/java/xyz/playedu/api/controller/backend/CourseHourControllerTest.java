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
package xyz.playedu.api.controller.backend;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.playedu.api.request.backend.CourseHourRequest;
import xyz.playedu.common.exception.NotFoundException;
import xyz.playedu.course.domain.CourseChapter;
import xyz.playedu.course.domain.CourseHour;
import xyz.playedu.course.service.CourseChapterService;
import xyz.playedu.course.service.CourseHourService;

@ExtendWith(MockitoExtension.class)
class CourseHourControllerTest {
    @Mock private CourseHourService hourService;
    @Mock private CourseChapterService chapterService;

    @InjectMocks private CourseHourController controller;

    @Test
    void updatesHourWithoutChapterLookupWhenChapterIdIsZero() throws NotFoundException {
        CourseHour hour = new CourseHour();
        hour.setId(20);
        when(hourService.findOrFail(20, 10)).thenReturn(hour);

        controller.update(10, 20, request(0));

        verify(chapterService, never()).findOrFail(anyInt(), anyInt());
        verify(hourService).update(hour, 0, 2, "课时", 600, 3);
    }

    @Test
    void validatesChapterWhenHourBelongsToChapter() throws NotFoundException {
        CourseHour hour = new CourseHour();
        hour.setId(20);
        CourseChapter chapter = new CourseChapter();
        chapter.setId(30);
        when(hourService.findOrFail(20, 10)).thenReturn(hour);
        when(chapterService.findOrFail(30, 10)).thenReturn(chapter);

        controller.update(10, 20, request(30));

        verify(chapterService).findOrFail(30, 10);
        verify(hourService).update(hour, 30, 2, "课时", 600, 3);
    }

    private CourseHourRequest request(int chapterId) {
        CourseHourRequest request = new CourseHourRequest();
        request.setChapterId(chapterId);
        request.setSort(2);
        request.setTitle("课时");
        request.setType("video");
        request.setDuration(600);
        request.setRid(40);
        request.setCredit(3);
        return request;
    }
}
