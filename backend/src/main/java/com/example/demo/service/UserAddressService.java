package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.UserAddressDTO;
import com.example.demo.entity.UserAddress;

import java.util.List;

public interface UserAddressService extends IService<UserAddress> {

    List<UserAddress> getMyAddresses();

    UserAddress getMyDefaultAddress();

    UserAddress createAddress(UserAddressDTO dto);

    UserAddress updateAddress(Long id, UserAddressDTO dto);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id);

    UserAddress getAddressByIdAndCheckPermission(Long id);
}
