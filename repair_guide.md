# 修复指南

本文件记录项目中发现的所有问题及其修复方案，所有后续修复都必须在此文件中登记。

---

## 修复记录

### 修复 #1：管理员用户可以删除自己

**问题描述**：管理员用户在用户管理页面可以删除当前登录的自己，导致无法继续操作系统。

**发现日期**：2026-06-07

**问题根源**：
- 后端 `deleteUser` 方法未校验待删除用户 ID 是否为当前登录用户 ID
- 前端删除按钮未对当前登录用户做禁用处理

**影响范围**：用户管理模块 - 删除用户功能

**修复方案**：

#### 1. 后端修复
**文件**：[backend/src/main/java/com/example/demo/service/impl/UserServiceImpl.java](backend/src/main/java/com/example/demo/service/impl/UserServiceImpl.java#L94-L102)

**修复内容**：
在 `deleteUser` 方法中添加校验，禁止删除当前登录用户：
```java
@Override
public void deleteUser(Long id) {
    Long currentUserId = SecurityUtil.getCurrentUserId();
    if (currentUserId != null && currentUserId.equals(id)) {
        throw new IllegalArgumentException("不能删除当前登录用户");
    }
    removeById(id);
    log.info("逻辑删除用户: id={}", id);
}
```

**关键点**：
- 使用 `SecurityUtil.getCurrentUserId()` 获取当前登录用户 ID
- 与待删除用户 ID 比较，相同则抛出 `IllegalArgumentException`
- 全局异常处理器会捕获该异常并返回友好错误信息

#### 2. 前端修复
**文件**：[frontend/src/views/UserView.vue](frontend/src/views/UserView.vue#L88-L111)

**修复内容**：
对当前登录用户的删除按钮添加 `disabled` 属性和 tooltip 提示：
```vue
<el-button
  size="small"
  type="danger"
  link
  :disabled="row.id === authStore.userInfo?.id"
  @click="handleDelete(row)">
  <el-tooltip v-if="row.id === authStore.userInfo?.id" content="不能删除当前登录用户" placement="top">
    <span>逻辑删除</span>
  </el-tooltip>
  <template v-else>逻辑删除</template>
</el-button>
```

**关键点**：
- 使用 `authStore.userInfo?.id` 获取当前登录用户 ID
- 与行数据 `row.id` 比较，相同则禁用按钮
- 禁用时显示 tooltip 提示用户原因
- 前后端双重校验，确保安全性

**修复验证**：
1. 使用管理员账号登录
2. 在用户列表中找到当前登录用户
3. 确认"逻辑删除"按钮已禁用，鼠标悬停显示"不能删除当前登录用户"
4. 尝试通过 API 直接调用删除接口（如使用 Postman），应返回错误信息
5. 删除其他用户功能正常

---

### 修复 #2：商品页面无法查看评价

**问题描述**：用户在商品管理页面没有查看商品评价的入口，无法直接了解商品的用户评价情况。虽然有独立的"商品评价"页面，但在商品列表中无法快速查看某个商品的评价。

**发现日期**：2026-06-07

**问题根源**：
- 商品管理页面的操作列缺少"查看评价"按钮
- 没有针对单个商品的评价查看弹窗

**影响范围**：商品管理模块 - 商品列表功能

**修复方案**：

#### 1. 前端修复
**文件**：[frontend/src/views/ProductView.vue](frontend/src/views/ProductView.vue#L67-L78)

**修复内容**：

1. 在操作列增加"查看评价"按钮：
```vue
<el-button link type="warning" size="small" :icon="ChatDotRound" @click="openViewReviews(row)">
  查看评价
</el-button>
```

2. 添加评价弹窗，包含：
- 评价统计区域（平均分、总评价数、各评分分布进度条）
- 按评分筛选功能
- 评价列表表格（用户、评分、内容、评价时间）
- 分页功能

3. 添加相关方法：
```javascript
const openViewReviews = (row) => {
  currentProductId.value = row.id
  reviewProductName.value = row.name
  reviewQuery.current = 1
  reviewQuery.rating = null
  reviewStats.value = null
  reviewList.value = []
  reviewTotal.value = 0
  reviewDialogVisible.value = true
  loadProductReviews()
}

const loadProductReviews = async () => {
  const [reviewRes, statsRes] = await Promise.all([
    getReviewPage({ ...reviewQuery, productId: currentProductId.value }),
    getReviewStats(currentProductId.value)
  ])
  reviewList.value = reviewRes.data.records
  reviewTotal.value = reviewRes.data.total
  reviewStats.value = statsRes.data
}
```

4. 导入评价相关 API 和图标：
```javascript
import { ChatDotRound } from '@element-plus/icons-vue'
import { getReviewPage, getReviewStats } from '../api/review'
```

**关键点**：
- 操作列宽度从 220px 调整为 320px 以容纳新按钮
- 评价弹窗宽度 700px，顶部边距 5vh，适合展示较多内容
- 同时加载评价列表和统计数据，提升用户体验
- 支持按评分筛选评价，方便用户快速了解不同评分的评价内容

**修复验证**：
1. 登录系统，进入"商品管理"页面
2. 确认每个商品操作列都有"查看评价"按钮（黄色警告色）
3. 点击任意商品的"查看评价"按钮
4. 确认弹窗显示该商品的评价统计（平均分、各评分分布）
5. 确认评价列表显示正确，支持分页和按评分筛选
6. 对暂无评价的商品，显示"该商品暂无评价"提示
7. 关闭弹窗后再次点击其他商品，确认显示对应商品的评价

---

### 修复 #3：商品页面查看评价弹窗模板语法错误

**问题描述**：商品页面"查看评价"弹窗的标题属性中存在未闭合的模板表达式语法错误，会导致页面渲染失败。

**发现日期**：2026-06-07

**问题根源**：
- 第146行 `:title` 属性中的模板字符串（反引号）缺少结尾的反引号
- 原代码：`:title="\`「${reviewProductName}」的评价"` （缺少结尾的 `` ` ``）

**影响范围**：商品管理模块 - 查看评价弹窗功能

**修复方案**：

#### 1. 前端修复
**文件**：[frontend/src/views/ProductView.vue](frontend/src/views/ProductView.vue#L146-L146)

**修复内容**：

在模板字符串末尾添加缺失的反引号：
```vue
<!-- 修复前 -->
<el-dialog v-model="reviewDialogVisible" :title="`「${reviewProductName}」的评价" width="700px" top="5vh">

<!-- 修复后 -->
<el-dialog v-model="reviewDialogVisible" :title="`「${reviewProductName}」的评价`" width="700px" top="5vh">
```

**关键点**：
- JavaScript 模板字符串必须使用成对的反引号（`` ` ``）包裹
- 在 Vue 模板的属性绑定中使用模板字符串时，同样需要保证语法正确
- 缺少闭合反引号会导致 Vue 模板解析失败，整个组件无法正常渲染

**修复验证**：
1. 登录系统，进入"商品管理"页面
2. 点击任意商品的"查看评价"按钮
3. 确认弹窗正常打开，标题显示为「商品名称」的评价格式
4. 确认弹窗内的评价统计和列表正常显示
5. 浏览器控制台无 JavaScript 语法错误

---

### 修复 #4：UserMapper 中使用 List 类型未导入

**问题描述**：用户数据访问层接口 `UserMapper` 中新增的 `listDeletedUsers` 方法使用了 `List<User>` 作为返回值类型，但缺少 `java.util.List` 的导入语句，会导致编译失败。

**发现日期**：2026-06-07

**问题根源**：
- 在添加导出 CSV 功能时，为 `UserMapper` 新增了 `listDeletedUsers` 方法，返回类型为 `List<User>`
- 遗漏了 `import java.util.List;` 导入语句
- Java 编译器无法识别 `List` 类型，导致编译错误

**影响范围**：用户管理模块 - 数据访问层（导出 CSV 功能依赖此方法）

**修复方案**：

#### 1. 后端修复
**文件**：[backend/src/main/java/com/example/demo/mapper/UserMapper.java](backend/src/main/java/com/example/demo/mapper/UserMapper.java#L11-L11)

**修复内容**：
在文件的导入语句区域添加 `java.util.List` 的导入：
```java
import org.apache.ibatis.annotations.Update;

import java.util.List;
```

**关键点**：
- `List` 是 `java.util` 包下的接口，必须显式导入
- 导入语句应放置在其他 import 语句之后，类定义之前
- 按照 Java 编码规范，静态导入、第三方库导入、JDK 标准库导入应分组并按字母排序

**修复验证**：
1. 在后端项目根目录执行 `mvn compile`
2. 确认编译成功，无 `cannot find symbol` 错误
3. 确认 UserMapper 接口中的 `listDeletedUsers` 方法可以正常编译
4. 启动应用，测试用户导出 CSV 功能正常
5. 确认已删除用户列表查询功能正常

---

### 修复 #5：用户没有领取和查看优惠券的入口

**问题描述**：用户登录系统后，侧边栏菜单中没有显示「优惠券中心」和「我的优惠券」入口，导致用户无法领取和查看优惠券。虽然优惠券模块的前端页面、后端 API、路由配置都已完成，但缺少侧边栏菜单入口。

**发现日期**：2026-06-07

**问题根源**：
- 前端 `App.vue` 的侧边栏菜单中缺少优惠券相关的菜单项
- 虽然图标 `Present` 和 `Wallet` 已在第 103 行导入，但菜单区域（第 36-49 行）未添加对应的 `el-menu-item`
- 优惠券模块的其他部分（页面、路由、API、后端逻辑）均已正确实现

**影响范围**：优惠券模块 - 所有功能（用户无法访问优惠券页面）

**修复方案**：

#### 1. 前端修复
**文件**：[frontend/src/App.vue](frontend/src/App.vue#L39-L47)

**修复内容**：
在「商品评价」菜单项之后、「消息中心」菜单项之前添加两个菜单项：

```vue
<el-menu-item index="/coupons">
  <el-icon><Present /></el-icon>
  <span>优惠券中心</span>
</el-menu-item>
<el-menu-item index="/my-coupons">
  <el-icon><Wallet /></el-icon>
  <span>我的优惠券</span>
</el-menu-item>
```

**关键点**：
- 菜单项顺序：商品评价 → 优惠券中心 → 我的优惠券 → 消息中心
- 「优惠券中心」路由为 `/coupons`，用于展示可领取优惠券列表（管理员还可管理模板）
- 「我的优惠券」路由为 `/my-coupons`，用于展示用户已领取的优惠券
- 图标 `Present`（礼物）和 `Wallet`（钱包）已提前导入，无需重复导入
- 两个菜单项对所有登录用户可见，不做管理员权限限制（管理员在优惠券中心页面内有额外功能）

#### 2. 后端权限确认（无需修改）
**文件**：[backend/src/main/java/com/example/demo/config/SecurityConfig.java](backend/src/main/java/com/example/demo/config/SecurityConfig.java#L53-L54)

**权限配置确认**：
```java
.requestMatchers("/api/coupons/templates/**").hasRole(RoleEnum.ADMIN.getCode())
.requestMatchers("/api/coupons/**").authenticated()
```

- `/api/coupons/templates/**` 路径（券模板管理）需要管理员角色 ✅
- `/api/coupons/**` 其他路径（领取、查询等）只需要登录认证 ✅
- 权限配置符合设计要求，无需修改

**修复验证**：
1. 使用普通用户账号登录系统
2. 确认左侧边栏菜单中显示「优惠券中心」和「我的优惠券」菜单项（位于商品评价和消息中心之间）
3. 点击「优惠券中心」，确认页面正常加载，显示可领取优惠券列表
4. 点击「我的优惠券」，确认页面正常加载，显示用户已领取的优惠券
5. 使用管理员账号登录系统
6. 进入「优惠券中心」，确认显示「创建券模板」按钮和模板管理功能
7. 普通用户进入「优惠券中心」，确认只显示可领取优惠券列表，不显示模板管理功能
8. 两个菜单项图标正确显示（礼物图标和钱包图标）
9. 点击菜单项后路由正确跳转，页面标题正确显示

---

### 修复 #6：优惠券页面 API 请求路径重复（api/api/...）

**问题描述**：用户进入「优惠券中心」和「我的优惠券」页面时，浏览器控制台报错 `No static resource api/api/coupons/...`，页面无法正常加载数据。错误信息显示请求路径中出现了重复的 `api` 前缀。

**发现日期**：2026-06-07

**问题根源**：
- 前端 API 配置文件 `index.js` 中已设置 `baseURL: '/api'`，会自动为所有请求添加 `/api` 前缀
- 但 `coupon.js` 中所有 API 方法的 `url` 参数又额外添加了 `/api/` 前缀（如 `/api/coupons/templates/available`）
- 导致实际请求路径变为 `/api/api/coupons/...`，与后端接口路径 `/api/coupons/...` 不匹配
- 其他 API 文件（`product.js`、`order.js`、`user.js` 等）的 URL 都是正确的，没有多余的 `/api/` 前缀

**影响范围**：优惠券模块 - 所有页面（优惠券中心、我的优惠券）

**修复方案**：

#### 1. 前端修复
**文件**：[frontend/src/api/coupon.js](frontend/src/api/coupon.js)

**修复内容**：
将 `coupon.js` 中所有 API 方法的 `url` 参数去掉 `/api` 前缀，共涉及 10 处修改：

| 方法 | 修复前 | 修复后 |
|------|--------|--------|
| createCouponTemplate | `/api/coupons/templates` | `/coupons/templates` |
| getCouponTemplates | `/api/coupons/templates/page` | `/coupons/templates/page` |
| getAvailableTemplates | `/api/coupons/templates/available` | `/coupons/templates/available` |
| updateTemplateStatus | `/api/coupons/templates/${id}/status` | `/coupons/templates/${id}/status` |
| receiveCoupon | `/api/coupons/templates/${templateId}/receive` | `/coupons/templates/${templateId}/receive` |
| getMyCoupons | `/api/coupons/my` | `/coupons/my` |
| getMyCouponDetail | `/api/coupons/my/${id}` | `/coupons/my/${id}` |
| getAvailableCouponsForOrder | `/api/coupons/my/available-for-order` | `/coupons/my/available-for-order` |
| calculateDiscount | `/api/coupons/${userCouponId}/calculate` | `/coupons/${userCouponId}/calculate` |

**修改示例**：
```javascript
// 修复前
export const getAvailableTemplates = () => {
  return request({
    url: '/api/coupons/templates/available',
    method: 'get'
  })
}

// 修复后
export const getAvailableTemplates = () => {
  return request({
    url: '/coupons/templates/available',
    method: 'get'
  })
}
```

**关键点**：
- `axios.create({ baseURL: '/api' })` 会自动为所有请求 URL 添加 `/api` 前缀
- API 文件中的 URL 应该是相对路径，不包含 `/api` 前缀
- 修复后实际请求路径为 `/api` + `/coupons/...` = `/api/coupons/...`，与后端接口匹配
- 必须修改所有 10 个 API 方法的 URL，不能遗漏

**修复验证**：
1. 启动前端和后端服务
2. 使用普通用户账号登录系统
3. 点击左侧菜单「优惠券中心」
4. 确认页面正常加载，显示可领取优惠券列表，无控制台错误
5. 点击「我的优惠券」
6. 确认页面正常加载，显示用户优惠券列表，无控制台错误
7. 打开浏览器开发者工具（F12）→ Network 面板
8. 刷新优惠券中心页面，确认请求 URL 为 `/api/coupons/templates/available`（正确）
9. 确认不是 `/api/api/coupons/templates/available`（错误）
10. 测试优惠券领取功能，确认请求正常发送
11. 测试我的优惠券筛选功能，确认请求 URL 正确
12. 确认其他模块（商品、订单、用户等）的 API 请求不受影响

---

### 修复 #7：普通用户访问优惠券中心提示权限不足

**问题描述**：普通用户登录后，点击左侧菜单「优惠券中心」时，弹出错误提示「权限不足，无法访问」，无法查看可领取优惠券列表。

**发现日期**：2026-06-07

**问题根源**：
- Spring Security 配置中，`/api/coupons/templates/**` 路径被设置为需要管理员角色（`hasRole('ADMIN')`）
- 但普通用户访问优惠券中心页面时，需要调用 `/api/coupons/templates/available` 接口获取可领取优惠券列表
- 该接口路径匹配到了 `/api/coupons/templates/**` 规则，导致普通用户被拒绝访问
- 管理员相关的模板管理接口（如创建、修改状态等）才需要管理员权限，而查询可领取列表是所有登录用户都应该能访问的

**影响范围**：优惠券模块 - 优惠券中心页面（普通用户无法访问）

**修复方案**：

#### 1. 后端修复
**文件**：[backend/src/main/java/com/example/demo/config/SecurityConfig.java](backend/src/main/java/com/example/demo/config/SecurityConfig.java#L53-L54)

**修复内容**：
在 `/api/coupons/templates/**` 规则之前，添加更具体的路径规则 `/api/coupons/templates/available`，设置为只需要登录认证即可访问：

```java
// 修复前
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/users/**").hasRole(RoleEnum.ADMIN.getCode())
    .requestMatchers("/api/coupons/templates/**").hasRole(RoleEnum.ADMIN.getCode())
    .requestMatchers("/api/coupons/**").authenticated()
    ...
)

// 修复后
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/users/**").hasRole(RoleEnum.ADMIN.getCode())
    .requestMatchers("/api/coupons/templates/available").authenticated()
    .requestMatchers("/api/coupons/templates/**").hasRole(RoleEnum.ADMIN.getCode())
    .requestMatchers("/api/coupons/**").authenticated()
    ...
)
```

**关键点**：
- Spring Security 的权限规则是按顺序匹配的，更具体的路径必须放在更通用的路径之前
- `/api/coupons/templates/available` 比 `/api/coupons/templates/**` 更具体，必须放在前面
- 这样普通用户调用 `/available` 接口时匹配第一条规则，认证通过即可访问
- 管理员调用其他 `/templates/**` 接口（如 `/page`、`/{id}/status` 等）时匹配第二条规则，需要管理员角色
- 其他 `/coupons/**` 接口（如领取、查询我的优惠券等）匹配第三条规则，只需要登录认证

**修复验证**：
1. 启动后端和前端服务
2. 使用普通用户账号（非管理员）登录系统
3. 点击左侧菜单「优惠券中心」
4. 确认页面正常加载，不弹出「权限不足」错误
5. 确认页面显示可领取优惠券列表
6. 使用浏览器开发者工具（F12）→ Network 面板
7. 确认 `/api/coupons/templates/available` 接口返回 200 状态码，不是 403
8. 使用管理员账号登录系统
9. 进入「优惠券中心」，确认显示「创建券模板」按钮和模板管理功能
10. 确认管理员调用 `/api/coupons/templates/page` 接口正常，返回 200 状态码

---

### 修复 #8：购物车结算页面没有优惠券选项

**问题描述**：用户在购物车页面选择商品后点击「结算」按钮，弹出的确认订单弹窗中没有优惠券选择区域，无法在结算时使用已领取的优惠券抵扣金额。

**发现日期**：2026-06-07

**问题根源**：
- 前端购物车结算弹窗（`CartView.vue`）缺少优惠券选择 UI 组件
- 前端 API 层缺少根据订单金额查询可用优惠券的方法
- 后端 `CartController` 的 `CheckoutRequest` 缺少 `userCouponId` 字段
- 后端 `CartService` 的 `checkout` 方法缺少 `userCouponId` 参数，无法将优惠券传递给订单创建流程

**影响范围**：购物车模块 - 结算功能（无法使用优惠券抵扣）

**修复方案**：

#### 1. 后端修复

**文件 1**：[backend/src/main/java/com/example/demo/controller/CartController.java](backend/src/main/java/com/example/demo/controller/CartController.java#L52-L78)

**修复内容**：
在 `CheckoutRequest` 中添加 `userCouponId` 字段，并在 `checkout` 方法中传递给 Service：

```java
@PostMapping("/checkout")
public Result<Order> checkout(@RequestBody CheckoutRequest req) {
    return Result.ok(cartService.checkout(req.getCartIds(), req.getRemark(), req.getUserCouponId()));
}

@Data
public static class CheckoutRequest {
    private List<Long> cartIds;
    private String remark;
    private Long userCouponId;  // 新增
}
```

**文件 2**：[backend/src/main/java/com/example/demo/service/CartService.java](backend/src/main/java/com/example/demo/service/CartService.java#L22-L22)

**修复内容**：
在 `checkout` 方法签名中添加 `userCouponId` 参数：

```java
// 修复前
Order checkout(List<Long> cartIds, String remark);

// 修复后
Order checkout(List<Long> cartIds, String remark, Long userCouponId);
```

**文件 3**：[backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java](backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java#L126-L173)

**修复内容**：
更新 `checkout` 方法签名，接收 `userCouponId` 并传递给 `createOrder` 方法：

```java
// 修复前
public Order checkout(List<Long> cartIds, String remark) {
    ...
    Order createdOrder = orderService.createOrder(order, orderItems, null);
    ...
}

// 修复后
public Order checkout(List<Long> cartIds, String remark, Long userCouponId) {
    ...
    Order createdOrder = orderService.createOrder(order, orderItems, userCouponId);
    ...
}
```

#### 2. 前端修复

**文件 1**：[frontend/src/api/cart.js](frontend/src/api/cart.js#L9-L9)

**修复内容**：
添加根据订单金额查询可用优惠券的 API 方法：

```javascript
export const getAvailableCouponsForOrder = (orderAmount) => 
  request.get('/coupons/my/available-for-order', { params: { orderAmount } })
```

**文件 2**：[frontend/src/views/CartView.vue](frontend/src/views/CartView.vue)

**修复内容**：

1. **导入新增 API 和图标**（第 150-158 行、第 209 行）：
```javascript
import { Plus, Minus, Delete, CircleCheck } from '@element-plus/icons-vue'
import {
  getMyCart,
  updateCartQuantity,
  removeFromCart,
  batchRemoveCart,
  checkoutCart,
  getAvailableCouponsForOrder
} from '../api/cart'
```

2. **新增响应式变量**（第 167-169 行）：
```javascript
const availableCoupons = ref([])
const selectedCouponId = ref(null)
const loadingCoupons = ref(false)
```

3. **新增计算属性**（第 190-205 行）：
```javascript
const selectedCoupon = computed(() => {
  return availableCoupons.value.find(c => c.id === selectedCouponId.value)
})

const discountAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  if (selectedCoupon.value.couponType === 1) {
    return Number(selectedCoupon.value.discountAmount) || 0
  } else {
    return selectedTotal.value * (1 - Number(selectedCoupon.value.discountRate))
  }
})

const finalAmount = computed(() => {
  return Math.max(0, selectedTotal.value - discountAmount.value)
})
```

4. **修改 `handleCheckout` 方法**（第 264-288 行）：
```javascript
const handleCheckout = async () => {
  // ... 原有校验逻辑 ...
  checkoutRemark.value = ''
  selectedCouponId.value = null
  availableCoupons.value = []
  
  loadingCoupons.value = true
  try {
    const res = await getAvailableCouponsForOrder(selectedTotal.value)
    availableCoupons.value = res.data
  } finally {
    loadingCoupons.value = false
  }
  
  checkoutDialogVisible.value = true
}
```

5. **修改 `confirmCheckout` 方法**（第 291-305 行）：
```javascript
const confirmCheckout = async () => {
  checkoutLoading.value = true
  try {
    const res = await checkoutCart({
      cartIds: selectedIds.value,
      remark: checkoutRemark.value,
      userCouponId: selectedCouponId.value  // 新增
    })
    // ...
  }
}
```

6. **新增 `formatDate` 工具方法**（第 370-373 行）：
```javascript
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}
```

7. **修改结算弹窗模板**（第 116-201 行）：
   - 弹窗宽度从 500px 改为 560px
   - 新增优惠券选择区域，包含加载状态、空状态、优惠券列表
   - 新增价格汇总区域，显示商品总额、优惠券抵扣、实付金额
   - 优惠券卡片支持点击选中/取消，选中状态有绿色边框和勾选图标

8. **新增优惠券选择区域样式**（第 506-639 行）：
   - `.coupon-section` - 优惠券区域容器
   - `.coupon-item` - 优惠券卡片（含选中状态样式）
   - `.coupon-item-left` - 左侧金额区域
   - `.coupon-item-right` - 右侧名称和有效期
   - `.coupon-check` - 选中图标定位
   - `.price-summary` - 价格汇总区域（黄色背景）
   - `.price-row` - 每行价格（含折扣行和最终行特殊样式）

**关键点**：
- 结算弹窗打开时，自动根据当前选中商品总金额查询可用优惠券
- 只显示满足满减门槛的优惠券（后端已筛选）
- 优惠券卡片显示类型、金额/折扣、满减条件、名称、有效期
- 点击优惠券卡片可选中/取消选中，选中时边框变绿并显示勾选图标
- 价格汇总区域实时计算并显示优惠抵扣金额和实付金额
- 满减券直接显示减免金额，折扣券实时计算折扣金额
- 提交订单时将选中的优惠券 ID 传递给后端
- 后端接收到 `userCouponId` 后传递给 `createOrder` 进行优惠券核销

**修复验证**：
1. 启动后端和前端服务
2. 使用普通用户账号登录
3. 进入「优惠券中心」，领取至少一张优惠券
4. 进入「购物车」页面，添加商品到购物车并选中
5. 点击「结算」按钮
6. 确认弹窗加载完成后，显示「选择优惠券」区域
7. 确认列出了可用的优惠券（满足满减门槛的）
8. 点击一张优惠券，确认卡片变为绿色边框并显示勾选图标
9. 确认价格汇总区域显示「优惠券抵扣：-¥xx」和正确的实付金额
10. 再次点击已选中的优惠券，确认取消选中，价格汇总恢复原价
11. 重新选择一张优惠券，点击「提交订单」
12. 进入订单详情页，确认订单使用了优惠券并显示优惠金额
13. 进入「我的优惠券」页面，确认该优惠券状态变为「已使用」
14. 取消该订单，确认优惠券状态恢复为「未使用」
15. 重复步骤 4-12，不选择优惠券直接提交订单
16. 确认订单没有使用优惠券，金额为原价

---

### 修复 #9：后端编译失败 - 重复 @Override 注解和 Lombok getter/setter 找不到

**问题描述**：Docker 构建或 Maven 编译时出现大量编译错误：
1. `CartServiceImpl.java:[128,1] java.lang.Override is not a repeatable annotation type`
2. `CustomUserDetailsService.java`、`UserServiceImpl.java`、`ProductController.java` 中找不到 User 和 Product 实体类的 getter/setter 方法（如 `getUsername()`、`getStatus()`、`setId()` 等）

**发现日期**：2026-06-07

**问题根源**：
1. **重复 @Override 注解**：在 `CartServiceImpl.java` 的 `checkout` 方法上，有两个 `@Override` 注解（第 126 行和第 128 行），而 `@Override` 不是可重复注解
2. **Lombok 注解处理器未正确配置**：Spring Boot 3.2.0 中，maven-compiler-plugin 需要显式配置 Lombok 注解处理器路径，否则 `@Data` 注解不会生成 getter/setter 方法，导致所有依赖这些方法的代码编译失败

**影响范围**：后端全模块（编译失败，无法启动）

**修复方案**：

#### 1. 修复重复 @Override 注解

**文件**：[backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java](backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java#L126-L128)

**修复内容**：
删除第 128 行重复的 `@Override` 注解：

```java
// 修复前
    @Override
    @Transactional(rollbackFor = Exception.class)
@Override
    public Order checkout(List<Long> cartIds, String remark, Long userCouponId) {

// 修复后
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(List<Long> cartIds, String remark, Long userCouponId) {
```

#### 2. 修复 Lombok 注解处理器配置

**文件**：[backend/pom.xml](backend/pom.xml#L19-L23)、[backend/pom.xml](backend/pom.xml#L91-L120)

**修复内容 1 - 添加 lombok.version 属性**（在 properties 中）：
```xml
<!-- 修复前 -->
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
</properties>

<!-- 修复后 -->
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

**修复内容 2 - 添加 maven-compiler-plugin 配置**（在 build/plugins 中）：
```xml
<!-- 修复后新增 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**关键点**：
- Spring Boot 3.x 中，Lombok 需要显式配置为注解处理器，而不仅仅是依赖
- `maven-compiler-plugin` 的 `annotationProcessorPaths` 配置告诉编译器在编译时使用 Lombok 注解处理器
- `@Data` 注解会在编译期生成 getter、setter、toString、equals、hashCode 等方法
- 如果注解处理器未正确配置，这些方法在编译时不存在，导致所有调用这些方法的代码报错

**修复验证**：
1. 在后端根目录执行 `mvn clean compile`
2. 确认编译成功，输出 `BUILD SUCCESS`
3. 确认没有任何错误或警告（警告可以有，但不能有错误）
4. 执行 `mvn clean package -DskipTests` 确认可以正常打包
5. 如果是 Docker 构建，重新执行 `docker build` 确认镜像构建成功
6. 启动应用，确认可以正常启动，没有 ClassNotFoundException 或 NoSuchMethodError

---

### 修复 #10：左侧菜单栏没有收货地址管理入口

**问题描述**：收货地址模块的前后端功能、路由配置、页面组件均已完成实现，但用户登录后在左侧菜单栏中找不到「收货地址」菜单项，无法通过侧边栏进入地址管理页面。虽然用户头像下拉菜单中有「收货地址」入口，但侧边栏缺失会影响功能发现性和使用便捷性。

**发现日期**：2026-06-07

**问题根源**：
- 前端 `App.vue` 的左侧菜单栏（第 16-58 行）中缺少「收货地址」对应的 `el-menu-item`
- 虽然第 111 行已导入 `Location` 图标，第 93 行的下拉菜单也有「收货地址」入口，但侧边栏菜单区域未添加对应菜单项
- 路由配置 `/addresses` 已正确添加，地址页面 `AddressView.vue` 也已创建
- 后端 API 接口、权限控制、用户权限隔离均已正确实现

**影响范围**：收货地址模块 - 功能入口（用户难以发现和访问地址管理功能）

**修复方案**：

#### 1. 前端修复
**文件**：[frontend/src/App.vue](frontend/src/App.vue#L48-L51)

**修复内容**：
在「我的优惠券」菜单项之后、「消息中心」菜单项之前添加「收货地址」菜单项：

```vue
<el-menu-item index="/my-coupons">
  <el-icon><Wallet /></el-icon>
  <span>我的优惠券</span>
</el-menu-item>
<el-menu-item index="/addresses">
  <el-icon><Location /></el-icon>
  <span>收货地址</span>
</el-menu-item>
<el-menu-item index="/notifications">
  <el-icon><Bell /></el-icon>
  <span>消息中心</span>
</el-menu-item>
```

**关键点**：
- 菜单项顺序：我的优惠券 → 收货地址 → 消息中心
- 「收货地址」路由为 `/addresses`，与路由配置一致
- 图标使用已导入的 `Location`（位置图标），语义明确
- 菜单项对所有登录用户可见（`/api/addresses/**` 在 SecurityConfig 中已配置为需要登录认证即可访问）
- 与用户头像下拉菜单中的「收货地址」入口保持功能一致

#### 2. 其他确认（无需修改）

**路由配置确认**：[frontend/src/router/index.js](frontend/src/router/index.js#L28-L28)
```javascript
{ path: '/addresses', component: AddressView, meta: { title: '收货地址', requiresAuth: true } }
```
- 路由已正确配置 ✅

**后端权限配置确认**：[backend/src/main/java/com/example/demo/config/SecurityConfig.java](backend/src/main/java/com/example/demo/config/SecurityConfig.java)
- `/api/addresses/**` 路径已被 `anyRequest().authenticated()` 覆盖，需要登录认证 ✅
- 后端 Service 层通过 `SecurityUtil.getCurrentUserId()` 确保用户只能操作自己的地址 ✅

**页面组件确认**：[frontend/src/views/AddressView.vue](frontend/src/views/AddressView.vue)
- 地址管理页面已完整实现，包含地址列表、新增/编辑、设置默认、删除等功能 ✅

**下拉菜单入口确认**：[frontend/src/App.vue](frontend/src/App.vue#L93-L93)
- 用户头像下拉菜单中的「收货地址」入口已正确配置，点击可跳转至 `/addresses` ✅

**修复验证**：
1. 启动前端和后端服务
2. 使用普通用户账号登录系统
3. 确认左侧边栏菜单中显示「收货地址」菜单项（位于「我的优惠券」和「消息中心」之间）
4. 确认菜单项图标正确显示（位置图标）
5. 点击「收货地址」菜单项，确认路由正确跳转至 `/addresses`，页面标题显示为「收货地址」
6. 确认地址管理页面正常加载，显示用户的地址列表（如果有）或空状态
7. 测试地址的新增、编辑、设置默认、删除功能，确认全部正常
8. 点击用户头像，在下拉菜单中点击「收货地址」，确认同样可以正确跳转
9. 使用管理员账号登录系统，重复步骤 3-7，确认管理员也可以正常访问和管理自己的地址
10. 测试用户权限隔离：使用用户 A 创建一个地址，然后使用用户 B 登录，确认无法看到或操作用户 A 的地址
11. 确认其他菜单项的顺序和功能不受影响

---

### 修复 #11：添加用户时密码字段缺失报错 + 邮箱格式无校验

**问题描述**：管理员在添加用户时，填入不规范的邮箱（如 `invalid-email`），点击确定时页面报错：
```
### Error updating database. Cause: java.sql.SQLException: Field 'password' doesn't have a default value
### SQL: INSERT INTO user ( username, email, age, role, status, deleted, version, created_time, updated_time ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? )
```
同时，系统未对邮箱格式进行校验，允许填入不符合规范的邮箱地址。

**发现日期**：2026-06-07

**问题根源**：
1. **密码字段缺失**：数据库 `user` 表的 `password` 字段定义为 `NOT NULL` 且没有默认值，但前端新增用户表单中缺少密码输入字段，导致插入时 `password` 为 `null`，触发数据库约束错误
2. **邮箱格式无校验**：前后端均未对邮箱格式进行正则校验，导致不规范的邮箱可以提交
3. **后端校验不完整**：虽然前端没有密码字段，但后端也未对新增用户时的密码必填性进行校验

**影响范围**：用户管理模块 - 新增/编辑用户功能

**修复方案**：

#### 1. 后端修复

**文件 1**：[backend/src/main/java/com/example/demo/service/impl/UserServiceImpl.java](backend/src/main/java/com/example/demo/service/impl/UserServiceImpl.java#L57-L108)

**修复内容 1 - 重命名并增强校验方法**：
将 `validateUnique` 方法重命名为 `validateUser`，增加邮箱格式校验和密码必填校验：

```java
// 修复前
private void validateUnique(User user, Long excludeId) {
    // 仅校验用户名和邮箱唯一性
}

// 修复后
private void validateUser(User user, Long excludeId, boolean isCreate) {
    Map<String, String> errors = new HashMap<>();

    // 用户名唯一性校验（原有逻辑）
    if (StringUtils.hasText(user.getUsername())) {
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())
                .ne(excludeId != null, User::getId, excludeId);
        if (count(usernameWrapper) > 0) {
            errors.put("username", "用户名已存在");
        }
    }

    // 邮箱格式校验 + 唯一性校验（新增）
    if (StringUtils.hasText(user.getEmail())) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!user.getEmail().matches(emailRegex)) {
            errors.put("email", "请输入正确的邮箱格式");
        } else {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, user.getEmail())
                    .ne(excludeId != null, User::getId, excludeId);
            if (count(emailWrapper) > 0) {
                errors.put("email", "邮箱已存在");
            }
        }
    }

    // 新增用户时密码必填校验（新增）
    if (isCreate && !StringUtils.hasText(user.getPassword())) {
        errors.put("password", "请输入密码");
    }

    if (!errors.isEmpty()) {
        throw new ValidationException(errors);
    }
}
```

**修复内容 2 - 更新调用方**：
```java
// createUser 方法
validateUser(user, null, true);  // true 表示新增，需要密码必填校验

// updateUser 方法
validateUser(user, user.getId(), false);  // false 表示编辑，密码可选
```

**关键点**：
- 邮箱正则表达式 `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` 可校验大部分常见邮箱格式
- 先校验格式，格式通过后再校验唯一性，避免无效的数据库查询
- 新增用户时（`isCreate=true`）密码必填，编辑用户时密码可选（留空表示不修改）
- 所有校验错误通过 `ValidationException` 抛出，包含字段级错误信息，前端可在对应字段旁显示

#### 2. 前端修复

**文件 1**：[frontend/src/views/UserView.vue](frontend/src/views/UserView.vue#L130-L141)

**修复内容 1 - 新增密码输入字段**：

```vue
<!-- 新增用户时 - 密码必填 -->
<el-form-item v-if="!isEdit" label="密码" prop="password" 
  :rules="[{ required: true, message: '请输入密码' }]" 
  :error="formErrors.password">
  <el-input v-model="form.password" type="password" show-password 
    placeholder="请输入密码" @input="clearFieldError('password')" />
</el-form-item>

<!-- 编辑用户时 - 密码可选 -->
<el-form-item v-if="isEdit" label="密码" prop="password" 
  :error="formErrors.password">
  <el-input v-model="form.password" type="password" show-password 
    placeholder="不修改请留空" @input="clearFieldError('password')" />
</el-form-item>
```

**修复内容 2 - 新增邮箱格式校验规则**：
```javascript
const emailRules = [
  { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
]
```

**修复内容 3 - 邮箱字段绑定校验规则**：
```vue
<el-form-item label="邮箱" prop="email" :rules="emailRules" :error="formErrors.email">
  <el-input v-model="form.email" @input="clearFieldError('email')" placeholder="请输入邮箱" />
</el-form-item>
```

**修复内容 4 - 添加密码字段错误处理**：
```javascript
// formErrors 新增 password 字段
const formErrors = reactive({ username: '', email: '', password: '' })

// clearFormErrors 清除 password 错误
const clearFormErrors = () => {
  formErrors.username = ''
  formErrors.email = ''
  formErrors.password = ''
}

// handleSubmit 处理 password 错误
} catch (err) {
  if (err.errors) {
    if (err.errors.password) {
      formErrors.password = err.errors.password
    }
    // ...
  }
}
```

**修复内容 5 - 表单数据初始化包含 password**：
```javascript
const form = reactive({ 
  id: null, username: '', email: '', password: '', 
  age: 18, status: 1, role: 'USER', version: null 
})

// openCreate 初始化密码为空
Object.assign(form, { id: null, username: '', email: '', password: '', ... })

// openEdit 编辑时清空密码字段（不展示原有密码）
Object.assign(form, { ...row, password: '' })
```

**关键点**：
- 密码输入框使用 `type="password"` 和 `show-password` 属性，支持密码显示/隐藏
- 新增用户时密码字段必填（带 `required` 规则），编辑用户时密码字段可选
- 编辑用户时密码字段默认清空，不显示用户原有密码（安全考虑）
- 邮箱使用 Element Plus 内置的 `type: 'email'` 校验规则，在 `blur` 时触发
- 前端校验 + 后端校验双重保障，即使绕过前端校验，后端也会拦截
- 所有字段错误都通过 `formErrors` 在对应字段旁显示，用户输入时自动清除

**修复验证**：
1. 启动后端和前端服务
2. 使用管理员账号登录系统，进入「用户管理」页面
3. 点击「新增用户」按钮
4. **验证密码必填**：不填写密码，直接点击「确定」，确认密码字段下方显示「请输入密码」
5. **验证邮箱格式**：输入不规范邮箱（如 `invalid-email`、`test@`、`test@.com`），点击「确定」或输入框失焦，确认邮箱字段下方显示「请输入正确的邮箱格式」
6. **验证正常提交**：填写正确的用户名、邮箱（如 `test@example.com`）、密码、年龄等信息，点击「确定」，确认创建成功
7. **验证重复用户名**：再次使用相同用户名创建用户，确认用户名字段下方显示「用户名已存在」
8. **验证重复邮箱**：使用相同邮箱创建用户，确认邮箱字段下方显示「邮箱已存在」
9. **验证编辑用户**：点击「编辑」按钮，确认密码字段显示「不修改请留空」
10. **验证编辑时密码可选**：不修改密码，直接修改其他字段（如年龄），点击「确定」，确认更新成功，密码保持不变
11. **验证编辑时修改密码**：在编辑时输入新密码，点击「确定」，确认更新成功，使用新密码可登录
12. **验证编辑时邮箱格式校验**：在编辑时将邮箱改为不规范格式，确认显示「请输入正确的邮箱格式」
13. **验证其他已填内容不被覆盖**：故意触发邮箱格式错误，确认用户名、年龄等其他已填内容保持不变
14. **验证错误自动清除**：在出错字段重新输入时，确认错误提示自动消失
15. 执行 `mvn compile` 确认后端编译成功，无任何错误

---

### 修复 #12：添加用户时填写密码仍提示"请输入密码"

**问题描述**：管理员在新增用户时，明明填写了密码字段，但点击确定后仍然提示"请输入密码"，表单无法提交。

**发现日期**：2026-06-07

**问题根源**：
`User` 实体类的 `password` 字段使用了 `@JsonIgnore` 注解，该注解会**同时影响序列化和反序列化**：
- **序列化**（Java 对象 → JSON，返回给前端）：password 字段被忽略 ✅（这是期望的安全行为）
- **反序列化**（JSON → Java 对象，接收前端数据）：password 字段也被忽略 ❌（这是问题所在）

导致即使前端正确填写了密码并发送给后端，Jackson 在反序列化时会忽略 password 字段，后端 User 对象中的 password 始终为 `null`，从而触发密码必填校验错误。

**影响范围**：用户管理模块 - 新增用户功能（密码无法正确传递到后端）

**修复方案**：

#### 1. 后端修复

**文件**：[backend/src/main/java/com/example/demo/entity/User.java](backend/src/main/java/com/example/demo/entity/User.java#L25-L27)

**修复内容**：
将 `@JsonIgnore` 替换为 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`：

```java
// 修复前
/** 密码（加密存储） */
@com.fasterxml.jackson.annotation.JsonIgnore
private String password;

// 修复后
/** 密码（加密存储） */
@com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
private String password;
```

**关键点**：
- `@JsonIgnore`：完全忽略字段，序列化和反序列化都不处理
- `@JsonProperty(access = WRITE_ONLY)`：只写模式，**反序列化时接收（前端→后端），序列化时忽略（后端→前端）**
- 这样既保证了安全性（密码不会返回给前端），又能正常接收前端传来的密码
- Jackson 的 `JsonProperty.Access` 枚举有三个值：
  - `READ_ONLY`：只读，序列化时输出，反序列化时忽略
  - `WRITE_ONLY`：只写，反序列化时接收，序列化时忽略 ← 我们需要的
  - `READ_WRITE`：读写都处理（默认行为）

**修复验证**：
1. 启动后端和前端服务
2. 使用管理员账号登录系统，进入「用户管理」页面
3. 点击「新增用户」按钮
4. 填写用户名、邮箱（格式正确）、密码（如 `123456`）、年龄等信息
5. 点击「确定」，确认创建成功，不再提示"请输入密码"
6. 使用新创建的账号和密码登录系统，确认可以正常登录
7. 测试编辑用户时修改密码：进入编辑页面，输入新密码，点击确定，确认更新成功
8. 验证密码不返回给前端：在浏览器开发者工具中查看用户列表接口的响应数据，确认不包含 password 字段
9. 执行 `mvn compile` 确认后端编译成功，无任何错误

---

### 修复 #13：下单时错误扣减总库存

**问题描述**：用户下单（购物车结算）后，商品的总库存（`stock` 字段）立即被扣减。正确的逻辑应该是：下单时只预占库存（增加 `reserved_stock`，不扣减 `stock`），确认收货后才正式扣减总库存。

**发现日期**：2026-06-07

**问题根源**：
1. **核心问题**：`CartServiceImpl.checkout()` 方法中（原第 160-162 行），在遍历购物车项创建订单项时，直接通过 `product.setStock(product.getStock() - item.getQuantity())` 扣减了总库存 `stock`。
2. **并发安全问题**：这段扣减库存的代码采用"先查询、再修改、后更新"的非原子操作，在高并发场景下可能导致超卖。
3. **库存校验错误**：购物车添加商品、修改数量、结算时的库存校验都使用了总库存 `stock`，而不是可用库存（`stock - reserved_stock`），导致在有商品被预占时，库存判断不准确。
4. **数据不完整**：`CartItemDTO` 和 `CartMapper.xml` 缺少 `reserved_stock` 字段，前端无法计算并展示可用库存。

**影响范围**：购物车模块 - 结算功能；库存预占模块 - 核心机制

**修复方案**：

#### 1. 后端修复 - CartServiceImpl（核心修复）

**文件**：[backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java](backend/src/main/java/com/example/demo/service/impl/CartServiceImpl.java#L40-L162)

**修复内容 1 - 删除直接扣减总库存的错误代码**：
删除 `checkout` 方法中直接修改 `product.stock` 的代码（原第 160-162 行）：
```java
// 修复前 - 错误代码
Product product = productMapper.selectById(item.getProductId());
product.setStock(product.getStock() - item.getQuantity());
productMapper.updateById(product);

// 修复后 - 已删除，库存预占逻辑移至 createOrder 方法中的 stockReservationService.createReservations()
```

**修复内容 2 - 库存校验改为使用可用库存**：
`addToCart()`、`updateQuantity()`、`checkout()` 三个方法中的库存校验，都改为使用**可用库存**（`stock - reserved_stock`）：
```java
// 修复前
if (product.getStock() < quantity) {
    throw new IllegalArgumentException("库存不足");
}

// 修复后
int availableStock = product.getStock() - (product.getReservedStock() == null ? 0 : product.getReservedStock());
if (availableStock < quantity) {
    throw new IllegalArgumentException("库存不足，可用库存: " + availableStock);
}
```

**关键点**：
- 下单时的库存预占逻辑已经在 `OrderServiceImpl.createOrder()` 中通过 `stockReservationService.createReservations()` 正确实现，使用数据库行锁（`UPDATE ... WHERE`）保证并发安全
- 确认收货时的正式扣减逻辑也已经在 `OrderServiceImpl.updateOrderStatus()` 中通过 `stockReservationService.deductStock()` 正确实现
- `CartServiceImpl.checkout()` 只需校验库存，不需要也不应该直接操作库存

#### 2. 后端修复 - CartItemDTO

**文件**：[backend/src/main/java/com/example/demo/dto/CartItemDTO.java](backend/src/main/java/com/example/demo/dto/CartItemDTO.java#L24-L28)

**修复内容**：新增 `productReservedStock` 字段：
```java
private Integer productStock;
private Integer productReservedStock;  // 新增
private String productCategory;
```

#### 3. 后端修复 - CartMapper.xml

**文件**：[backend/src/main/resources/mapper/CartMapper.xml](backend/src/main/resources/mapper/CartMapper.xml#L5-L40)

**修复内容 1 - resultMap 中增加字段映射**：
```xml
<result property="productStock" column="product_stock"/>
<result property="productReservedStock" column="product_reserved_stock"/>  <!-- 新增 -->
<result property="productCategory" column="product_category"/>
```

**修复内容 2 - 两个 SELECT 语句中增加字段查询**：
```xml
<!-- 修复前 -->
SELECT c.id, ..., p.stock AS product_stock, p.category AS product_category

<!-- 修复后 -->
SELECT c.id, ..., p.stock AS product_stock, 
       p.reserved_stock AS product_reserved_stock, p.category AS product_category
```

#### 4. 前端修复 - CartView.vue

**文件**：[frontend/src/views/CartView.vue](frontend/src/views/CartView.vue)

**修复内容 1 - 更新演示特性标签**：
```vue
<!-- 修复前 -->
<el-tag size="small" type="info" class="ml-8">自动扣减库存</el-tag>

<!-- 修复后 -->
<el-tag size="small" type="success" class="ml-8">下单预占</el-tag>
<el-tag size="small" type="warning" class="ml-8">收货扣减</el-tag>
<el-tag size="small" type="info" class="ml-8">防止超卖</el-tag>
```

**修复内容 2 - 数量选择器和库存提示使用可用库存**：
```vue
<!-- 修复前 -->
<el-input-number
  v-model="item.quantity"
  :min="1"
  :max="item.productStock"
  ...
/>
<el-button ... :disabled="item.quantity >= item.productStock" ... />
<span class="stock-tip">库存: {{ item.productStock }}</span>

<!-- 修复后 -->
<el-input-number
  v-model="item.quantity"
  :min="1"
  :max="getAvailableStock(item)"
  ...
/>
<el-button ... :disabled="item.quantity >= getAvailableStock(item)" ... />
<span class="stock-tip">
  可用: {{ getAvailableStock(item) }}
  <span v-if="(item.productReservedStock || 0) > 0" class="reserved-tip">
    (预占: {{ item.productReservedStock || 0 }})
  </span>
</span>
```

**修复内容 3 - 新增计算可用库存的方法**：
```javascript
const getAvailableStock = (item) => {
  const total = item.productStock || 0
  const reserved = item.productReservedStock || 0
  return Math.max(0, total - reserved)
}
```

**修复内容 4 - 新增预占库存提示样式**：
```css
.stock-tip {
  color: #67c23a;  /* 从灰色改为绿色 */
}
.reserved-tip {
  color: #e6a23c;  /* 橙色 */
  margin-left: 4px;
}
```

#### 5. 库存流转机制确认（无需修改，逻辑正确）

**正确的库存流转流程**：
```
1. 下单 → createReservations() 
   → reserved_stock += 数量 （只预占，stock 不变）

2. 取消订单/超时 → releaseReservations()
   → reserved_stock -= 数量 （释放预占，stock 不变）

3. 确认收货 → deductStock()
   → stock -= 数量 （正式扣减总库存）
   → reserved_stock -= 数量 （同时减少预占）
```

**修复验证**：
1. 重启后端和前端服务
2. 查看数据库中 `product` 表的某商品，记录初始 `stock` 和 `reserved_stock`（应为 0）
3. 进入商品页面，确认显示"总库存"、"可用库存"、"预占库存"三列，可用库存 = 总库存
4. 进入购物车页面，添加该商品到购物车
5. 确认购物车中商品显示"可用: X"，如果有预占还会显示"(预占: Y)"
6. **下单前**，记录商品的 `stock` 值
7. 提交订单，创建成功后立即查询商品表
8. **验证**：`stock` 值**不变**（正确！），`reserved_stock` 增加了对应数量
9. 进入"库存预占管理"页面，确认有一条状态为"预占中"的记录
10. 进入订单详情页，确认"库存预占记录"区域显示了预占信息
11. **取消订单**，再次查询商品表
12. **验证**：`reserved_stock` 恢复为 0，`stock` 仍不变（正确！）
13. 预占记录状态变为"已释放"，释放原因为"订单已取消"
14. 重新下单，然后将订单状态推进到"已完成"（模拟确认收货）
15. **验证**：`stock` 扣减了对应数量，`reserved_stock` 恢复为 0（正确！）
16. 预占记录状态变为"已扣减"
17. 测试高并发场景（可选）：多个用户同时下单同一商品，确认不会出现超卖
18. 执行 `mvn compile` 确认后端编译成功，无任何错误
19. 前端浏览器控制台无 JavaScript 错误

---

## 修复登记模板（新增修复请复制此模板）

### 修复 #序号：问题标题

**问题描述**：清晰描述问题现象

**发现日期**：YYYY-MM-DD

**问题根源**：分析问题产生的根本原因

**影响范围**：说明受影响的模块/功能

**修复方案**：

#### 1. 后端修复（如适用）
**文件**：[文件路径](文件路径#L起始行-L结束行)

**修复内容**：
```
代码或修改说明
```

#### 2. 前端修复（如适用）
**文件**：[文件路径](文件路径#L起始行-L结束行)

**修复内容**：
```
代码或修改说明
```

**修复验证**：列出验证步骤

---

## 修复规范

1. 所有修复必须在此文件登记，包括问题描述、根源分析、修复内容
2. 修复涉及的代码必须提供文件链接和行号
3. 关键代码修改必须提供代码片段
4. 必须包含修复验证步骤
5. 优先采用前后端双重校验的方式增强安全性
6. 修复完成后更新本文件的修复记录
