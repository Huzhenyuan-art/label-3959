package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.FavoriteItemDTO;
import com.example.demo.entity.Favorite;
import com.example.demo.service.FavoriteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏接口
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<List<FavoriteItemDTO>> getMyFavorites() {
        return Result.ok(favoriteService.getMyFavorites());
    }

    @PostMapping
    public Result<Favorite> addFavorite(@RequestBody AddFavoriteRequest req) {
        return Result.ok(favoriteService.addFavorite(req.getProductId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeFavorite(@PathVariable Long id) {
        favoriteService.removeFavorite(id);
        return Result.ok();
    }

    @DeleteMapping("/product/{productId}")
    public Result<Void> removeFavoriteByProductId(@PathVariable Long productId) {
        favoriteService.removeFavoriteByProductId(productId);
        return Result.ok();
    }

    @GetMapping("/check/{productId}")
    public Result<Boolean> isFavorite(@PathVariable Long productId) {
        return Result.ok(favoriteService.isFavorite(productId));
    }

    @Data
    public static class AddFavoriteRequest {
        private Long productId;
    }
}
