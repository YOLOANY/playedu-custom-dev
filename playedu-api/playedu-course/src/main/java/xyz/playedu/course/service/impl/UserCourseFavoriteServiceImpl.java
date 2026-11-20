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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import xyz.playedu.course.service.UserCourseFavoriteService;

@Service
public class UserCourseFavoriteServiceImpl
        extends ServiceImpl<UserCourseFavoriteMapper, UserCourseFavorite>
        implements UserCourseFavoriteService {
    private static final String FAVORITE_UNIQUE_KEY = "uk_user_course_favorite";

    @Autowired private CourseService courseService;

    @Autowired private UserBus userBus;

    @Autowired private UserService userService;

    @Autowired private DepartmentService departmentService;

    @Override
    public void favorite(Integer userId, Integer courseId) throws NotFoundException {
        Course course = courseService.findOrFail(courseId);
        if (!Integer.valueOf(1).equals(course.getIsShow())) {
            throw new ServiceException("课程已下架");
        }
        if (!userBus.canSeeCourse(userId, courseId)) {
            throw new ServiceException("无权收藏该课程");
        }

        UserCourseFavorite favorite = new UserCourseFavorite();
        favorite.setUserId(userId);
        favorite.setCourseId(courseId);
        favorite.setCreatedAt(new Date());
        try {
            if (!save(favorite)) {
                throw new ServiceException("收藏保存失败");
            }
        } catch (DuplicateKeyException e) {
            if (!isFavoriteDuplicate(e)) {
                throw e;
            }
        }
    }

    @Override
    public void unfavorite(Integer userId, Integer courseId) {
        remove(query().getWrapper().eq("user_id", userId).eq("course_id", courseId));
    }

    @Override
    public boolean isFavorite(Integer userId, Integer courseId) {
        return count(query().getWrapper().eq("user_id", userId).eq("course_id", courseId)) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResult<Course> paginate(Integer userId, int page, int size) {
        int safePage = Math.min(Math.max(page, 1), 100000);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Integer> accessDepIds = getAccessDepIds(userId);

        PaginationResult<Course> result = new PaginationResult<>();
        result.setData(
                baseMapper.paginate(
                        userId,
                        accessDepIds,
                        (safePage - 1) * safeSize,
                        safeSize));
        result.setTotal(baseMapper.paginateCount(userId, accessDepIds));
        return result;
    }

    private List<Integer> getAccessDepIds(Integer userId) {
        Set<Integer> accessDepIds = new LinkedHashSet<>();
        List<Integer> userDepIds = userService.getDepIdsByUserId(userId);
        if (userDepIds == null || userDepIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Department> departments = departmentService.chunk(userDepIds);
        if (departments == null) {
            return new ArrayList<>();
        }
        for (Department department : departments) {
            accessDepIds.add(department.getId());
            if (department.getParentChain() != null && !department.getParentChain().isEmpty()) {
                accessDepIds.addAll(
                        Arrays.stream(department.getParentChain().split(","))
                                .map(Integer::valueOf)
                                .toList());
            }
        }
        return new ArrayList<>(accessDepIds);
    }

    private boolean isFavoriteDuplicate(DuplicateKeyException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(FAVORITE_UNIQUE_KEY)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
