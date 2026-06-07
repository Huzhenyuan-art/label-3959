package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.StockReservationCreateDTO;
import com.example.demo.dto.StockReservationDTO;
import com.example.demo.entity.Product;
import com.example.demo.entity.StockReservation;
import com.example.demo.enums.StockReservationStatusEnum;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.StockReservationMapper;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl extends ServiceImpl<StockReservationMapper, StockReservation> implements StockReservationService {

    private final StockReservationMapper stockReservationMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservations(StockReservationCreateDTO dto) {
        Long orderId = dto.getOrderId();
        LocalDateTime expireTime = dto.getExpireTime();

        List<StockReservationCreateDTO.ReservationItem> items = dto.getItems();
        for (StockReservationCreateDTO.ReservationItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在: " + item.getProductId());
            }

            int availableStock = product.getStock() - (product.getReservedStock() == null ? 0 : product.getReservedStock());
            if (availableStock < item.getQuantity()) {
                throw new IllegalArgumentException("商品【" + product.getName() + "】库存不足，可用库存: " + availableStock + "，需要: " + item.getQuantity());
            }

            int updated = stockReservationMapper.increaseReservedStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                throw new IllegalArgumentException("商品【" + product.getName() + "】预占库存失败，请稍后重试");
            }

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(orderId);
            reservation.setOrderItemId(item.getOrderItemId());
            reservation.setProductId(item.getProductId());
            reservation.setProductName(item.getProductName());
            reservation.setQuantity(item.getQuantity());
            reservation.setStatus(StockReservationStatusEnum.RESERVED.getCode());
            reservation.setExpireTime(expireTime);
            save(reservation);

            log.info("创建库存预占成功: orderId={}, productId={}, productName={}, quantity={}",
                    orderId, item.getProductId(), item.getProductName(), item.getQuantity());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservations(Long orderId, String reason) {
        LambdaQueryWrapper<StockReservation> wrapper = new LambdaQueryWrapper<StockReservation>()
                .eq(StockReservation::getOrderId, orderId)
                .eq(StockReservation::getStatus, StockReservationStatusEnum.RESERVED.getCode());

        List<StockReservation> reservations = list(wrapper);
        if (reservations.isEmpty()) {
            log.warn("未找到需要释放的预占记录: orderId={}", orderId);
            return;
        }

        for (StockReservation reservation : reservations) {
            int updated = stockReservationMapper.decreaseReservedStock(reservation.getProductId(), reservation.getQuantity());
            if (updated == 0) {
                throw new IllegalArgumentException("释放预占库存失败，商品ID: " + reservation.getProductId());
            }

            reservation.setStatus(StockReservationStatusEnum.RELEASED.getCode());
            reservation.setReleaseReason(reason);
            updateById(reservation);

            log.info("释放库存预占: orderId={}, productId={}, productName={}, quantity={}, reason={}",
                    orderId, reservation.getProductId(), reservation.getProductName(), reservation.getQuantity(), reason);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long orderId) {
        LambdaQueryWrapper<StockReservation> wrapper = new LambdaQueryWrapper<StockReservation>()
                .eq(StockReservation::getOrderId, orderId)
                .eq(StockReservation::getStatus, StockReservationStatusEnum.RESERVED.getCode());

        List<StockReservation> reservations = list(wrapper);
        if (reservations.isEmpty()) {
            log.warn("未找到需要扣减的预占记录: orderId={}", orderId);
            return;
        }

        for (StockReservation reservation : reservations) {
            int updated = stockReservationMapper.deductStock(reservation.getProductId(), reservation.getQuantity());
            if (updated == 0) {
                throw new IllegalArgumentException("扣减库存失败，商品ID: " + reservation.getProductId() + "，商品名称: " + reservation.getProductName());
            }

            reservation.setStatus(StockReservationStatusEnum.DEDUCTED.getCode());
            updateById(reservation);

            log.info("正式扣减库存: orderId={}, productId={}, productName={}, quantity={}",
                    orderId, reservation.getProductId(), reservation.getProductName(), reservation.getQuantity());
        }
    }

    @Override
    public IPage<StockReservationDTO> pageReservations(int current, int size, Long orderId, Long productId, Integer status) {
        Long currentUserId = SecurityUtil.isAdmin() ? null : SecurityUtil.getCurrentUserId();
        Page<StockReservationDTO> page = new Page<>(current, size);
        return stockReservationMapper.selectReservationPage(page, orderId, productId, status, currentUserId);
    }

    @Override
    public List<StockReservationDTO> getReservationsByOrderId(Long orderId) {
        return stockReservationMapper.selectByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expiredReservations = stockReservationMapper.selectExpiredReservations(now);
        if (expiredReservations.isEmpty()) {
            return 0;
        }

        int updatedCount = stockReservationMapper.releaseExpiredReservations(now, "预占超时自动释放");

        for (StockReservation reservation : expiredReservations) {
            stockReservationMapper.decreaseReservedStock(reservation.getProductId(), reservation.getQuantity());
            log.info("超时自动释放预占: reservationId={}, orderId={}, productId={}, productName={}, quantity={}",
                    reservation.getId(), reservation.getOrderId(), reservation.getProductId(), reservation.getProductName(), reservation.getQuantity());
        }

        log.info("本次超时释放预占记录数: {}", updatedCount);
        return updatedCount;
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledReleaseExpired() {
        try {
            int count = releaseExpiredReservations();
            if (count > 0) {
                log.info("定时任务执行完成，释放超时预占记录: {} 条", count);
            }
        } catch (Exception e) {
            log.error("定时释放超时预占失败", e);
        }
    }
}
