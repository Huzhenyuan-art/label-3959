package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.PendingReviewDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewStatsDTO;
import com.example.demo.entity.ProductReview;

import java.util.List;

public interface ProductReviewService extends IService<ProductReview> {

    ProductReview submitReview(Long orderItemId, Integer rating, String content);

    IPage<ReviewDTO> getReviewPage(int current, int size, Long productId, Integer rating);

    ReviewStatsDTO getReviewStats(Long productId);

    List<PendingReviewDTO> getPendingReviews();
}
