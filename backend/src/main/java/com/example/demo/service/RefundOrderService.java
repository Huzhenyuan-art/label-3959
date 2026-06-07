package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.RefundApplyDTO;
import com.example.demo.dto.RefundAuditDTO;
import com.example.demo.dto.RefundDetailDTO;
import com.example.demo.entity.RefundOrder;

public interface RefundOrderService extends IService<RefundOrder> {

    IPage<RefundDetailDTO> pageRefunds(int current, int size, String refundNo, Long orderId, String username, Integer status);

    RefundDetailDTO getRefundDetail(Long id);

    RefundOrder applyRefund(RefundApplyDTO dto);

    void auditRefund(RefundAuditDTO dto);

    void cancelRefund(Long id);
}
