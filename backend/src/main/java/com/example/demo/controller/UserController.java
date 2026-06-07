package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.OperationLog;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.enums.OperationTypeEnum;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
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
                                    @RequestParam(required = false) Integer maxAge,
                                    @RequestParam(required = false) String role) {
        return Result.ok(userService.pageUsers(current, size, username, status, minAge, maxAge, role));
    }

    /** 条件列表查询 */
    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer minAge,
                                   @RequestParam(required = false) Integer maxAge,
                                   @RequestParam(required = false) String role) {
        return Result.ok(userService.listUsers(username, status, minAge, maxAge, role));
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
    @OperationLog(type = OperationTypeEnum.USER_CREATE, targetType = "user", targetIdExpression = "#result.data.id")
    public Result<User> create(@RequestBody User user) {
        return Result.ok(userService.createUser(user));
    }

    /** 更新用户（演示乐观锁） */
    @PutMapping("/{id}")
    @OperationLog(type = OperationTypeEnum.USER_UPDATE, targetType = "user", targetIdExpression = "#id")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.ok(userService.updateUser(user));
    }

    /** 逻辑删除用户（演示 @TableLogic） */
    @DeleteMapping("/{id}")
    @OperationLog(type = OperationTypeEnum.USER_DELETE, targetType = "user", targetIdExpression = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    /** 批量创建（演示 saveBatch） */
    @PostMapping("/batch")
    public Result<List<User>> batchCreate(@RequestBody List<User> users) {
        return Result.ok(userService.batchCreate(users));
    }

    /** 分页查询已逻辑删除的用户 */
    @GetMapping("/deleted/page")
    public Result<IPage<User>> pageDeleted(@RequestParam(defaultValue = "1") int current,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Integer minAge,
                                           @RequestParam(required = false) Integer maxAge,
                                           @RequestParam(required = false) String role) {
        return Result.ok(userService.pageDeletedUsers(current, size, username, status, minAge, maxAge, role));
    }

    /** 恢复已逻辑删除的用户（演示乐观锁） */
    @PutMapping("/{id}/restore")
    @OperationLog(type = OperationTypeEnum.USER_RESTORE, targetType = "user", targetIdExpression = "#id")
    public Result<User> restore(@PathVariable Long id, @RequestBody User user) {
        return Result.ok(userService.restoreUser(id, user.getVersion()));
    }

    /** 导出 CSV */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Integer minAge,
                       @RequestParam(required = false) Integer maxAge,
                       @RequestParam(required = false) String role,
                       @RequestParam(defaultValue = "false") boolean deleted,
                       HttpServletResponse response) throws IOException {
        List<User> users = deleted
                ? userService.listDeletedUsers(username, status, minAge, maxAge, role)
                : userService.listUsers(username, status, minAge, maxAge, role);

        if (users.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"当前筛选结果为空，没有可导出的数据\",\"data\":null}");
            return;
        }

        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String fileName = (deleted ? "已删除用户_" : "用户列表_") + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');

            writer.write("ID,用户名,邮箱,角色,年龄,状态,版本,创建时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (User user : users) {
                String roleText = "ADMIN".equals(user.getRole()) ? "管理员" : "普通用户";
                String statusText = user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用";
                String createdTime = user.getCreatedTime() != null ? user.getCreatedTime().format(formatter) : "";

                writer.write(String.format("%d,%s,%s,%s,%d,%s,%d,%s\n",
                        user.getId(),
                        escapeCsv(user.getUsername()),
                        escapeCsv(user.getEmail()),
                        roleText,
                        user.getAge() != null ? user.getAge() : 0,
                        statusText,
                        user.getVersion() != null ? user.getVersion() : 1,
                        createdTime
                ));
            }
            writer.flush();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
