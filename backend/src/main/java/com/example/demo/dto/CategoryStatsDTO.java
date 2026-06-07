package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品分类统计 DTO
 */
@Data
public class CategoryStatsDTO {
    private String category;
    private Long count;
    private BigDecimal totalStock;
    private BigDecimal avgPrice;
}
