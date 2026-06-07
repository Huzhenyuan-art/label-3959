package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.RefundApplyDTO;
import com.example.demo.dto.RefundAuditDTO;
import com.example.demo.dto.RefundDetailDTO;
import com.example.demo.entity.RefundOrder;
import com.example.demo.service.RefundOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@Validated
public class RefundOrderController {

    private final RefundOrderService refundOrderService;

    @GetMapping("/page")
    public Result<IPage<RefundDetailDTO>> page(@RequestParam(defaultValue = "1") int current,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String refundNo,
                                               @RequestParam(required = false) Long orderId,
                                               @RequestParam(required = false) String username,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(refundOrderService.pageRefunds(current, size, refundNo, orderId, username, status));
    }

    @GetMapping("/{id}")
    public Result<RefundDetailDTO> detail(@PathVariable Long id) {
        return Result.ok(refundOrderService.getRefundDetail(id));
    }

    @PostMapping("/apply")
    public Result<RefundOrder> apply(@Valid @RequestBody RefundApplyDTO dto) {
        return Result.ok(refundOrderService.applyRefund(dto));
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody RefundAuditDTO dto) {
        refundOrderService.auditRefund(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        refundOrderService.cancelRefund(id);
        return Result.ok();
    }

    @Data
    public static class RefundApplyRequest {
        private Long orderId;
        private java.math.BigDecimal refundAmount;
        private Integer refundType;
        private String refundReason;
        private String refundDesc;
        private String proofImages;
    }

    @Data
    public static class RefundAuditRequest {
        private Long refundId;
        private Boolean approved;
        private String auditRemark;
    }
}
