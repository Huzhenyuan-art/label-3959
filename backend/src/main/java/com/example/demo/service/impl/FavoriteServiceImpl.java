package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.FavoriteItemDTO;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Product;
import com.example.demo.mapper.FavoriteMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.FavoriteService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    @Override
    public List<FavoriteItemDTO> getMyFavorites() {
        Long userId = SecurityUtil.getCurrentUserId();
        return favoriteMapper.selectFavoriteItemsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Favorite addFavorite(Long productId) {
        Long userId = SecurityUtil.getCurrentUserId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        Favorite existingFavorite = getOne(wrapper);

        if (existingFavorite != null) {
            logger.info("商品已在收藏中: userId={}, productId={}", userId, productId);
            return existingFavorite;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        save(favorite);
        logger.info("添加收藏: userId={}, productId={}, favoriteId={}", userId, productId, favorite.getId());
        return favorite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Favorite favorite = getById(id);
        if (favorite == null) {
            throw new IllegalArgumentException("收藏记录不存在");
        }
        if (!favorite.getUserId().equals(userId)) {
            throw new SecurityException("无权操作他人收藏");
        }
        removeById(id);
        logger.info("取消收藏: userId={}, favoriteId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavoriteByProductId(Long productId) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        Favorite favorite = getOne(wrapper);
        if (favorite != null) {
            if (!favorite.getUserId().equals(userId)) {
                throw new SecurityException("无权操作他人收藏");
            }
            removeById(favorite.getId());
            logger.info("取消收藏(按商品ID): userId={}, productId={}", userId, productId);
        }
    }

    @Override
    public boolean isFavorite(Long productId) {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        return count(wrapper) > 0;
    }
}
