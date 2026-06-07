package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.PendingReviewDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewStatsDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.ProductReview;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductReviewMapper;
import com.example.demo.service.ProductReviewService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ProductReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ProductReviewServiceImpl.class);

    private final ProductReviewMapper productReviewMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductReview submitReview(Long orderItemId, Integer rating, String content) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("请先登录");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }

        OrderItem orderItem = orderItemMapper.selectById(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单明细不存在: " + orderItemId);
        }

        Order order = orderMapper.selectById(orderItem.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderItem.getOrderId());
        }

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("无权评价他人订单的商品");
        }

        if (order.getStatus() != 3) {
            throw new IllegalArgumentException("仅已完成的订单可以评价，当前订单状态: " + order.getStatus());
        }

        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getOrderItemId, orderItemId);
        if (count(wrapper) > 0) {
            throw new IllegalArgumentException("该商品已评价过，不能重复评价");
        }

        ProductReview review = new ProductReview();
        review.setUserId(userId);
        review.setProductId(orderItem.getProductId());
        review.setOrderItemId(orderItemId);
        review.setOrderId(orderItem.getOrderId());
        review.setRating(rating);
        review.setContent(content);
        save(review);

        logger.info("提交评价成功: reviewId={}, productId={}, orderItemId={}, rating={}", review.getId(), review.getProductId(), orderItemId, rating);
        return review;
    }

    @Override
    public IPage<ReviewDTO> getReviewPage(int current, int size, Long productId, Integer rating) {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        Page<ReviewDTO> page = new Page<>(current, size);
        return productReviewMapper.selectReviewPage(page, productId, rating);
    }

    @Override
    public ReviewStatsDTO getReviewStats(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        return productReviewMapper.selectReviewStats(productId);
    }

    @Override
    public List<PendingReviewDTO> getPendingReviews() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("请先登录");
        }
        return productReviewMapper.selectPendingReviews(userId);
    }
}
