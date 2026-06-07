package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.CartItemDTO;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Order;
import com.example.demo.service.CartService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<List<CartItemDTO>> getMyCart() {
        return Result.ok(cartService.getMyCart());
    }

    @PostMapping
    public Result<Cart> addToCart(@RequestBody AddToCartRequest req) {
        return Result.ok(cartService.addToCart(req.getProductId(), req.getQuantity()));
    }

    @PutMapping("/{id}/quantity")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestBody UpdateQuantityRequest req) {
        cartService.updateQuantity(id, req.getQuantity());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchRemove(@RequestBody BatchRemoveRequest req) {
        cartService.batchRemove(req.getIds());
        return Result.ok();
    }

    @PostMapping("/checkout")
    public Result<Order> checkout(@RequestBody CheckoutRequest req) {
        return Result.ok(cartService.checkout(req.getCartIds(), req.getRemark(), req.getUserCouponId()));
    }

    @Data
    public static class AddToCartRequest {
        private Long productId;
        private Integer quantity;
    }

    @Data
    public static class UpdateQuantityRequest {
        private Integer quantity;
    }

    @Data
    public static class BatchRemoveRequest {
        private List<Long> ids;
    }

    @Data
    public static class CheckoutRequest {
        private List<Long> cartIds;
        private String remark;
        private Long userCouponId;
    }
}
