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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品 Service 实现
 * 演示：分页查询、条件构造器、自定义 SQL 统计
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final int STOCK_WARNING_THRESHOLD = 10;

    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    @Override
    public IPage<Product> pageProducts(int current, int size, String name, String category,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minStock, Integer maxStock) {
        // 演示：QueryWrapper 多条件分页（含价格区间、库存区间过滤）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(name), Product::getName, name)
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice)
                .ge(minStock != null, Product::getStock, minStock)
                .le(maxStock != null, Product::getStock, maxStock)
                .orderByDesc(Product::getCreatedTime);

        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public Product createProduct(Product product) {
        save(product);
        logger.info("创建商品: id={}, name={}", product.getId(), product.getName());
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
            logger.info("库存预警已发送: productId={}, name={}, stock={}",
                    product.getId(), product.getName(), product.getStock());
        }
    }

    @Override
    public void deleteProduct(Long id) {
        removeById(id);
        logger.info("删除商品: id={}", id);
    }

    @Override
    public List<CategoryStatsDTO> getCategoryStats(String name, String category,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   Integer minStock, Integer maxStock) {
        // 先按过滤条件查询商品，再在内存中分组统计（保持与分页查询一致的过滤逻辑）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(name), Product::getName, name)
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice)
                .ge(minStock != null, Product::getStock, minStock)
                .le(maxStock != null, Product::getStock, maxStock);

        List<Product> products = list(wrapper);

        Map<String, List<Product>> grouped = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory));

        return grouped.entrySet().stream()
                .map(entry -> {
                    CategoryStatsDTO dto = new CategoryStatsDTO();
                    dto.setCategory(entry.getKey());
                    dto.setCount((long) entry.getValue().size());
                    BigDecimal totalStock = entry.getValue().stream()
                            .map(p -> BigDecimal.valueOf(p.getStock() != null ? p.getStock() : 0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dto.setTotalStock(totalStock);
                    BigDecimal avgPrice = entry.getValue().stream()
                            .map(Product::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(entry.getValue().size()), 2, java.math.RoundingMode.HALF_UP);
                    dto.setAvgPrice(avgPrice);
                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }
}
