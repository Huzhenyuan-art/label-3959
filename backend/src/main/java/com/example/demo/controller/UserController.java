package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口
 * 演示：逻辑删除、乐观锁、自动填充、条件查询、分页、批量操作
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<IPage<User>> page(@RequestParam(defaultValue = "1") int current,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String username,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer minAge,
                                    @RequestParam(required = false) Integer maxAge) {
        return Result.ok(userService.pageUsers(current, size, username, status, minAge, maxAge));
    }

    /** 条件列表查询 */
    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer minAge,
                                   @RequestParam(required = false) Integer maxAge) {
        return Result.ok(userService.listUsers(username, status, minAge, maxAge));
    }

    /** 根据 ID 查询 */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) return Result.fail(404, "用户不存在");
        return Result.ok(user);
    }

    /** 创建用户（演示自动填充） */
    @PostMapping
    public Result<User> create(@RequestBody User user) {
        return Result.ok(userService.createUser(user));
    }

    /** 更新用户（演示乐观锁） */
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.ok(userService.updateUser(user));
    }

    /** 逻辑删除用户（演示 @TableLogic） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    /** 批量创建（演示 saveBatch） */
    @PostMapping("/batch")
    public Result<List<User>> batchCreate(@RequestBody List<User> users) {
        return Result.ok(userService.batchCreate(users));
    }
}
