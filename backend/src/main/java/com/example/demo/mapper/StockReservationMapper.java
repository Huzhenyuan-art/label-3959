package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.StockReservationDTO;
import com.example.demo.entity.StockReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StockReservationMapper extends BaseMapper<StockReservation> {

    IPage<StockReservationDTO> selectReservationPage(Page<StockReservationDTO> page,
                                                     @Param("orderId") Long orderId,
                                                     @Param("productId") Long productId,
                                                     @Param("status") Integer status,
                                                     @Param("currentUserId") Long currentUserId);

    List<StockReservationDTO> selectByOrderId(@Param("orderId") Long orderId);

    @Update("UPDATE product SET reserved_stock = reserved_stock + #{quantity} " +
            "WHERE id = #{productId} AND (stock - reserved_stock) >= #{quantity}")
    int increaseReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET reserved_stock = reserved_stock - #{quantity} " +
            "WHERE id = #{productId} AND reserved_stock >= #{quantity}")
    int decreaseReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock - #{quantity}, reserved_stock = reserved_stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND reserved_stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE stock_reservation SET status = 1, release_reason = #{reason}, updated_time = NOW() " +
            "WHERE status = 0 AND expire_time <= #{now}")
    int releaseExpiredReservations(@Param("now") LocalDateTime now, @Param("reason") String reason);

    List<StockReservation> selectExpiredReservations(@Param("now") LocalDateTime now);
}
