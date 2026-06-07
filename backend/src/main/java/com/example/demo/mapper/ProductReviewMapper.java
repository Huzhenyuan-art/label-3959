package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.PendingReviewDTO;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewStatsDTO;
import com.example.demo.entity.ProductReview;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    IPage<ReviewDTO> selectReviewPage(Page<ReviewDTO> page, @Param("productId") Long productId, @Param("rating") Integer rating);

    ReviewStatsDTO selectReviewStats(@Param("productId") Long productId);

    List<PendingReviewDTO> selectPendingReviews(@Param("userId") Long userId);
}
