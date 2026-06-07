package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.OperationLogQueryDTO;
import com.example.demo.entity.OperationLog;

import java.util.List;
import java.util.Map;

public interface OperationLogService {

    void saveLogAsync(OperationLog log);

    IPage<OperationLog> pageLogs(int current, int size, OperationLogQueryDTO query);

    OperationLog getById(Long id);

    List<Map<String, String>> getOperationTypes();

    List<Map<String, String>> getOperationCategories();
}
