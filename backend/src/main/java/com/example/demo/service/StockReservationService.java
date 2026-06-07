package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.StockReservationCreateDTO;
import com.example.demo.dto.StockReservationDTO;

import java.util.List;

public interface StockReservationService {

    void createReservations(StockReservationCreateDTO dto);

    void releaseReservations(Long orderId, String reason);

    void deductStock(Long orderId);

    IPage<StockReservationDTO> pageReservations(int current, int size, Long orderId, Long productId, Integer status);

    List<StockReservationDTO> getReservationsByOrderId(Long orderId);

    int releaseExpiredReservations();
}
