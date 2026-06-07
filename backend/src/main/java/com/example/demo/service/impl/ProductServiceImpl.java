package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.NotificationService;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品 Service 实现
 * 演示：分页查询、条件构造器、自定义 SQL 统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final int STOCK_WARNING_THRESHOLD = 10;

    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    @Override
    public IPage<Product> pageProducts(int current, int size, String name, String category) {
        // 演示：QueryWrapper 多条件分页
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(name), Product::getName, name)
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .orderByDesc(Product::getCreatedTime);

        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public Product createProduct(Product product) {
        save(product);
        log.info("创建商品: id={}, name={}", product.getId(), product.getName());
        checkStockWarning(product);
        return product;
    }

    @Override
    public Product updateProduct(Product product) {
        Product oldProduct = getById(product.getId());
        updateById(product);
        Product updated = getById(product.getId());
        if (oldProduct == null || oldProduct.getStock() == null || !oldProduct.getStock().equals(updated.getStock())) {
            checkStockWarning(updated);
        }
        return updated;
    }

    private void checkStockWarning(Product product) {
        if (product.getStock() != null && product.getStock() <= STOCK_WARNING_THRESHOLD) {
            notificationService.sendStockWarningNotification(
                    product.getId(),
                    product.getName(),
                    product.getStock()
            );
            log.info("库存预警已发送: productId={}, name={}, stock={}",
                    product.getId(), product.getName(), product.getStock());
        }
    }

    @Override
    public void deleteProduct(Long id) {
        removeById(id);
        log.info("删除商品: id={}", id);
    }

    @Override
    public List<CategoryStatsDTO> getCategoryStats() {
        // 演示：自定义注解 SQL 分组统计
        return productMapper.selectCategoryStats();
    }
}
