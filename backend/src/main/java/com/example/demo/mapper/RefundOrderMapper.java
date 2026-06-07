package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.RefundDetailDTO;
import com.example.demo.entity.RefundOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {

    IPage<RefundDetailDTO> selectRefundPage(
            Page<RefundDetailDTO> page,
            @Param("refundNo") String refundNo,
            @Param("orderId") Long orderId,
            @Param("username") String username,
            @Param("status") Integer status,
            @Param("userId") Long userId
    );

    RefundDetailDTO selectRefundDetail(@Param("id") Long id);

    RefundOrder selectPendingByOrderId(@Param("orderId") Long orderId);
}
