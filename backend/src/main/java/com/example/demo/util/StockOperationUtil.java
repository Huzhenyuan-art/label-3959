package com.example.demo.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.StockReservation;
import com.example.demo.enums.StockReservationStatusEnum;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.StockReservationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public final class StockOperationUtil {

    private static final Logger logger = LoggerFactory.getLogger(StockOperationUtil.class);

    private StockOperationUtil() {
    }

    public static Product getProductOrThrow(ProductMapper mapper, Long productId) {
        Product product = mapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + productId);
        }
        return product;
    }

    public static int getAvailableStock(Product product) {
        return product.getStock() - (product.getReservedStock() == null ? 0 : product.getReservedStock());
    }

    public static void validateStockAvailability(Product product, int requestedQuantity) {
        int availableStock = getAvailableStock(product);
        if (availableStock < requestedQuantity) {
            throw new IllegalArgumentException("商品【" + product.getName() + "】库存不足，可用库存: "
                    + availableStock + "，需要: " + requestedQuantity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public static void rollbackStock(Long orderId, OrderItemMapper orderItemMapper, ProductMapper productMapper) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
                logger.info("库存回滚: productId={}, productName={}, quantity={}",
                        item.getProductId(), item.getProductName(), item.getQuantity());
            }
        }
    }

    public static List<StockReservation> getActiveReservations(StockReservationMapper mapper, Long orderId) {
        return mapper.selectList(
                new LambdaQueryWrapper<StockReservation>()
                        .eq(StockReservation::getOrderId, orderId)
                        .eq(StockReservation::getStatus, StockReservationStatusEnum.RESERVED.getCode())
        );
    }
}
