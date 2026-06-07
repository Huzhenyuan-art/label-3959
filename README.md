# MyBatis Plus Demo

> 一个前后端分离的全栈演示项目，系统展示 MyBatis Plus 3.5.x 的 **11 种核心特性**，包含多表联查、乐观锁、逻辑删除、自动填充等。

## Tech Stack

| 层 | 技术 |
|---|---|
| **前端** | Vue 3 + Element Plus + Vite |
| **后端** | Spring Boot 3.2 + MyBatis Plus 3.5.7 |
| **数据库** | MySQL 8.0 |
| **容器化** | Docker + Docker Compose |

---

## MyBatis Plus 演示特性

### 1. BaseMapper — 基础 CRUD

继承 `BaseMapper<T>` 即可获得完整的单表增删改查，无需写任何 SQL。

```java
// UserMapper.java
public interface UserMapper extends BaseMapper<User> {}

// 调用示例
userMapper.insert(user);
userMapper.selectById(1L);
userMapper.updateById(user);
userMapper.deleteById(1L);
```

**演示页面：** 用户管理 → 新增 / 编辑 / 删除

---

### 2. IService / ServiceImpl — 服务层增强

继承 `ServiceImpl<M, T>` 获得 `save`、`list`、`page` 等批量 + 分页快捷方法。

```java
// UserServiceImpl.java
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}

// 批量插入
saveBatch(users);

// 链式查询
lambdaQuery().eq(User::getStatus, 1).list();
```

---

### 3. LambdaQueryWrapper — 类型安全条件构造

使用 Lambda 引用字段名，编译期检查，彻底告别字符串拼写错误。

```java
// UserServiceImpl.java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
    .like(StringUtils.hasText(username), User::getUsername, username)
    .eq(status != null, User::getStatus, status)
    .orderByDesc(User::getCreatedTime);

return page(new Page<>(current, size), wrapper);
```

**演示页面：** 用户管理 / 商品管理 → 搜索栏多条件过滤

---

### 4. 分页插件 — IPage + PaginationInnerInterceptor

注册插件后，调用 `page()` 方法自动完成 `COUNT(*)` + `LIMIT` 分页，无需手写。

```java
// MybatisPlusConfig.java — 注册分页插件
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

// ProductServiceImpl.java — 使用
IPage<Product> result = page(new Page<>(current, size), wrapper);
// result.getRecords()  当前页数据
// result.getTotal()    总记录数
// result.getPages()    总页数
```

**演示页面：** 商品管理 → 底部分页组件（带总数显示）

---

### 5. 逻辑删除 — @TableLogic

在字段上加 `@TableLogic`，删除操作变为 `UPDATE ... SET deleted=1`，所有查询自动附加 `AND deleted=0`。

```java
// User.java
@TableLogic
private Integer deleted;   // 0-正常  1-已删除
```

```sql
-- 执行 removeById(1) 实际生成：
UPDATE user SET deleted=1 WHERE id=1

-- 执行 selectList() 实际生成：
SELECT * FROM user WHERE deleted=0
```

**演示页面：** 用户管理 → 点击「逻辑删除」按钮，数据从列表消失但数据库仍保留

---

### 6. 自动填充 — @TableField(fill = ...)

实现 `MetaObjectHandler`，在插入/更新时自动填充时间戳字段，无需在业务代码中手动赋值。

```java
// User.java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createdTime;       // 仅插入时填充

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedTime;       // 插入和更新都填充

// AutoFillHandler.java
@Override
public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
    this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
}
```

**演示页面：** 用户管理 → 新建用户后，表格中「创建时间」列自动显示

---

### 7. 乐观锁 — @Version

在字段上加 `@Version`，`updateById` 会自动在 `WHERE` 条件中加入版本校验，并在更新成功后自动将 version+1。并发冲突时抛出异常。

```java
// User.java / Order.java
@Version
private Integer version;
```

```sql
-- 执行 updateById(user) 实际生成（version 自动注入）：
UPDATE user SET username=?, version=2 WHERE id=1 AND version=1
-- 若 version 不匹配（已被其他请求修改），UPDATE 影响 0 行 → 抛出异常
```

**演示页面：**
- 用户管理 → 编辑弹窗底部显示「当前版本号 v?（提交后自动+1）」
- 订单管理 → 更新状态弹窗显示乐观锁提示

---

### 8. XML Mapper — 多表联查（一对多）

复杂的多表 JOIN 查询写在 XML 中，使用 `resultMap` + `collection` 实现一对多映射。

```xml
<!-- OrderMapper.xml -->
<resultMap id="OrderDetailWithItemsMap" type="OrderDetailDTO">
    <id property="id" column="order_id"/>
    <result property="username" column="username"/>    <!-- 来自 user 表 -->
    ...
    <!-- 一对多：order → order_item（含 product 字段） -->
    <collection property="items" ofType="OrderItemDTO">
        <id property="id" column="item_id"/>
        <result property="productCategory" column="product_category"/>  <!-- 来自 product 表 -->
        ...
    </collection>
</resultMap>

<select id="selectOrderDetail" resultMap="OrderDetailWithItemsMap">
    SELECT o.*, u.username, u.email,
           oi.id AS item_id, oi.quantity,
           p.category AS product_category
    FROM `order` o
    LEFT JOIN user u        ON o.user_id    = u.id AND u.deleted = 0
    LEFT JOIN order_item oi ON oi.order_id  = o.id
    LEFT JOIN product p     ON oi.product_id = p.id
    WHERE o.id = #{id}
</select>
```

**演示页面：** 订单管理 → 点击「查看详情」→ 展示四表联查结果及 SQL 源码

---

### 9. 动态 SQL — `<where>` / `<if>`

在 XML 中使用动态标签，根据参数是否为空决定是否追加查询条件，避免手拼 SQL 字符串。

```xml
<!-- OrderMapper.xml -->
<select id="selectOrderPage" resultMap="OrderDetailMap">
    SELECT o.id, u.username, o.total_amount, o.status ...
    FROM `order` o LEFT JOIN user u ON o.user_id = u.id
    <where>
        <if test="username != null and username != ''">
            AND u.username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="status != null">
            AND o.status = #{status}
        </if>
    </where>
    ORDER BY o.created_time DESC
</select>
```

**演示页面：** 订单管理 → 用户名 / 状态筛选

---

### 10. @Select 注解 SQL — 自定义查询

简单的自定义 SQL 可以直接写在 Mapper 注解上，无需 XML。

```java
// ProductMapper.java
@Select("SELECT category, COUNT(*) AS count, SUM(stock) AS totalStock, AVG(price) AS avgPrice " +
        "FROM product GROUP BY category ORDER BY count DESC")
List<CategoryStatsDTO> selectCategoryStats();
```

**演示页面：** 首页 → 「商品分类统计」表格 + 商品管理左侧统计面板

---

### 11. saveBatch — 批量插入

`saveBatch` 将多条记录分批次写入，减少数据库交互次数，性能远优于循环单条 insert。

```java
// UserServiceImpl.java
List<User> users = List.of(userA, userB, userC);
saveBatch(users);   // 默认每批 1000 条
```

**演示页面：** 用户管理 → 点击「批量插入演示」按钮，一次写入 3 条

---

## 项目结构

```
label-3959/
├── backend/                                   # Spring Boot 后端
│   ├── src/main/java/com/example/demo/
│   │   ├── config/
│   │   │   ├── MybatisPlusConfig.java         # 注册分页插件 + 乐观锁插件
│   │   │   ├── AutoFillHandler.java           # 自动填充处理器
│   │   │   ├── DataInitializer.java           # 应用启动时写入演示数据
│   │   │   └── GlobalExceptionHandler.java    # 全局异常处理
│   │   ├── entity/
│   │   │   ├── User.java                      # @TableLogic @Version @TableField
│   │   │   ├── Product.java
│   │   │   ├── Order.java                     # @Version 乐观锁
│   │   │   └── OrderItem.java
│   │   ├── mapper/
│   │   │   ├── UserMapper.java                # BaseMapper<User>
│   │   │   ├── ProductMapper.java             # @Select 注解 SQL
│   │   │   ├── OrderMapper.java               # XML 多表联查接口
│   │   │   └── OrderItemMapper.java
│   │   ├── service/
│   │   │   ├── impl/UserServiceImpl.java      # LambdaQueryWrapper + 分页 + 批量
│   │   │   ├── impl/ProductServiceImpl.java   # 分页 + 条件查询
│   │   │   └── impl/OrderServiceImpl.java     # 多表联查 + 事务 + 乐观锁
│   │   ├── controller/
│   │   │   ├── UserController.java
│   │   │   ├── ProductController.java
│   │   │   └── OrderController.java
│   │   ├── dto/
│   │   │   ├── OrderDetailDTO.java            # 多表联查结果 DTO（含内部类 OrderItemDTO）
│   │   │   └── CategoryStatsDTO.java          # 分类统计 DTO
│   │   └── common/
│   │       └── Result.java                    # 统一响应体
│   └── src/main/resources/
│       ├── application.yml                    # 数据源 + MyBatis Plus 配置
│       └── mapper/OrderMapper.xml             # 多表联查 XML + 动态 SQL
├── frontend/                                  # Vue 3 前端
│   ├── src/
│   │   ├── api/                               # Axios 封装
│   │   │   ├── index.js                       # 统一拦截器（错误 Toast）
│   │   │   ├── user.js
│   │   │   ├── product.js
│   │   │   └── order.js
│   │   ├── views/
│   │   │   ├── HomeView.vue                   # 首页：统计卡片 + 分类图表 + 特性清单
│   │   │   ├── UserView.vue                   # 用户管理：CRUD + 逻辑删除 + 乐观锁 + 批量
│   │   │   ├── ProductView.vue                # 商品管理：分页 + 分类过滤 + 统计
│   │   │   ├── OrderView.vue                  # 订单管理：联查列表 + 创建 + 状态更新
│   │   │   └── OrderDetailView.vue            # 订单详情：四表联查展示 + SQL 源码
│   │   ├── router/index.js
│   │   └── App.vue                            # 侧边栏布局
│   ├── Dockerfile
│   └── nginx.conf                             # 反向代理 /api → backend:8080
├── init.sql                                   # 数据库建表 DDL（纯 ASCII）
├── docker-compose.yml                         # 三服务编排
└── .gitignore
```

---

## 数据库设计

```
user                        product
─────────────────           ─────────────────────
id (PK, AUTO)               id (PK, AUTO)
username                    name
email                       price
age                         stock
status        ←── 0/1       category
deleted       ←── @TableLogic  description
version       ←── @Version  created_time  ←── 自动填充
created_time  ←── 自动填充   updated_time  ←── 自动填充
updated_time  ←── 自动填充

order                       order_item
─────────────────           ─────────────────────
id (PK, AUTO)               id (PK, AUTO)
user_id  ──────────────→ user.id
total_amount                order_id ─────────→ order.id
status        ←── 0~4       product_id ───────→ product.id
remark                      product_name  (快照)
version       ←── @Version  quantity
created_time  ←── 自动填充   price         (快照，下单时价格)
updated_time  ←── 自动填充
```

---

## 快速启动

### 前置条件

- Docker Desktop 已安装并运行

### 一键启动

```bash
git clone <repo-url>
cd label-3959
docker compose up --build
```

首次构建约需 **5~10 分钟**（Maven 下载依赖 + npm 安装），后续启动仅需 **30 秒**。

### 服务地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3959 | Vue 页面 |
| 后端 API | http://localhost:8959/api | Spring Boot REST |
| 数据库 | localhost:33959 | MySQL（user: root / pass: root123） |

---

## API 接口文档

### 用户接口 `/api/users`

| 方法 | 路径 | 说明 | 演示特性 |
|------|------|------|---------|
| GET | `/api/users/page` | 分页查询 | 分页插件 + LambdaQueryWrapper |
| GET | `/api/users` | 条件列表查询 | LambdaQueryWrapper |
| GET | `/api/users/{id}` | 根据 ID 查询 | BaseMapper.selectById |
| POST | `/api/users` | 创建用户 | 自动填充 createdTime/updatedTime |
| PUT | `/api/users/{id}` | 更新用户 | 乐观锁 @Version |
| DELETE | `/api/users/{id}` | 逻辑删除 | @TableLogic |
| POST | `/api/users/batch` | 批量创建 | saveBatch |

### 商品接口 `/api/products`

| 方法 | 路径 | 说明 | 演示特性 |
|------|------|------|---------|
| GET | `/api/products/page` | 分页查询（支持名称/分类过滤） | 分页插件 + 条件构造器 |
| GET | `/api/products` | 全量列表 | BaseMapper.selectList |
| GET | `/api/products/stats` | 分类统计 | @Select 注解 SQL + GROUP BY |
| POST | `/api/products` | 创建商品 | 自动填充 |
| PUT | `/api/products/{id}` | 更新商品 | 自动填充 updatedTime |
| DELETE | `/api/products/{id}` | 删除商品 | BaseMapper.deleteById |

### 订单接口 `/api/orders`

| 方法 | 路径 | 说明 | 演示特性 |
|------|------|------|---------|
| GET | `/api/orders/page` | 分页查询（联查用户信息） | XML 多表联查 + 动态 SQL |
| GET | `/api/orders/{id}` | 订单详情（含明细+商品） | XML 一对多 resultMap |
| POST | `/api/orders` | 创建订单 | @Transactional 事务 + 批量写明细 |
| PUT | `/api/orders/{id}/status` | 更新订单状态 | 乐观锁 @Version |

---

## 常见问题

**Q: 容器启动后前端页面空白或接口 502？**

后端启动需要几秒钟，等待 10~15 秒后刷新页面即可。

**Q: 数据库有数据但显示乱码？**

演示数据由 Spring Boot `DataInitializer` 在启动时通过 JDBC 写入，编码由 JVM 保证为 UTF-8，不会出现乱码。如遇问题请执行：
```bash
docker compose down -v
docker compose up --build
```

**Q: 如何验证乐观锁效果？**

在用户管理页面编辑同一条记录，提交后再次编辑（此时前端持有的是旧 version），会收到「乐观锁冲突」错误提示。

**Q: 如何查看 MyBatis 实际执行的 SQL？**

所有 SQL 打印在后端日志中：
```bash
docker compose logs backend
```
或 `application.yml` 中配置的 `StdOutImpl` 日志适配器会将 SQL 输出到 stdout。
