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

CREATE TABLE IF NOT EXISTS `product_review` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT        NOT NULL,
  `product_id`     BIGINT        NOT NULL,
  `order_item_id`  BIGINT        NOT NULL,
  `order_id`       BIGINT        NOT NULL,
  `rating`         TINYINT       NOT NULL,
  `content`        VARCHAR(500)  DEFAULT NULL,
  `created_time`   DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_item` (`order_item_id`),
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT,
  `name`             VARCHAR(100)   NOT NULL,
  `type`             TINYINT        NOT NULL,
  `discount_amount`  DECIMAL(10,2)  DEFAULT NULL,
  `discount_rate`    DECIMAL(3,2)   DEFAULT NULL,
  `min_amount`       DECIMAL(10,2)  NOT NULL DEFAULT 0,
  `total_count`      INT            NOT NULL DEFAULT 0,
  `received_count`   INT            NOT NULL DEFAULT 0,
  `used_count`       INT            NOT NULL DEFAULT 0,
  `per_user_limit`   INT            NOT NULL DEFAULT 1,
  `valid_start_time` DATETIME       DEFAULT NULL,
  `valid_end_time`   DATETIME       DEFAULT NULL,
  `valid_days`       INT            DEFAULT NULL,
  `status`           TINYINT        NOT NULL DEFAULT 1,
  `description`      VARCHAR(500)   DEFAULT NULL,
  `created_time`     DATETIME       DEFAULT NULL,
  `updated_time`     DATETIME       DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_valid_time` (`valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id`               BIGINT         NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT         NOT NULL,
  `template_id`      BIGINT         NOT NULL,
  `coupon_code`      VARCHAR(50)    DEFAULT NULL,
  `status`           TINYINT        NOT NULL DEFAULT 0,
  `used_time`        DATETIME       DEFAULT NULL,
  `order_id`         BIGINT         DEFAULT NULL,
  `discount_amount`  DECIMAL(10,2)  DEFAULT NULL,
  `valid_start_time` DATETIME       NOT NULL,
  `valid_end_time`   DATETIME       NOT NULL,
  `created_time`     DATETIME       DEFAULT NULL,
  `updated_time`     DATETIME       DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_template_id` (`template_id`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_valid_time` (`valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `order` ADD COLUMN `coupon_id` BIGINT DEFAULT NULL AFTER `remark`;
ALTER TABLE `order` ADD COLUMN `discount_amount` DECIMAL(10,2) DEFAULT 0 AFTER `coupon_id`;
ALTER TABLE `order` ADD INDEX `idx_coupon_id` (`coupon_id`);

CREATE TABLE IF NOT EXISTS `user_address` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT        NOT NULL,
  `receiver_name`   VARCHAR(50)  NOT NULL,
  `receiver_phone`  VARCHAR(20)  NOT NULL,
  `province`     VARCHAR(50)   DEFAULT NULL,
  `city`         VARCHAR(50)   DEFAULT NULL,
  `district`     VARCHAR(50)   DEFAULT NULL,
  `detail_address` VARCHAR(200) NOT NULL,
  `is_default`   TINYINT       NOT NULL DEFAULT 0,
  `deleted`      TINYINT       NOT NULL DEFAULT 0,
  `created_time` DATETIME      DEFAULT NULL,
  `updated_time` DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `order` ADD COLUMN `address_id` BIGINT DEFAULT NULL AFTER `discount_amount`;
ALTER TABLE `order` ADD COLUMN `receiver_name` VARCHAR(50) DEFAULT NULL AFTER `address_id`;
ALTER TABLE `order` ADD COLUMN `receiver_phone` VARCHAR(20) DEFAULT NULL AFTER `receiver_name`;
ALTER TABLE `order` ADD COLUMN `receiver_address` VARCHAR(300) DEFAULT NULL AFTER `receiver_phone`;
ALTER TABLE `order` ADD INDEX `idx_address_id` (`address_id`);

ALTER TABLE `product` ADD COLUMN `reserved_stock` INT NOT NULL DEFAULT 0 AFTER `stock`;
ALTER TABLE `product` ADD INDEX `idx_reserved_stock` (`reserved_stock`);

CREATE TABLE IF NOT EXISTS `stock_reservation` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`        BIGINT        NOT NULL,
  `order_item_id`   BIGINT        DEFAULT NULL,
  `product_id`      BIGINT        NOT NULL,
  `product_name`    VARCHAR(100)  NOT NULL,
  `quantity`        INT           NOT NULL,
  `status`          TINYINT       NOT NULL DEFAULT 0,
  `expire_time`     DATETIME      DEFAULT NULL,
  `release_reason`  VARCHAR(200)  DEFAULT NULL,
  `created_time`    DATETIME      DEFAULT NULL,
  `updated_time`    DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_expire_time` (`expire_time`),
  INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
