package com.example.demo.dto;

import lombok.Data;

@Data
public class UserAddressDTO {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
}
