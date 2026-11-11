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
package xyz.playedu.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.playedu.common.constant.SystemConstant;
import xyz.playedu.common.domain.User;
import xyz.playedu.common.domain.UserDepartment;
import xyz.playedu.common.exception.NotFoundException;
import xyz.playedu.common.exception.ServiceException;
import xyz.playedu.common.mapper.UserMapper;
import xyz.playedu.common.service.UserDepartmentService;
import xyz.playedu.common.service.UserService;
import xyz.playedu.common.types.paginate.PaginationResult;
import xyz.playedu.common.types.paginate.UserPaginateFilter;
import xyz.playedu.common.util.HelperUtil;
import xyz.playedu.common.util.IdCardCrypto;
import xyz.playedu.common.util.IdCardValidator;

/**
 * @author tengteng
 * @description 针对表【users】的数据库操作Service实现
 * @createDate 2023-02-23 13:50:58
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired private UserDepartmentService userDepartmentService;
    @Autowired private IdCardCrypto idCardCrypto;

    private void validateIdCard(String idCard) {
        if (idCard != null && !idCard.isBlank() && !IdCardValidator.isValid(idCard)) {
            throw new ServiceException("请输入正确的身份证号");
        }
    }

    @Override
    public boolean emailIsExists(String email) {
        User user = getOne(query().getWrapper().eq("email", email));
        return user != null;
    }

    @Override
    public boolean mobileIsExists(String mobile, Integer excludeUserId) {
        return count(
                        query().getWrapper()
                                .eq("mobile", mobile)
                                .ne(excludeUserId != null, "id", excludeUserId))
                > 0;
    }

    @Override
    public PaginationResult<User> paginate(int page, int size, UserPaginateFilter filter) {
        if (filter.getIdCard() != null && !filter.getIdCard().isBlank()) {
            filter.setIdCardBidx(idCardCrypto.blindIndex(filter.getIdCard()));
        }
        filter.setPageStart((page - 1) * size);
        filter.setPageSize(size);

        PaginationResult<User> pageResult = new PaginationResult<>();
        pageResult.setData(getBaseMapper().paginate(filter));
        pageResult.setTotal(getBaseMapper().paginateCount(filter));

        return pageResult;
    }

    @Override
    public List<String> existsEmailsByEmails(List<String> emails) {
        return list(query().getWrapper().in("email", emails).select("id", "email")).stream()
                .map(User::getEmail)
                .toList();
    }

    @Override
    public void removeRelateDepartmentsByUserId(Integer userId) {
        userDepartmentService.remove(
                userDepartmentService.query().getWrapper().eq("user_id", userId));
    }

    @Override
    public User findOrFail(Integer id) throws NotFoundException {
        User user = getOne(query().getWrapper().eq("id", id));
        if (user == null) {
            throw new NotFoundException("学员不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public User createWithDepIds(
            String email,
            String name,
            Integer avatar,
            String password,
            String idCard,
            Integer[] depIds) {
        return createWithDepIds(email, name, null, avatar, password, idCard, depIds);
    }

    @Override
    @Transactional
    public User createWithDepIds(
            String email,
            String name,
            String mobile,
            Integer avatar,
            String password,
            String idCard,
            Integer[] depIds) {
        validateIdCard(idCard);
        String salt = HelperUtil.randomString(6);
        String passwordHashed = HelperUtil.MD5(password + salt);

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setMobile(mobile == null || mobile.isBlank() ? null : mobile);
        user.setAvatar(avatar);
        user.setPassword(passwordHashed);
        user.setSalt(salt);
        user.setIdCard(idCardCrypto.encrypt(idCard));
        user.setIdCardBidx(idCardCrypto.blindIndex(idCard));
        user.setCredit1(0);
        user.setIsSetPassword(0);
        user.setIsActive(1);
        user.setIsLock(0);
        user.setCreateIp(SystemConstant.INTERNAL_IP);
        user.setCreateCity(SystemConstant.INTERNAL_IP_AREA);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        if (idCard != null && !idCard.isBlank()) {
            user.setVerifyAt(new Date());
            user.setIsVerify(1);
        }

        save(user);
        userDepartmentService.storeDepIds(user.getId(), depIds);
        return user;
    }

    @Override
    @Transactional
    public User updateWithDepIds(
            User user,
            String email,
            String name,
            Integer avatar,
            String password,
            String idCard,
            Integer[] depIds) {
        return updateWithDepIds(
                user, email, name, user.getMobile(), avatar, password, idCard, depIds);
    }

    @Override
    @Transactional
    public User updateWithDepIds(
            User user,
            String email,
            String name,
            String mobile,
            Integer avatar,
            String password,
            String idCard,
            Integer[] depIds) {
        validateIdCard(idCard);
        User newUser = new User();
        newUser.setId(user.getId());
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setMobile(mobile == null || mobile.isBlank() ? null : mobile);
        newUser.setAvatar(avatar);
        if (idCard != null) {
            newUser.setIdCard(idCardCrypto.encrypt(idCard));
            newUser.setIdCardBidx(idCardCrypto.blindIndex(idCard));
        }

        if (password != null && !password.isEmpty()) {
            newUser.setPassword(HelperUtil.MD5(password + user.getSalt()));
        }

        boolean hasIdCard =
                idCard == null
                        ? user.getIdCard() != null && !user.getIdCard().isEmpty()
                        : !idCard.isBlank();
        boolean clearVerifyAt = false;
        if (newUser.getName() != null && !newUser.getName().isEmpty() && hasIdCard) {
            if (user.getVerifyAt() == null) {
                newUser.setIsVerify(1);
                newUser.setVerifyAt(new Date());
            }
        } else if (Integer.valueOf(1).equals(user.getIsVerify())) {
            newUser.setIsVerify(0);
            newUser.setVerifyAt(null);
            clearVerifyAt = user.getVerifyAt() != null;
        }

        updateById(newUser);
        if (idCard != null && newUser.getIdCard().isEmpty()) {
            lambdaUpdate().eq(User::getId, user.getId()).set(User::getIdCardBidx, null).update();
        }
        if (newUser.getMobile() == null) {
            lambdaUpdate().eq(User::getId, user.getId()).set(User::getMobile, null).update();
        }
        if (clearVerifyAt) {
            lambdaUpdate().eq(User::getId, user.getId()).set(User::getVerifyAt, null).update();
        }
        userDepartmentService.resetStoreDepIds(newUser.getId(), depIds);
        return newUser;
    }

    @Override
    public List<Integer> getDepIdsByUserId(Integer userId) {
        return userDepartmentService
                .list(userDepartmentService.query().getWrapper().eq("user_id", userId))
                .stream()
                .map(UserDepartment::getDepId)
                .toList();
    }

    @Override
    public User find(Integer id) {
        return getOne(query().getWrapper().eq("id", id));
    }

    @Override
    public User find(String email) {
        return getOne(query().getWrapper().eq("email", email));
    }

    @Override
    public void passwordChange(User user, String oldPassword, String newPassword)
            throws ServiceException {
        if (!HelperUtil.MD5(oldPassword + user.getSalt()).equals(user.getPassword())) {
            throw new ServiceException("原密码不正确");
        }
        updateById(
                new User() {
                    {
                        setId(user.getId());
                        setPassword(HelperUtil.MD5(newPassword + user.getSalt()));
                    }
                });
    }

    @Override
    public List<User> chunks(List<Integer> ids, List<String> fields) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return list(query().getWrapper().in("id", ids).select(fields));
    }

    @Override
    public List<User> chunks(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return list(query().getWrapper().in("id", ids));
    }

    @Override
    public Long total() {
        return count();
    }

    @Override
    @SneakyThrows
    public Long todayCount() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String todayDate = simpleDateFormat.format(now);
        return count(
                query().getWrapper().between("created_at", simpleDateFormat.parse(todayDate), now));
    }

    @Override
    @SneakyThrows
    public Long yesterdayCount() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todayDate = simpleDateFormat.format(new Date());
        String yesterdayDate =
                simpleDateFormat.format(new Date(System.currentTimeMillis() - 86400000));
        return count(
                query().getWrapper()
                        .between(
                                "created_at",
                                simpleDateFormat.parse(yesterdayDate),
                                simpleDateFormat.parse(todayDate)));
    }

    @Override
    public Map<Integer, List<Integer>> getDepIdsGroup(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        Map<Integer, List<UserDepartment>> data =
                userDepartmentService
                        .list(userDepartmentService.query().getWrapper().in("user_id", userIds))
                        .stream()
                        .collect(Collectors.groupingBy(UserDepartment::getUserId));
        Map<Integer, List<Integer>> result = new HashMap<>();
        data.forEach(
                (userId, records) -> {
                    result.put(userId, records.stream().map(UserDepartment::getDepId).toList());
                });
        return result;
    }

    @Override
    public void changeAvatar(Integer userId, Integer avatar) {
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatar);
        updateById(user);
    }

    @Override
    public void updateName(Integer id, String cn) {
        User user = new User();
        user.setId(id);
        user.setName(cn);
        updateById(user);
    }

    @Override
    public void updateDepId(Integer id, Integer[] depIds) {
        userDepartmentService.resetStoreDepIds(id, depIds);
    }

    @Override
    public void updateEmail(Integer id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        updateById(user);
    }
}
