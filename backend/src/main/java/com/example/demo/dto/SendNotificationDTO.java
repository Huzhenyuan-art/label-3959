package com.example.demo.dto;

import lombok.Data;

@Data
public class SendNotificationDTO {

    private Long userId;

    private Integer type;

    private String title;

    private String content;

    private Long bizId;

    private String bizType;
}
