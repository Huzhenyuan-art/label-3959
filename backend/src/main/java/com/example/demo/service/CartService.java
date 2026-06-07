package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.CartItemDTO;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Order;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<CartItemDTO> getMyCart();

    Cart addToCart(Long productId, Integer quantity);

    void updateQuantity(Long id, Integer quantity);

    void removeFromCart(Long id);

    void batchRemove(List<Long> ids);

    Order checkout(List<Long> cartIds, String remark);
}
