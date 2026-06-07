package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.mapper.OrderItemMapper;
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
    private final OrderItemMapper orderItemMapper;

    @Override
    public IPage<Product> pageProducts(int current, int size, String name, String category,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minStock, Integer maxStock) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(name), Product::getName, name)
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice)
                .ge(minStock != null, Product::getStock, minStock)
                .le(maxStock != null, Product::getStock, maxStock)
                .orderByDesc(Product::getCreatedTime);

        IPage<Product> page = page(new Page<>(current, size), wrapper);

        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            List<Long> productIds = page.getRecords().stream()
                    .map(Product::getId)
                    .collect(Collectors.toList());
            Map<Long, Integer> orderCountMap = getOrderReferenceCounts(productIds);
            page.getRecords().forEach(p -> p.setOrderCount(orderCountMap.getOrDefault(p.getId(), 0)));
        }

        return page;
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
        int orderCount = countOrderReferences(id);
        if (orderCount > 0) {
            throw new IllegalArgumentException("该商品已被 " + orderCount + " 个订单使用，无法删除");
        }
        removeById(id);
        logger.info("删除商品: id={}", id);
    }

    @Override
    public Map<Long, Integer> getOrderReferenceCounts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getProductId, productIds)
                .select(OrderItem::getProductId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        return items.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    @Override
    public int countOrderReferences(Long productId) {
        if (productId == null) {
            return 0;
        }
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getProductId, productId);
        Long count = orderItemMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public List<CategoryStatsDTO> getCategoryStats(String name, String category,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   Integer minStock, Integer maxStock) {
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
