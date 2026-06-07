package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.FavoriteItemDTO;
import com.example.demo.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    List<FavoriteItemDTO> selectFavoriteItemsByUserId(@Param("userId") Long userId);

    FavoriteItemDTO selectFavoriteItemById(@Param("userId") Long userId, @Param("id") Long id);
}
