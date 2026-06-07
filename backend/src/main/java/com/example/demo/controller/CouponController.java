package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.CouponCreateDTO;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.dto.UserCouponDTO;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.entity.UserCoupon;
import com.example.demo.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/templates")
    public Result<CouponTemplate> createTemplate(@RequestBody CouponCreateDTO dto) {
        return Result.ok(couponService.createTemplate(dto));
    }

    @GetMapping("/templates/page")
    public Result<IPage<CouponTemplate>> pageTemplates(@RequestParam(defaultValue = "1") int current,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(required = false) Integer type,
                                                       @RequestParam(required = false) Integer status) {
        return Result.ok(couponService.pageTemplates(current, size, name, type, status));
    }

    @GetMapping("/templates/available")
    public Result<List<CouponTemplate>> getAvailableTemplates() {
        return Result.ok(couponService.getAvailableTemplates());
    }

    @PutMapping("/templates/{id}/status")
    public Result<CouponTemplate> updateTemplateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(couponService.updateTemplateStatus(id, status));
    }

    @PostMapping("/templates/{templateId}/receive")
    public Result<UserCoupon> receiveCoupon(@PathVariable Long templateId) {
        return Result.ok(couponService.receiveCoupon(templateId));
    }

    @GetMapping("/my")
    public Result<List<UserCouponDTO>> getMyCoupons(@RequestParam(required = false) Integer status) {
        return Result.ok(couponService.getUserCoupons(status));
    }

    @GetMapping("/my/{id}")
    public Result<UserCouponDTO> getMyCouponDetail(@PathVariable Long id) {
        return Result.ok(couponService.getUserCouponDetail(id));
    }

    @GetMapping("/my/available-for-order")
    public Result<List<UserCouponDTO>> getAvailableCouponsForOrder(@RequestParam BigDecimal orderAmount) {
        return Result.ok(couponService.getAvailableCouponsForOrder(orderAmount));
    }

    @PostMapping("/{userCouponId}/calculate")
    public Result<CouponUseResultDTO> calculateDiscount(@PathVariable Long userCouponId,
                                                        @RequestParam BigDecimal orderAmount) {
        return Result.ok(couponService.calculateDiscount(userCouponId, orderAmount));
    }
}
