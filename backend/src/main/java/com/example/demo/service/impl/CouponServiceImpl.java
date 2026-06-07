package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CouponCreateDTO;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.dto.UserCouponDTO;
import com.example.demo.entity.CouponTemplate;
import com.example.demo.entity.UserCoupon;
import com.example.demo.enums.CouponTemplateStatusEnum;
import com.example.demo.enums.CouponTypeEnum;
import com.example.demo.enums.UserCouponStatusEnum;
import com.example.demo.mapper.CouponTemplateMapper;
import com.example.demo.mapper.UserCouponMapper;
import com.example.demo.service.CouponService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate createTemplate(CouponCreateDTO dto) {
        validateCouponCreateDTO(dto);

        CouponTemplate template = new CouponTemplate();
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setDiscountAmount(dto.getDiscountAmount());
        template.setDiscountRate(dto.getDiscountRate());
        template.setMinAmount(dto.getMinAmount() != null ? dto.getMinAmount() : BigDecimal.ZERO);
        template.setTotalCount(dto.getTotalCount());
        template.setReceivedCount(0);
        template.setUsedCount(0);
        template.setPerUserLimit(dto.getPerUserLimit() != null ? dto.getPerUserLimit() : 1);
        template.setValidStartTime(dto.getValidStartTime());
        template.setValidEndTime(dto.getValidEndTime());
        template.setValidDays(dto.getValidDays());
        template.setStatus(CouponTemplateStatusEnum.ACTIVE.getCode());
        template.setDescription(dto.getDescription());

        save(template);
        logger.info("创建优惠券模板成功: templateId={}, name={}", template.getId(), template.getName());
        return template;
    }

    private void validateCouponCreateDTO(CouponCreateDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("优惠券名称不能为空");
        }
        if (dto.getName().length() > 100) {
            throw new IllegalArgumentException("优惠券名称不能超过100个字符");
        }

        CouponTypeEnum type = CouponTypeEnum.getByCode(dto.getType());
        if (type == null) {
            throw new IllegalArgumentException("无效的优惠券类型");
        }

        if (type == CouponTypeEnum.FIXED_AMOUNT) {
            if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("满减券必须设置有效的减免金额");
            }
        } else if (type == CouponTypeEnum.DISCOUNT_RATE) {
            if (dto.getDiscountRate() == null) {
                throw new IllegalArgumentException("折扣券必须设置折扣率");
            }
            if (dto.getDiscountRate().compareTo(new BigDecimal("0.01")) < 0 ||
                    dto.getDiscountRate().compareTo(new BigDecimal("0.99")) > 0) {
                throw new IllegalArgumentException("折扣率必须在0.01到0.99之间");
            }
        }

        if (dto.getMinAmount() != null && dto.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("满减门槛不能为负数");
        }

        if (dto.getTotalCount() == null || dto.getTotalCount() <= 0) {
            throw new IllegalArgumentException("发放总量必须大于0");
        }

        if (dto.getPerUserLimit() != null && dto.getPerUserLimit() <= 0) {
            throw new IllegalArgumentException("每人限领次数必须大于0");
        }

        if (dto.getValidDays() == null && (dto.getValidStartTime() == null || dto.getValidEndTime() == null)) {
            throw new IllegalArgumentException("必须设置有效期，要么设置固定天数，要么设置开始和结束时间");
        }

        if (dto.getValidStartTime() != null && dto.getValidEndTime() != null) {
            if (dto.getValidEndTime().isBefore(dto.getValidStartTime())) {
                throw new IllegalArgumentException("有效期结束时间不能早于开始时间");
            }
            if (dto.getValidEndTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("有效期结束时间不能早于当前时间");
            }
        }

        if (dto.getValidDays() != null && dto.getValidDays() <= 0) {
            throw new IllegalArgumentException("有效天数必须大于0");
        }
    }

    @Override
    public IPage<CouponTemplate> pageTemplates(int current, int size, String name, Integer type, Integer status) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(CouponTemplate::getName, name);
        }
        if (type != null) {
            wrapper.eq(CouponTemplate::getType, type);
        }
        if (status != null) {
            wrapper.eq(CouponTemplate::getStatus, status);
        }
        wrapper.orderByDesc(CouponTemplate::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplate updateTemplateStatus(Long id, Integer status) {
        CouponTemplate template = getById(id);
        if (template == null) {
            throw new IllegalArgumentException("优惠券模板不存在: " + id);
        }
        if (CouponTemplateStatusEnum.getByCode(status) == null) {
            throw new IllegalArgumentException("无效的状态值");
        }
        template.setStatus(status);
        updateById(template);
        logger.info("更新优惠券模板状态: templateId={}, status={}", id, status);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon receiveCoupon(Long templateId) {
        Long userId = SecurityUtil.getCurrentUserId();

        CouponTemplate template = getById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("优惠券模板不存在: " + templateId);
        }

        if (!CouponTemplateStatusEnum.ACTIVE.getCode().equals(template.getStatus())) {
            throw new IllegalArgumentException("该优惠券已停止发放");
        }

        LocalDateTime now = LocalDateTime.now();
        if (template.getValidStartTime() != null && now.isBefore(template.getValidStartTime())) {
            throw new IllegalArgumentException("该优惠券尚未开始发放");
        }
        if (template.getValidEndTime() != null && now.isAfter(template.getValidEndTime())) {
            throw new IllegalArgumentException("该优惠券已过期");
        }

        int receivedCount = userCouponMapper.countByUserIdAndTemplateId(userId, templateId);
        if (receivedCount >= template.getPerUserLimit()) {
            throw new IllegalArgumentException("您已达到该优惠券的领取上限，每人限领" + template.getPerUserLimit() + "张");
        }

        if (template.getReceivedCount() >= template.getTotalCount()) {
            throw new IllegalArgumentException("该优惠券已被领完");
        }

        int updated = couponTemplateMapper.incrementReceivedCount(templateId);
        if (updated <= 0) {
            throw new IllegalArgumentException("领取失败，请稍后重试");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setCouponCode(generateCouponCode());
        userCoupon.setStatus(UserCouponStatusEnum.AVAILABLE.getCode());

        if (template.getValidDays() != null) {
            userCoupon.setValidStartTime(now);
            userCoupon.setValidEndTime(now.plusDays(template.getValidDays()));
        } else {
            userCoupon.setValidStartTime(template.getValidStartTime());
            userCoupon.setValidEndTime(template.getValidEndTime());
        }

        userCouponMapper.insert(userCoupon);
        logger.info("用户领取优惠券成功: userId={}, templateId={}, userCouponId={}", userId, templateId, userCoupon.getId());
        return userCoupon;
    }

    private String generateCouponCode() {
        return "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public List<UserCouponDTO> getUserCoupons(Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        userCouponMapper.expireCoupons(LocalDateTime.now());
        return userCouponMapper.selectUserCouponList(userId, status);
    }

    @Override
    public UserCouponDTO getUserCouponDetail(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserCouponDTO dto = userCouponMapper.selectUserCouponDetail(id);
        if (dto == null) {
            throw new IllegalArgumentException("优惠券不存在: " + id);
        }
        if (!userId.equals(dto.getUserId())) {
            throw new SecurityException("无权查看他人优惠券");
        }
        dto.setCouponTypeDesc(CouponTypeEnum.getByCode(dto.getCouponType()).getDesc());
        dto.setStatusDesc(UserCouponStatusEnum.getByCode(dto.getStatus()).getDesc());
        return dto;
    }

    @Override
    public CouponUseResultDTO calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
        Long userId = SecurityUtil.getCurrentUserId();

        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new IllegalArgumentException("优惠券不存在: " + userCouponId);
        }

        if (!userId.equals(userCoupon.getUserId())) {
            throw new SecurityException("无权使用他人优惠券");
        }

        CouponTemplate template = getById(userCoupon.getTemplateId());
        validateCouponAvailable(userCoupon, template, orderAmount);

        BigDecimal discountAmount = calculateDiscountAmount(template, orderAmount);

        CouponUseResultDTO result = new CouponUseResultDTO();
        result.setUserCouponId(userCouponId);
        result.setTemplateId(template.getId());
        result.setCouponName(template.getName());
        result.setCouponType(template.getType());
        result.setDiscountAmount(discountAmount);
        result.setOriginalAmount(orderAmount);
        result.setFinalAmount(orderAmount.subtract(discountAmount).max(BigDecimal.ZERO));

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon useCoupon(Long userCouponId, Long orderId, BigDecimal orderAmount) {
        Long userId = SecurityUtil.getCurrentUserId();

        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new IllegalArgumentException("优惠券不存在: " + userCouponId);
        }

        if (!userId.equals(userCoupon.getUserId())) {
            throw new SecurityException("无权使用他人优惠券");
        }

        CouponTemplate template = getById(userCoupon.getTemplateId());
        validateCouponAvailable(userCoupon, template, orderAmount);

        BigDecimal discountAmount = calculateDiscountAmount(template, orderAmount);

        userCoupon.setStatus(UserCouponStatusEnum.USED.getCode());
        userCoupon.setUsedTime(LocalDateTime.now());
        userCoupon.setOrderId(orderId);
        userCoupon.setDiscountAmount(discountAmount);
        userCouponMapper.updateById(userCoupon);

        couponTemplateMapper.incrementUsedCount(template.getId());

        logger.info("使用优惠券成功: userCouponId={}, orderId={}, discountAmount={}", userCouponId, orderId, discountAmount);
        return userCoupon;
    }

    private void validateCouponAvailable(UserCoupon userCoupon, CouponTemplate template, BigDecimal orderAmount) {
        if (!UserCouponStatusEnum.AVAILABLE.getCode().equals(userCoupon.getStatus())) {
            UserCouponStatusEnum statusEnum = UserCouponStatusEnum.getByCode(userCoupon.getStatus());
            throw new IllegalArgumentException("优惠券状态异常，当前状态：" + (statusEnum != null ? statusEnum.getDesc() : "未知"));
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(userCoupon.getValidStartTime())) {
            throw new IllegalArgumentException("优惠券尚未生效");
        }
        if (now.isAfter(userCoupon.getValidEndTime())) {
            throw new IllegalArgumentException("优惠券已过期");
        }

        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额无效");
        }

        if (orderAmount.compareTo(template.getMinAmount()) < 0) {
            throw new IllegalArgumentException("订单金额未达到满减门槛，需满" + template.getMinAmount() + "元可用");
        }
    }

    private BigDecimal calculateDiscountAmount(CouponTemplate template, BigDecimal orderAmount) {
        CouponTypeEnum type = CouponTypeEnum.getByCode(template.getType());
        BigDecimal discountAmount;

        if (type == CouponTypeEnum.FIXED_AMOUNT) {
            discountAmount = template.getDiscountAmount();
        } else if (type == CouponTypeEnum.DISCOUNT_RATE) {
            discountAmount = orderAmount.multiply(BigDecimal.ONE.subtract(template.getDiscountRate()))
                    .setScale(2, RoundingMode.DOWN);
        } else {
            throw new IllegalArgumentException("无效的优惠券类型");
        }

        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        return discountAmount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreCoupon(Long orderId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, UserCouponStatusEnum.USED.getCode());

        UserCoupon userCoupon = userCouponMapper.selectOne(wrapper);
        if (userCoupon == null) {
            return;
        }

        userCoupon.setStatus(UserCouponStatusEnum.AVAILABLE.getCode());
        userCoupon.setUsedTime(null);
        userCoupon.setOrderId(null);
        userCoupon.setDiscountAmount(null);
        userCouponMapper.updateById(userCoupon);

        couponTemplateMapper.decrementUsedCount(userCoupon.getTemplateId());

        logger.info("恢复优惠券可用状态: userCouponId={}, orderId={}", userCoupon.getId(), orderId);
    }

    @Override
    public List<CouponTemplate> getAvailableTemplates() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getStatus, CouponTemplateStatusEnum.ACTIVE.getCode())
                .apply("received_count < total_count")
                .and(w -> w.isNull(CouponTemplate::getValidStartTime)
                        .or().le(CouponTemplate::getValidStartTime, now))
                .and(w -> w.isNull(CouponTemplate::getValidEndTime)
                        .or().ge(CouponTemplate::getValidEndTime, now))
                .orderByDesc(CouponTemplate::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public List<UserCouponDTO> getAvailableCouponsForOrder(BigDecimal orderAmount) {
        Long userId = SecurityUtil.getCurrentUserId();
        userCouponMapper.expireCoupons(LocalDateTime.now());

        List<UserCouponDTO> coupons = userCouponMapper.selectUserCouponList(userId, UserCouponStatusEnum.AVAILABLE.getCode());

        return coupons.stream()
                .filter(coupon -> {
                    if (coupon.getMinAmount() == null) return true;
                    return orderAmount.compareTo(coupon.getMinAmount()) >= 0;
                })
                .peek(coupon -> {
                    coupon.setCouponTypeDesc(CouponTypeEnum.getByCode(coupon.getCouponType()).getDesc());
                    coupon.setStatusDesc(UserCouponStatusEnum.getByCode(coupon.getStatus()).getDesc());
                })
                .toList();
    }
}
