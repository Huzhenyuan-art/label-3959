package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.OperationLogQueryDTO;
import com.example.demo.entity.OperationLog;
import com.example.demo.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<IPage<OperationLog>> page(@RequestParam(defaultValue = "1") int current,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String operationType,
                                            @RequestParam(required = false) String operationCategory,
                                            @RequestParam(required = false) String operatorName,
                                            @RequestParam(required = false) Long targetId,
                                            @RequestParam(required = false) String targetType,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                            @RequestParam(required = false) Integer status) {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setOperationType(operationType);
        query.setOperationCategory(operationCategory);
        query.setOperatorName(operatorName);
        query.setTargetId(targetId);
        query.setTargetType(targetType);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setStatus(status);
        return Result.ok(operationLogService.pageLogs(current, size, query));
    }

    @GetMapping("/{id}")
    public Result<OperationLog> getById(@PathVariable Long id) {
        OperationLog log = operationLogService.getById(id);
        if (log == null) {
            return Result.fail(404, "日志不存在");
        }
        return Result.ok(log);
    }

    @GetMapping("/operation-types")
    public Result<List<Map<String, String>>> getOperationTypes() {
        return Result.ok(operationLogService.getOperationTypes());
    }

    @GetMapping("/operation-categories")
    public Result<List<Map<String, String>>> getOperationCategories() {
        return Result.ok(operationLogService.getOperationCategories());
    }
}
