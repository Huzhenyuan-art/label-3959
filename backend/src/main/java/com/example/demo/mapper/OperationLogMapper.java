package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.OperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    IPage<OperationLog> selectPageByConditions(Page<OperationLog> page,
                                               @Param("operationType") String operationType,
                                               @Param("operationCategory") String operationCategory,
                                               @Param("operatorName") String operatorName,
                                               @Param("targetId") Long targetId,
                                               @Param("targetType") String targetType,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("status") Integer status);
}
