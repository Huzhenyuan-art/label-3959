package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.UserAddressDTO;
import com.example.demo.entity.UserAddress;
import com.example.demo.service.UserAddressService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public Result<List<UserAddress>> getMyAddresses() {
        return Result.ok(userAddressService.getMyAddresses());
    }

    @GetMapping("/default")
    public Result<UserAddress> getMyDefaultAddress() {
        return Result.ok(userAddressService.getMyDefaultAddress());
    }

    @GetMapping("/{id}")
    public Result<UserAddress> getAddressDetail(@PathVariable Long id) {
        return Result.ok(userAddressService.getAddressByIdAndCheckPermission(id));
    }

    @PostMapping
    public Result<UserAddress> createAddress(@RequestBody UserAddressDTO dto) {
        return Result.ok(userAddressService.createAddress(dto));
    }

    @PutMapping("/{id}")
    public Result<UserAddress> updateAddress(@PathVariable Long id, @RequestBody UserAddressDTO dto) {
        return Result.ok(userAddressService.updateAddress(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id) {
        userAddressService.setDefaultAddress(id);
        return Result.ok();
    }

    @Data
    public static class SetDefaultRequest {
        private Long id;
    }
}
