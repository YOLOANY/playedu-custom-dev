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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.playedu.common.domain.Department;
import xyz.playedu.common.exception.NotFoundException;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.service.DepartmentService;
import xyz.playedu.common.service.UserService;
import xyz.playedu.common.types.paginate.PaginationResult;
import xyz.playedu.course.bus.UserBus;
import xyz.playedu.course.domain.Course;
import xyz.playedu.course.domain.UserCourseFavorite;
import xyz.playedu.course.mapper.UserCourseFavoriteMapper;
import xyz.playedu.course.service.CourseService;

@ExtendWith(MockitoExtension.class)
class UserCourseFavoriteServiceImplTest {
    @Mock private CourseService courseService;
    @Mock private UserBus userBus;
    @Mock private UserService userService;
    @Mock private DepartmentService departmentService;
    @Mock private UserCourseFavoriteMapper favoriteMapper;

    @Spy @InjectMocks private UserCourseFavoriteServiceImpl service;

    private Course course;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", favoriteMapper);
        course = new Course();
        course.setId(20);
        course.setTitle("合成课程");
        course.setIsShow(1);
    }

    @Test
    void savesFavoriteForAccessibleCourse() throws Exception {
        allowVisibleCourse();
        doReturn(true).when(service).save(any(UserCourseFavorite.class));

        service.favorite(10, 20);

        ArgumentCaptor<UserCourseFavorite> captor =
                ArgumentCaptor.forClass(UserCourseFavorite.class);
        verify(service).save(captor.capture());
        assertEquals(10, captor.getValue().getUserId());
        assertEquals(20, captor.getValue().getCourseId());
        assertNotNull(captor.getValue().getCreatedAt());
        verify(userBus).canSeeCourse(10, 20);
    }

    @Test
    void rejectsMissingHiddenAndInaccessibleCourses() throws Exception {
        doThrow(new NotFoundException("课程不存在")).when(courseService).findOrFail(20);
        assertThrows(NotFoundException.class, () -> service.favorite(10, 20));

        doReturn(course).when(courseService).findOrFail(20);
        course.setIsShow(0);
        assertThrows(ServiceException.class, () -> service.favorite(10, 20));
        verify(userBus, never()).canSeeCourse(anyInt(), anyInt());

        course.setIsShow(1);
        when(userBus.canSeeCourse(10, 20)).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.favorite(10, 20));
        verify(service, never()).save(any(UserCourseFavorite.class));
    }

    @Test
    void ignoresOnlyTheFavoriteUniqueKeyConflict() throws Exception {
        allowVisibleCourse();
        DuplicateKeyException duplicate =
                new DuplicateKeyException(
                        "insert failed",
                        new SQLException("Duplicate entry for key uk_user_course_favorite"));
        doThrow(duplicate).when(service).save(any(UserCourseFavorite.class));

        assertDoesNotThrow(() -> service.favorite(10, 20));
    }

    @Test
    void propagatesOtherUniqueKeyConflicts() throws Exception {
        allowVisibleCourse();
        doThrow(new DuplicateKeyException("Duplicate entry for key another_unique_key"))
                .when(service)
                .save(any(UserCourseFavorite.class));

        assertThrows(DuplicateKeyException.class, () -> service.favorite(10, 20));
    }

    @Test
    void failsWhenFavoriteIsNotSaved() throws Exception {
        allowVisibleCourse();
        doReturn(false).when(service).save(any(UserCourseFavorite.class));

        assertThrows(ServiceException.class, () -> service.favorite(10, 20));
    }

    @Test
    void unfavoriteIsIdempotentWithoutCoursePermissionChecks() {
        doReturn(false).when(service).remove(any());

        service.unfavorite(10, 20);

        verify(service).remove(any());
        verifyNoInteractions(courseService, userBus);
    }

    @Test
    void readsFavoriteStateForCurrentStudent() {
        doReturn(1L, 0L).when(service).count(any());

        assertTrue(service.isFavorite(10, 20));
        assertFalse(service.isFavorite(10, 20));
    }

    @Test
    void paginatesWithExpandedDeduplicatedDepartmentIds() {
        Department first = department(3, "1,2");
        Department second = department(4, "2,5");
        when(userService.getDepIdsByUserId(10)).thenReturn(List.of(3, 4));
        when(departmentService.chunk(List.of(3, 4))).thenReturn(List.of(first, second));
        when(favoriteMapper.paginate(anyInt(), anyList(), anyInt(), anyInt()))
                .thenReturn(List.of(course));
        when(favoriteMapper.paginateCount(anyInt(), anyList())).thenReturn(1L);

        PaginationResult<Course> result = service.paginate(10, 20, 1000);

        List<Integer> expectedDepIds = List.of(3, 1, 2, 4, 5);
        verify(favoriteMapper).paginate(10, expectedDepIds, 1900, 100);
        verify(favoriteMapper).paginateCount(10, expectedDepIds);
        assertEquals(1L, result.getTotal());
        assertEquals(List.of(course), result.getData());
    }

    @Test
    void paginatesOpenCoursesWhenStudentHasNoDepartment() {
        when(userService.getDepIdsByUserId(10)).thenReturn(List.of());
        when(favoriteMapper.paginate(anyInt(), anyList(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(favoriteMapper.paginateCount(anyInt(), anyList())).thenReturn(0L);

        PaginationResult<Course> result = service.paginate(10, 0, 0);

        verify(favoriteMapper).paginate(eq(10), eq(List.of()), eq(0), eq(1));
        verify(favoriteMapper).paginateCount(10, List.of());
        verifyNoInteractions(departmentService);
        assertEquals(0L, result.getTotal());
    }

    private void allowVisibleCourse() throws NotFoundException {
        when(courseService.findOrFail(20)).thenReturn(course);
        when(userBus.canSeeCourse(10, 20)).thenReturn(true);
    }

    private Department department(Integer id, String parentChain) {
        Department department = new Department();
        department.setId(id);
        department.setParentChain(parentChain);
        return department;
    }
}
