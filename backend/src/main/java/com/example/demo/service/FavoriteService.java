package com.example.demo.service;

import com.example.demo.dto.FavoriteItemDTO;
import com.example.demo.entity.Favorite;

import java.util.List;

public interface FavoriteService {

    List<FavoriteItemDTO> getMyFavorites();

    Favorite addFavorite(Long productId);

    void removeFavorite(Long id);

    void removeFavoriteByProductId(Long productId);

    boolean isFavorite(Long productId);
}
