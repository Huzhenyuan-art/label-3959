<template>
  <div class="home-page">
    <!-- 特性卡片 -->
    <div class="feature-grid">
      <div v-for="f in features" :key="f.title" class="feature-card">
        <div class="feature-icon" :style="{ background: f.bg }">
          <el-icon size="24" :color="f.color"><component :is="f.icon" /></el-icon>
        </div>
        <div class="feature-info">
          <div class="feature-title">{{ f.title }}</div>
          <div class="feature-desc">{{ f.desc }}</div>
        </div>
      </div>
    </div>

    <!-- 数据统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-number" :style="{ color: s.color }">
            <el-skeleton v-if="loading" :rows="1" animated />
            <span v-else>{{ s.value }}</span>
          </div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分类统计表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>商品分类统计</span>
          <el-tag size="small">@Select 注解 SQL</el-tag>
        </div>
      </template>
      <el-table :data="categoryStats" v-loading="loading" stripe>
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="count" label="商品数量">
          <template #default="{ row }">
            <el-tag>{{ row.count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="库存总量" />
        <el-table-column prop="avgPrice" label="平均价格">
          <template #default="{ row }">
            ¥{{ Number(row.avgPrice).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- MyBatis Plus 特性列表 -->
    <el-card shadow="never" class="features-detail-card">
      <template #header>
        <span>MyBatis Plus 演示特性清单</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item v-for="item in featureList" :key="item.name" :label="item.name">
          <el-tag :type="item.type" size="small">{{ item.page }}</el-tag>
          <span class="feature-detail">{{ item.detail }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryStats } from '../api/product'
import { getUserList } from '../api/user'
import { getProductList } from '../api/product'
import { getOrderPage } from '../api/order'

const loading = ref(true)
const categoryStats = ref([])
const stats = ref([
  { label: '注册用户', value: 0, color: '#409EFF' },
  { label: '在售商品', value: 0, color: '#67C23A' },
  { label: '订单总数', value: 0, color: '#E6A23C' },
  { label: '商品分类', value: 0, color: '#F56C6C' }
])

const features = [
  { title: '基础 CRUD', desc: 'BaseMapper 提供完整单表操作', icon: 'Edit', bg: 'rgba(64,158,255,0.1)', color: '#409EFF' },
  { title: '条件构造器', desc: 'LambdaQueryWrapper 类型安全查询', icon: 'Filter', bg: 'rgba(103,194,58,0.1)', color: '#67C23A' },
  { title: '分页插件', desc: 'IPage + PaginationInnerInterceptor', icon: 'Document', bg: 'rgba(230,162,60,0.1)', color: '#E6A23C' },
  { title: '逻辑删除', desc: '@TableLogic 软删除，查询自动过滤', icon: 'Delete', bg: 'rgba(245,108,108,0.1)', color: '#F56C6C' },
  { title: '自动填充', desc: '@TableField fill 自动填充时间戳', icon: 'Timer', bg: 'rgba(144,147,153,0.1)', color: '#909399' },
  { title: '乐观锁', desc: '@Version 防并发冲突', icon: 'Lock', bg: 'rgba(64,158,255,0.1)', color: '#409EFF' },
  { title: '多表联查', desc: 'XML Mapper 一对多 resultMap', icon: 'Connection', bg: 'rgba(103,194,58,0.1)', color: '#67C23A' },
  { title: '批量操作', desc: 'saveBatch 高效批量插入', icon: 'CopyDocument', bg: 'rgba(230,162,60,0.1)', color: '#E6A23C' }
]

const featureList = [
  { name: 'BaseMapper CRUD', page: '用户管理', type: '', detail: '增删改查全覆盖' },
  { name: 'LambdaQueryWrapper', page: '用户管理', type: '', detail: '类型安全的条件构造，避免字段名拼写错误' },
  { name: '分页插件', page: '商品管理', type: 'success', detail: 'page() 方法 + PaginationInnerInterceptor 自动分页' },
  { name: '逻辑删除 @TableLogic', page: '用户管理', type: 'danger', detail: '删除时设 deleted=1，所有查询自动加 deleted=0 条件' },
  { name: '自动填充 @TableField', page: '用户/商品', type: 'warning', detail: 'INSERT 时填 createdTime，UPDATE 时填 updatedTime' },
  { name: '乐观锁 @Version', page: '用户/订单', type: 'info', detail: 'updateById 自动校验 version，防并发修改丢失' },
  { name: 'XML 一对多联查', page: '订单管理', type: 'success', detail: 'resultMap + collection 映射订单明细' },
  { name: '批量插入 saveBatch', page: '用户管理', type: '', detail: '高效批量写入，减少数据库交互次数' },
  { name: '自定义 @Select SQL', page: '首页统计', type: 'warning', detail: '注解式 SQL，分组统计商品分类数据' },
  { name: '动态 SQL <if>', page: '订单查询', type: 'info', detail: 'XML 中使用 <where>/<if> 构建动态查询条件' }
]

onMounted(async () => {
  try {
    const [usersRes, productsRes, ordersRes, statsRes] = await Promise.all([
      getUserList({}),
      getProductList(),
      getOrderPage({ current: 1, size: 1 }),
      getCategoryStats()
    ])
    stats.value[0].value = usersRes.data.length
    stats.value[1].value = productsRes.data.length
    stats.value[2].value = ordersRes.data.total
    stats.value[3].value = statsRes.data.length
    categoryStats.value = statsRes.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page { display: flex; flex-direction: column; gap: 20px; }

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.feature-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-title { font-weight: 600; font-size: 14px; color: #1a202c; }
.feature-desc { font-size: 12px; color: #718096; margin-top: 2px; }

.stats-row { margin: 0; }

.stat-card { text-align: center; border-radius: 12px; }
.stat-number { font-size: 32px; font-weight: 700; }
.stat-label { font-size: 13px; color: #718096; margin-top: 4px; }

.table-card, .features-detail-card { border-radius: 12px; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.feature-detail { font-size: 12px; color: #718096; margin-left: 8px; }
</style>
