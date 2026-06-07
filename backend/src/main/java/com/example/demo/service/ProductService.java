package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductService extends IService<Product> {

    IPage<Product> pageProducts(int current, int size, String name, String category, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, Integer maxStock);

    Product createProduct(Product product);

    Product updateProduct(Product product);

    void deleteProduct(Long id);

    List<CategoryStatsDTO> getCategoryStats(String name, String category, BigDecimal minPrice, BigDecimal maxPrice, Integer minStock, Integer maxStock);

    Map<Long, Integer> getOrderReferenceCounts(List<Long> productIds);

    int countOrderReferences(Long productId);
}
