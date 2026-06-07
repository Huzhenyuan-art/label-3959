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
