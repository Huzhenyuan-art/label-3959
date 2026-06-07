package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.PendingReviewDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewStatsDTO;
import com.example.demo.entity.ProductReview;
import com.example.demo.service.ProductReviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @PostMapping
    public Result<ProductReview> submit(@RequestBody SubmitReviewRequest req) {
        return Result.ok(productReviewService.submitReview(req.getOrderItemId(), req.getRating(), req.getContent()));
    }

    @GetMapping("/page")
    public Result<IPage<ReviewDTO>> page(@RequestParam(defaultValue = "1") int current,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam Long productId,
                                         @RequestParam(required = false) Integer rating) {
        return Result.ok(productReviewService.getReviewPage(current, size, productId, rating));
    }

    @GetMapping("/stats/{productId}")
    public Result<ReviewStatsDTO> stats(@PathVariable Long productId) {
        return Result.ok(productReviewService.getReviewStats(productId));
    }

    @GetMapping("/pending")
    public Result<List<PendingReviewDTO>> pending() {
        return Result.ok(productReviewService.getPendingReviews());
    }

    @Data
    public static class SubmitReviewRequest {
        private Long orderItemId;
        private Integer rating;
        private String content;
    }
}
