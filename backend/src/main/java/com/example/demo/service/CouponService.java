package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.CouponCreateDTO;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.dto.UserCouponDTO;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService extends IService<CouponTemplate> {

    CouponTemplate createTemplate(CouponCreateDTO dto);

    IPage<CouponTemplate> pageTemplates(int current, int size, String name, Integer type, Integer status);

    CouponTemplate updateTemplateStatus(Long id, Integer status);

    UserCoupon receiveCoupon(Long templateId);

    List<UserCouponDTO> getUserCoupons(Integer status);

    UserCouponDTO getUserCouponDetail(Long id);

    CouponUseResultDTO calculateDiscount(Long userCouponId, BigDecimal orderAmount);

    UserCoupon useCoupon(Long userCouponId, Long orderId, BigDecimal orderAmount);

    void restoreCoupon(Long orderId);

    List<CouponTemplate> getAvailableTemplates();

    List<UserCouponDTO> getAvailableCouponsForOrder(BigDecimal orderAmount);
}
