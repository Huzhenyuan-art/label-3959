package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户 Service 实现
 * 演示 MyBatis Plus 多种用法
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public IPage<User> pageUsers(int current, int size, String username, Integer status) {
        // 演示：分页 + LambdaQueryWrapper 条件构造
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreatedTime);

        log.info("分页查询用户: current={}, size={}, username={}, status={}", current, size, username, status);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<User> listUsers(String username, Integer status) {
        // 演示：LambdaQueryWrapper 链式条件
        return lambdaQuery()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .orderByAsc(User::getId)
                .list();
    }

    @Override
    public User createUser(User user) {
        // 演示：自动填充 createdTime / updatedTime
        user.setDeleted(0);
        user.setVersion(1);
        save(user);
        log.info("创建用户成功: id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        // 演示：乐观锁 - updateById 会自动检查并递增 version
        boolean success = updateById(user);
        if (!success) {
            throw new IllegalArgumentException("更新失败，数据已被他人修改，请刷新后重试（乐观锁冲突）");
        }
        log.info("更新用户成功: id={}", user.getId());
        return getById(user.getId());
    }

    @Override
    public void deleteUser(Long id) {
        // 演示：逻辑删除 - removeById 会将 deleted 字段设为 1
        removeById(id);
        log.info("逻辑删除用户: id={}", id);
    }

    @Override
    public List<User> batchCreate(List<User> users) {
        // 演示：批量插入
        users.forEach(u -> {
            u.setDeleted(0);
            u.setVersion(1);
        });
        saveBatch(users);
        log.info("批量创建用户: count={}", users.size());
        return users;
    }
}
