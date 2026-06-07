package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import org.apache.ibatis.annotations.Param;

/**
 * 订单 Mapper
 * 演示：XML 多表联查 + 分页
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单（联查用户信息）
     */
    IPage<OrderDetailDTO> selectOrderPage(Page<OrderDetailDTO> page,
                                          @Param("username") String username,
                                          @Param("status") Integer status,
                                          @Param("userId") Long userId,
                                          @Param("createdTimeStart") String createdTimeStart,
                                          @Param("createdTimeEnd") String createdTimeEnd);

    /**
     * 查询订单详情（联查用户、订单明细、商品信息）
     */
    OrderDetailDTO selectOrderDetail(@Param("id") Long id);
}
