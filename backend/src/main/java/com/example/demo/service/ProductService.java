package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    IPage<Product> pageProducts(int current, int size, String name, String category);

    Product createProduct(Product product);

    Product updateProduct(Product product);

    void deleteProduct(Long id);

    List<CategoryStatsDTO> getCategoryStats();
}
