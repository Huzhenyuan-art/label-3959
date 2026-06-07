package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口
 * 演示：XML 多表联查、事务批量插入、乐观锁状态更新
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 分页查询订单（联查用户信息） */
    @GetMapping("/page")
    public Result<IPage<OrderDetailDTO>> page(@RequestParam(defaultValue = "1") int current,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String createdTimeStart,
                                              @RequestParam(required = false) String createdTimeEnd) {
        return Result.ok(orderService.pageOrders(current, size, username, status, createdTimeStart, createdTimeEnd));
    }

    /** 订单详情（联查用户、明细、商品） */
    @GetMapping("/{id}")
    public Result<OrderDetailDTO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id));
    }

    /** 创建订单（事务 + 批量插入明细） */
    @PostMapping
    public Result<Order> create(@RequestBody CreateOrderRequest req) {
        return Result.ok(orderService.createOrder(req.getOrder(), req.getItems(), req.getUserCouponId(), req.getAddressId()));
    }

    /** 更新订单状态（演示乐观锁） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody UpdateStatusRequest req) {
        orderService.updateOrderStatus(id, req.getStatus(), req.getVersion());
        return Result.ok();
    }

    /** 处理订单退款 */
    @PutMapping("/{id}/refund")
    public Result<Void> processRefund(@PathVariable Long id,
                                      @RequestBody ProcessRefundRequest req) {
        orderService.processRefund(id, req.isSuccess(), req.getReason());
        return Result.ok();
    }

    /** 更新订单备注（独立编辑，仅更新备注字段，走乐观锁） */
    @PutMapping("/{id}/remark")
    public Result<Void> updateRemark(@PathVariable Long id,
                                     @RequestBody UpdateRemarkRequest req) {
        orderService.updateRemark(id, req.getRemark(), req.getVersion());
        return Result.ok();
    }

    @Data
    public static class CreateOrderRequest {
        private Order order;
        private List<OrderItem> items;
        private Long userCouponId;
        private Long addressId;
    }

    @Data
    public static class UpdateStatusRequest {
        private Integer status;
        private Integer version;
    }

    @Data
    public static class ProcessRefundRequest {
        private boolean success;
        private String reason;
    }

    @Data
    public static class UpdateRemarkRequest {
        private String remark;
        private Integer version;
    }
}
