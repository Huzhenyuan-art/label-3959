package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CartItemDTO;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.mapper.CartMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.CartService;
import com.example.demo.service.OrderService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;

    @Override
    public List<CartItemDTO> getMyCart() {
        Long userId = SecurityUtil.getCurrentUserId();
        return cartMapper.selectCartItemsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Cart addToCart(Long productId, Integer quantity) {
        Long userId = SecurityUtil.getCurrentUserId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("库存不足");
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, productId);
        Cart existingCart = getOne(wrapper);

        if (existingCart != null) {
            int newQuantity = existingCart.getQuantity() + quantity;
            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("库存不足");
            }
            existingCart.setQuantity(newQuantity);
            updateById(existingCart);
            log.info("更新购物车: userId={}, productId={}, quantity={}", userId, productId, newQuantity);
            return existingCart;
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            save(cart);
            log.info("添加购物车: userId={}, productId={}, quantity={}", userId, productId, quantity);
            return cart;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long id, Integer quantity) {
        Long userId = SecurityUtil.getCurrentUserId();
        Cart cart = getById(id);
        if (cart == null) {
            throw new IllegalArgumentException("购物车项不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new SecurityException("无权操作他人购物车");
        }

        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("库存不足");
        }

        cart.setQuantity(quantity);
        updateById(cart);
        log.info("更新购物车数量: id={}, quantity={}", id, quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromCart(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Cart cart = getById(id);
        if (cart == null) {
            throw new IllegalArgumentException("购物车项不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new SecurityException("无权操作他人购物车");
        }
        removeById(id);
        log.info("删除购物车项: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(List<Long> ids) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).in(Cart::getId, ids);
        remove(wrapper);
        log.info("批量删除购物车项: userId={}, ids={}", userId, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(List<Long> cartIds, String remark, Long userCouponId, Long addressId) {
        Long userId = SecurityUtil.getCurrentUserId();

        if (cartIds == null || cartIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要结算的商品");
        }

        List<CartItemDTO> cartItems = cartMapper.selectCartItemsByIds(userId, cartIds);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("购物车项不存在或无权操作");
        }

        for (CartItemDTO item : cartItems) {
            if (item.getProductStock() < item.getQuantity()) {
                throw new IllegalArgumentException("商品【" + item.getProductName() + "】库存不足");
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItemDTO item : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getProductPrice());
            orderItems.add(orderItem);

            BigDecimal itemTotal = item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            Product product = productMapper.selectById(item.getProductId());
            product.setStock(product.getStock() - item.getQuantity());
            productMapper.updateById(product);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setRemark(remark);
        order.setStatus(0);
        order.setVersion(1);

        Order createdOrder = orderService.createOrder(order, orderItems, userCouponId, addressId);

        batchRemove(cartIds);

        log.info("购物车结算成功: userId={}, orderId={}, itemCount={}", userId, createdOrder.getId(), cartItems.size());
        return createdOrder;
    }
}
