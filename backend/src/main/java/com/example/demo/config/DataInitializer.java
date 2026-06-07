package com.example.demo.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.enums.RoleEnum;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getId, 1)) > 0) {
            log.info("演示数据已存在，跳过初始化");
            return;
        }
        log.info("开始插入演示数据...");
        insertUsers();
        insertProducts();
        insertOrders();
        log.info("演示数据初始化完成");
    }

    private void insertUsers() {
        List<User> users = List.of(
                makeUser("admin", "admin@example.com", 30, 1, RoleEnum.ADMIN.getCode(), "admin123"),
                makeUser("张三", "zhangsan@example.com", 25, 1, RoleEnum.USER.getCode(), "123456"),
                makeUser("李四", "lisi@example.com", 30, 1, RoleEnum.USER.getCode(), "123456"),
                makeUser("王五", "wangwu@example.com", 22, 1, RoleEnum.USER.getCode(), "123456"),
                makeUser("赵六", "zhaoliu@example.com", 35, 0, RoleEnum.USER.getCode(), "123456"),
                makeUser("钱七", "qianqi@example.com", 28, 1, RoleEnum.USER.getCode(), "123456"),
                makeUser("孙八", "sunba@example.com", 32, 1, RoleEnum.USER.getCode(), "123456")
        );
        users.forEach(userMapper::insert);
        log.info("插入用户 {} 条", users.size());
    }

    private User makeUser(String username, String email, int age, int status, String role, String rawPassword) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setAge(age);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setStatus(status);
        u.setDeleted(0);
        u.setVersion(1);
        u.setCreatedTime(LocalDateTime.now());
        u.setUpdatedTime(LocalDateTime.now());
        return u;
    }

    private void insertProducts() {
        List<Product> products = List.of(
                makeProduct("iPhone 15", new BigDecimal("5999.00"), 100, "手机", "苹果最新款手机，A16 芯片"),
                makeProduct("MacBook Pro 14", new BigDecimal("12999.00"), 50, "电脑", "苹果 M3 芯片笔记本电脑"),
                makeProduct("AirPods Pro", new BigDecimal("1499.00"), 200, "耳机", "苹果主动降噪无线耳机"),
                makeProduct("小米14", new BigDecimal("3999.00"), 150, "手机", "小米旗舰，骁龙8Gen3"),
                makeProduct("华为 Mate 60 Pro", new BigDecimal("6999.00"), 80, "手机", "华为旗舰，卫星通话"),
                makeProduct("ThinkPad X1 Carbon", new BigDecimal("9999.00"), 30, "电脑", "联想超薄商务本"),
                makeProduct("索尼 WH-1000XM5", new BigDecimal("2299.00"), 120, "耳机", "索尼旗舰降噪耳机"),
                makeProduct("iPad Pro 12.9", new BigDecimal("7999.00"), 60, "平板", "苹果 M2 芯片平板")
        );
        products.forEach(productMapper::insert);
        log.info("插入商品 {} 条", products.size());
    }

    private Product makeProduct(String name, BigDecimal price, int stock, String category, String desc) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory(category);
        p.setDescription(desc);
        p.setCreatedTime(LocalDateTime.now());
        p.setUpdatedTime(LocalDateTime.now());
        return p;
    }

    private void insertOrders() {
        insertOrder(1L, new BigDecimal("7498.00"), 3, "请尽快发货，谢谢",
                List.of(makeItem(1L, "iPhone 15", 1, new BigDecimal("5999.00")),
                        makeItem(3L, "AirPods Pro", 1, new BigDecimal("1499.00"))));

        insertOrder(2L, new BigDecimal("12999.00"), 1, null,
                List.of(makeItem(2L, "MacBook Pro 14", 1, new BigDecimal("12999.00"))));

        insertOrder(1L, new BigDecimal("3999.00"), 2, "好评",
                List.of(makeItem(4L, "小米14", 1, new BigDecimal("3999.00"))));

        insertOrder(3L, new BigDecimal("1499.00"), 0, null,
                List.of(makeItem(3L, "AirPods Pro", 1, new BigDecimal("1499.00"))));

        insertOrder(5L, new BigDecimal("16998.00"), 3, "非常满意",
                List.of(makeItem(2L, "MacBook Pro 14", 1, new BigDecimal("12999.00")),
                        makeItem(4L, "小米14", 1, new BigDecimal("3999.00"))));

        log.info("插入订单 5 条");
    }

    private void insertOrder(Long userId, BigDecimal total, int status, String remark, List<OrderItem> items) {
        Order o = new Order();
        o.setUserId(userId);
        o.setTotalAmount(total);
        o.setStatus(status);
        o.setRemark(remark);
        o.setVersion(1);
        o.setCreatedTime(LocalDateTime.now());
        o.setUpdatedTime(LocalDateTime.now());
        orderMapper.insert(o);
        items.forEach(item -> {
            item.setOrderId(o.getId());
            orderItemMapper.insert(item);
        });
    }

    private OrderItem makeItem(Long productId, String productName, int qty, BigDecimal price) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantity(qty);
        item.setPrice(price);
        return item;
    }
}
