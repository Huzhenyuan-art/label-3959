package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.User;
import com.example.demo.enums.RoleEnum;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<User> pageUsers(int current, int size, String username, Integer status, Integer minAge, Integer maxAge, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .ge(minAge != null, User::getAge, minAge)
                .le(maxAge != null, User::getAge, maxAge)
                .eq(StringUtils.hasText(role), User::getRole, role)
                .orderByDesc(User::getCreatedTime);

        log.info("分页查询用户: current={}, size={}, username={}, status={}, minAge={}, maxAge={}, role={}", current, size, username, status, minAge, maxAge, role);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<User> listUsers(String username, Integer status, Integer minAge, Integer maxAge, String role) {
        return lambdaQuery()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .ge(minAge != null, User::getAge, minAge)
                .le(maxAge != null, User::getAge, maxAge)
                .eq(StringUtils.hasText(role), User::getRole, role)
                .orderByAsc(User::getId)
                .list();
    }

    @Override
    public User createUser(User user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole(RoleEnum.USER.getCode());
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setDeleted(0);
        user.setVersion(1);
        save(user);
        log.info("创建用户成功: id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return user;
    }

    @Override
    public User updateUser(User user) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(user.getId())) {
            User existingUser = getById(user.getId());
            if (existingUser != null && RoleEnum.ADMIN.getCode().equals(existingUser.getRole())
                    && RoleEnum.USER.getCode().equals(user.getRole())) {
                throw new IllegalArgumentException("不能将自己降为普通用户");
            }
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        boolean success = updateById(user);
        if (!success) {
            throw new IllegalArgumentException("更新失败，数据已被他人修改，请刷新后重试（乐观锁冲突）");
        }
        log.info("更新用户成功: id={}", user.getId());
        return getById(user.getId());
    }

    @Override
    public void deleteUser(Long id) {
        removeById(id);
        log.info("逻辑删除用户: id={}", id);
    }

    @Override
    public List<User> batchCreate(List<User> users) {
        users.forEach(u -> {
            if (StringUtils.hasText(u.getPassword())) {
                u.setPassword(passwordEncoder.encode(u.getPassword()));
            }
            if (!StringUtils.hasText(u.getRole())) {
                u.setRole(RoleEnum.USER.getCode());
            }
            if (u.getStatus() == null) {
                u.setStatus(1);
            }
            u.setDeleted(0);
            u.setVersion(1);
        });
        saveBatch(users);
        log.info("批量创建用户: count={}", users.size());
        return users;
    }
}
