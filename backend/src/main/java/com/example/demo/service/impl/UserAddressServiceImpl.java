package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.UserAddressDTO;
import com.example.demo.entity.UserAddress;
import com.example.demo.mapper.UserAddressMapper;
import com.example.demo.service.UserAddressService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> getMyAddresses() {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public UserAddress getMyDefaultAddress() {
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress createAddress(UserAddressDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        validateDTO(dto);

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0);

        if (address.getIsDefault() == 1) {
            cancelOtherDefaultAddresses(userId);
        } else {
            long count = count(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
            if (count == 0) {
                address.setIsDefault(1);
            }
        }

        save(address);
        log.info("创建收货地址: userId={}, addressId={}", userId, address.getId());
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress updateAddress(Long id, UserAddressDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserAddress address = getAddressByIdAndCheckPermission(id);
        validateDTO(dto);

        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());

        Integer oldIsDefault = address.getIsDefault();
        Integer newIsDefault = dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0;

        if (newIsDefault == 1 && oldIsDefault == 0) {
            cancelOtherDefaultAddresses(userId);
        }
        address.setIsDefault(newIsDefault);

        updateById(address);
        log.info("更新收货地址: userId={}, addressId={}", userId, id);
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long id) {
        UserAddress address = getAddressByIdAndCheckPermission(id);
        removeById(id);

        if (address.getIsDefault() == 1) {
            Long userId = SecurityUtil.getCurrentUserId();
            LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getCreatedTime);
            List<UserAddress> remaining = list(wrapper);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(1);
                updateById(remaining.get(0));
                log.info("删除默认地址后，自动设置新的默认地址: userId={}, newDefaultId={}", userId, remaining.get(0).getId());
            }
        }

        log.info("删除收货地址: addressId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserAddress address = getAddressByIdAndCheckPermission(id);

        cancelOtherDefaultAddresses(userId);
        address.setIsDefault(1);
        updateById(address);

        log.info("设置默认地址: userId={}, addressId={}", userId, id);
    }

    @Override
    public UserAddress getAddressByIdAndCheckPermission(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserAddress address = getById(id);
        if (address == null) {
            throw new IllegalArgumentException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new SecurityException("无权操作他人地址");
        }
        return address;
    }

    private void cancelOtherDefaultAddresses(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1);
        List<UserAddress> defaultAddresses = list(wrapper);
        for (UserAddress addr : defaultAddresses) {
            addr.setIsDefault(0);
            updateById(addr);
        }
    }

    private void validateDTO(UserAddressDTO dto) {
        if (dto.getReceiverName() == null || dto.getReceiverName().trim().isEmpty()) {
            throw new IllegalArgumentException("收件人姓名不能为空");
        }
        if (dto.getReceiverPhone() == null || dto.getReceiverPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!dto.getReceiverPhone().matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (dto.getDetailAddress() == null || dto.getDetailAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("详细地址不能为空");
        }
    }
}
