package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    /** 分页查询（可按用户名/状态/年龄区间过滤） */
    IPage<User> pageUsers(int current, int size, String username, Integer status, Integer minAge, Integer maxAge);

    /** 条件查询列表 */
    List<User> listUsers(String username, Integer status, Integer minAge, Integer maxAge);

    /** 创建用户 */
    User createUser(User user);

    /** 更新用户（乐观锁保护） */
    User updateUser(User user);

    /** 逻辑删除用户 */
    void deleteUser(Long id);

    /** 批量创建示例 */
    List<User> batchCreate(List<User> users);
}
