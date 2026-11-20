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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import xyz.playedu.common.domain.Department;
import xyz.playedu.common.service.DepartmentService;
import xyz.playedu.common.service.UserService;
import xyz.playedu.common.types.paginate.PaginationResult;
import xyz.playedu.course.bus.UserBus;
import xyz.playedu.course.domain.Course;
import xyz.playedu.course.service.CourseService;

@SpringBootTest(
        classes = UserCourseFavoriteAccessIntegrationTest.TestApplication.class,
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
@Sql(
        scripts = "classpath:favorite-test-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserCourseFavoriteAccessIntegrationTest {
    private static final int USER_ID = 910;

    @Autowired private UserCourseFavoriteServiceImpl favoriteService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SqlSessionFactory sqlSessionFactory;

    @MockBean private CourseService courseService;
    @MockBean private UserBus userBus;
    @MockBean private UserService userService;
    @MockBean private DepartmentService departmentService;

    @BeforeEach
    void resetSyntheticData() {
        jdbcTemplate.update("DELETE FROM user_course_favorites WHERE user_id = ?", USER_ID);
        jdbcTemplate.update(
                "DELETE FROM course_department_user WHERE course_id BETWEEN 9000 AND 9099");
        jdbcTemplate.update("DELETE FROM courses WHERE id BETWEEN 9000 AND 9099");
    }

    @Test
    void filtersHiddenAndInaccessibleCoursesBeforeCounting() {
        insertCourse(9001, "开放课程", 1);
        insertCourse(9002, "父部门课程", 1);
        insertCourse(9003, "子部门课程", 1);
        insertCourse(9004, "下架课程", 0);
        insertCourse(9005, "多重指派课程", 1);
        for (int courseId = 9001; courseId <= 9005; courseId++) {
            insertFavorite(courseId, courseId);
        }
        assign(9002, 1);
        assign(9003, 11);
        assign(9005, 2);
        assign(9005, 2);
        when(userService.getDepIdsByUserId(USER_ID)).thenReturn(List.of(10));
        when(departmentService.chunk(List.of(10)))
                .thenReturn(List.of(department(10, "1,2")));

        PaginationResult<Course> result = favoriteService.paginate(USER_ID, 1, 20);

        assertEquals(3L, result.getTotal());
        assertEquals(
                Set.of(9001, 9002, 9005),
                result.getData().stream().map(Course::getId).collect(Collectors.toSet()));
    }

    @Test
    void retainsFavoriteAfterCourseDeletionAndReadsCurrentCourseData() {
        withoutDepartment();
        insertCourse(9001, "旧标题", 1);
        insertFavorite(9001, 1);
        jdbcTemplate.update(
                "UPDATE courses SET title = ?, thumb = ? WHERE id = ?",
                "当前标题",
                88,
                9001);

        PaginationResult<Course> current = favoriteService.paginate(USER_ID, 1, 20);
        assertEquals("当前标题", current.getData().get(0).getTitle());
        assertEquals(88, current.getData().get(0).getThumb());

        jdbcTemplate.update("DELETE FROM courses WHERE id = ?", 9001);
        PaginationResult<Course> deleted = favoriteService.paginate(USER_ID, 1, 20);
        assertEquals(0L, deleted.getTotal());
        assertTrue(deleted.getData().isEmpty());
        assertEquals(1L, favoriteCount());
    }

    @Test
    void returnsFullPagesAndMatchingTotalWithMixedPermissions() {
        withoutDepartment();
        int sequence = 0;
        for (int courseId = 9001; courseId <= 9025; courseId++) {
            insertCourse(courseId, "可访问课程" + courseId, 1);
            insertFavorite(courseId, ++sequence);
        }
        for (int courseId = 9026; courseId <= 9030; courseId++) {
            insertCourse(courseId, "下架课程" + courseId, 0);
            insertFavorite(courseId, ++sequence);
        }
        for (int courseId = 9031; courseId <= 9035; courseId++) {
            insertCourse(courseId, "无权限课程" + courseId, 1);
            assign(courseId, 99);
            insertFavorite(courseId, ++sequence);
        }
        insertCourse(9036, "待删除课程一", 1);
        insertCourse(9037, "待删除课程二", 1);
        insertFavorite(9036, ++sequence);
        insertFavorite(9037, ++sequence);
        jdbcTemplate.update("DELETE FROM courses WHERE id IN (9036, 9037)");

        PaginationResult<Course> firstPage = favoriteService.paginate(USER_ID, 1, 20);
        PaginationResult<Course> secondPage = favoriteService.paginate(USER_ID, 2, 20);

        assertEquals(25L, firstPage.getTotal());
        assertEquals(25L, secondPage.getTotal());
        assertEquals(20, firstPage.getData().size());
        assertEquals(5, secondPage.getData().size());
        Set<Integer> allIds =
                java.util.stream.Stream.concat(
                                firstPage.getData().stream(), secondPage.getData().stream())
                        .map(Course::getId)
                        .collect(Collectors.toSet());
        assertEquals(25, allIds.size());
        assertTrue(allIds.stream().allMatch(id -> id >= 9001 && id <= 9025));
        assertEquals(37L, favoriteCount());
    }

    @Test
    void retainsFavoriteAcrossPermissionRevocationAndRestoration() {
        insertCourse(9001, "权限变化课程", 1);
        insertFavorite(9001, 1);
        assign(9001, 1);
        when(userService.getDepIdsByUserId(USER_ID)).thenReturn(List.of(10));
        when(departmentService.chunk(List.of(10)))
                .thenReturn(List.of(department(10, "1")));

        assertEquals(1L, favoriteService.paginate(USER_ID, 1, 20).getTotal());

        jdbcTemplate.update(
                "DELETE FROM course_department_user WHERE course_id = ? AND range_id = ?",
                9001,
                1);
        assign(9001, 99);
        PaginationResult<Course> revoked = favoriteService.paginate(USER_ID, 1, 20);
        assertEquals(0L, revoked.getTotal());
        assertTrue(revoked.getData().isEmpty());
        assertEquals(1L, favoriteCount());

        assign(9001, 1);
        PaginationResult<Course> restored = favoriteService.paginate(USER_ID, 1, 20);
        assertEquals(1L, restored.getTotal());
        assertEquals(9001, restored.getData().get(0).getId());
    }

    @Test
    void favoriteAccessQueryUsesAvailableIndexes() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", USER_ID);
        params.put("accessDepIds", List.of(1, 2, 10));
        params.put("pageStart", 0);
        params.put("pageSize", 20);
        MappedStatement statement =
                sqlSessionFactory
                        .getConfiguration()
                        .getMappedStatement(
                                "xyz.playedu.course.mapper.UserCourseFavoriteMapper.paginate");
        BoundSql boundSql = statement.getBoundSql(params);
        List<Object> arguments = new ArrayList<>();
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String property = mapping.getProperty();
            arguments.add(
                    boundSql.hasAdditionalParameter(property)
                            ? boundSql.getAdditionalParameter(property)
                            : params.get(property));
        }

        List<Map<String, Object>> plan =
                jdbcTemplate.queryForList("EXPLAIN " + boundSql.getSql(), arguments.toArray());

        assertUsesIndex(plan, "f");
        assertUsesIndexOrSmallTable(plan, "c");
        assertUsesIndex(plan, "open_rel");
        assertUsesIndex(plan, "access_rel");
    }

    @Test
    void concurrentFavoriteRequestsCreateOnlyOneRow() throws Exception {
        Course course = new Course();
        course.setId(9001);
        course.setIsShow(1);
        when(courseService.findOrFail(9001)).thenReturn(course);
        when(userBus.canSeeCourse(USER_ID, 9001)).thenReturn(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> favoriteAfterStart(ready, start));
            Future<?> second = executor.submit(() -> favoriteAfterStart(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(1L, favoriteCount());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void favoriteAfterStart(CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            favoriteService.favorite(USER_ID, 9001);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void assertUsesIndex(List<Map<String, Object>> plan, String table) {
        Map<String, Object> row =
                plan.stream()
                        .filter(item -> table.equals(item.get("table")))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("EXPLAIN 缺少表 " + table));
        assertTrue(row.get("key") != null, () -> table + " 未使用索引：" + row);
    }

    private void assertUsesIndexOrSmallTable(List<Map<String, Object>> plan, String table) {
        Map<String, Object> row =
                plan.stream()
                        .filter(item -> table.equals(item.get("table")))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("EXPLAIN 缺少表 " + table));
        Number estimatedRows = (Number) row.get("rows");
        assertTrue(
                row.get("key") != null
                        || (estimatedRows != null && estimatedRows.longValue() <= 10),
                () -> table + " 未使用索引且不是小表：" + row);
    }

    private void withoutDepartment() {
        when(userService.getDepIdsByUserId(USER_ID)).thenReturn(List.of());
    }

    private Department department(Integer id, String parentChain) {
        Department department = new Department();
        department.setId(id);
        department.setParentChain(parentChain);
        return department;
    }

    private void insertCourse(Integer id, String title, Integer isShow) {
        jdbcTemplate.update(
                """
                INSERT INTO courses
                    (id, title, thumb, charge, short_desc, is_required, class_hour,
                     is_show, created_at, sort_at, extra, admin_id)
                VALUES (?, ?, 0, 0, '', 0, 1, ?, NOW(), NOW(), '', 0)
                """,
                id,
                title,
                isShow);
    }

    private void insertFavorite(Integer courseId, Integer sequence) {
        jdbcTemplate.update(
                "INSERT INTO user_course_favorites (user_id, course_id, created_at) VALUES (?, ?, ?)",
                USER_ID,
                courseId,
                new Timestamp(1700000000000L + sequence * 1000L));
    }

    private void assign(Integer courseId, Integer departmentId) {
        jdbcTemplate.update(
                "INSERT INTO course_department_user (course_id, range_id, type) VALUES (?, ?, 0)",
                courseId,
                departmentId);
    }

    private Long favoriteCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_course_favorites WHERE user_id = ?",
                Long.class,
                USER_ID);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {"xyz.playedu.course.mapper"})
    @Import(UserCourseFavoriteServiceImpl.class)
    static class TestApplication {}
}
