package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.CartItemDTO;
import com.example.demo.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    List<CartItemDTO> selectCartItemsByUserId(@Param("userId") Long userId);

    List<CartItemDTO> selectCartItemsByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
