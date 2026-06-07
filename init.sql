SET NAMES 'utf8mb4';
CREATE DATABASE IF NOT EXISTS mybatis_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mybatis_demo;

CREATE TABLE IF NOT EXISTS `user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)  NOT NULL,
  `email`        VARCHAR(100) DEFAULT NULL,
  `age`          INT          DEFAULT NULL,
  `password`     VARCHAR(255) NOT NULL,
  `role`         VARCHAR(20)  NOT NULL DEFAULT 'USER',
  `status`       TINYINT      NOT NULL DEFAULT 1,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  `version`      INT          NOT NULL DEFAULT 1,
  `created_time` DATETIME     DEFAULT NULL,
  `updated_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`), INDEX `idx_username` (`username`), INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product` (
  `id`           BIGINT         NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(100)   NOT NULL,
  `price`        DECIMAL(10,2)  NOT NULL,
  `stock`        INT            NOT NULL DEFAULT 0,
  `category`     VARCHAR(50)    DEFAULT NULL,
  `description`  VARCHAR(500)   DEFAULT NULL,
  `created_time` DATETIME       DEFAULT NULL,
  `updated_time` DATETIME       DEFAULT NULL,
  PRIMARY KEY (`id`), INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order` (
  `id`           BIGINT         NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT         NOT NULL,
  `total_amount` DECIMAL(12,2)  NOT NULL,
  `status`       TINYINT        NOT NULL DEFAULT 0,
  `remark`       VARCHAR(200)   DEFAULT NULL,
  `version`      INT            NOT NULL DEFAULT 1,
  `created_time` DATETIME       DEFAULT NULL,
  `updated_time` DATETIME       DEFAULT NULL,
  PRIMARY KEY (`id`), INDEX `idx_user_id` (`user_id`), INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
  `id`           BIGINT         NOT NULL AUTO_INCREMENT,
  `order_id`     BIGINT         NOT NULL,
  `product_id`   BIGINT         NOT NULL,
  `product_name` VARCHAR(100)   NOT NULL,
  `quantity`     INT            NOT NULL,
  `price`        DECIMAL(10,2)  NOT NULL,
  PRIMARY KEY (`id`), INDEX `idx_order_id` (`order_id`), INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT        NOT NULL,
  `type`          TINYINT       NOT NULL,
  `title`         VARCHAR(100)  NOT NULL,
  `content`       VARCHAR(500)  NOT NULL,
  `biz_id`        BIGINT        DEFAULT NULL,
  `biz_type`      VARCHAR(50)   DEFAULT NULL,
  `is_read`       TINYINT       NOT NULL DEFAULT 0,
  `created_time`  DATETIME      DEFAULT NULL,
  `read_time`     DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_user_read` (`user_id`, `is_read`),
  INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cart` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT        NOT NULL,
  `product_id`   BIGINT        NOT NULL,
  `quantity`     INT           NOT NULL DEFAULT 1,
  `created_time` DATETIME      DEFAULT NULL,
  `updated_time` DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
