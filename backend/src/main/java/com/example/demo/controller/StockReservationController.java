package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.StockReservationDTO;
import com.example.demo.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-reservations")
@RequiredArgsConstructor
public class StockReservationController {

    private final StockReservationService stockReservationService;

    @GetMapping("/page")
    public Result<IPage<StockReservationDTO>> page(@RequestParam(defaultValue = "1") int current,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) Long orderId,
                                                   @RequestParam(required = false) Long productId,
                                                   @RequestParam(required = false) Integer status) {
        return Result.ok(stockReservationService.pageReservations(current, size, orderId, productId, status));
    }

    @GetMapping("/order/{orderId}")
    public Result<List<StockReservationDTO>> getByOrderId(@PathVariable Long orderId) {
        return Result.ok(stockReservationService.getReservationsByOrderId(orderId));
    }

    @PostMapping("/release-expired")
    public Result<Integer> releaseExpired() {
        int count = stockReservationService.releaseExpiredReservations();
        return Result.ok(count);
    }
}
