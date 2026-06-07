package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.OperationLogQueryDTO;
import com.example.demo.entity.OperationLog;
import com.example.demo.enums.OperationTypeEnum;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Override
    @Async("auditLogExecutor")
    public void saveLogAsync(OperationLog operationLog) {
        try {
            save(operationLog);
            logger.debug("审计日志保存成功: operationType={}, operator={}", operationLog.getOperationType(), operationLog.getOperatorName());
        } catch (Exception e) {
            logger.error("审计日志保存失败", e);
        }
    }

    @Override
    public IPage<OperationLog> pageLogs(int current, int size, OperationLogQueryDTO query) {
        return baseMapper.selectPageByConditions(
                new Page<>(current, size),
                query.getOperationType(),
                query.getOperationCategory(),
                query.getOperatorName(),
                query.getTargetId(),
                query.getTargetType(),
                query.getStartTime(),
                query.getEndTime(),
                query.getStatus()
        );
    }

    @Override
    public OperationLog getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Map<String, String>> getOperationTypes() {
        return Arrays.stream(OperationTypeEnum.values())
                .map(enumItem -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("code", enumItem.getCode());
                    map.put("desc", enumItem.getDesc());
                    map.put("category", enumItem.getCategory());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getOperationCategories() {
        return Arrays.stream(OperationTypeEnum.values())
                .map(OperationTypeEnum::getCategory)
                .distinct()
                .map(category -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("category", category);
                    return map;
                })
                .collect(Collectors.toList());
    }
}
