package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewStatsDTO {

    private Long productId;

    private String productName;

    private BigDecimal avgRating;

    private Long totalCount;

    private Long rating1Count;
    private Long rating2Count;
    private Long rating3Count;
    private Long rating4Count;
    private Long rating5Count;
}
