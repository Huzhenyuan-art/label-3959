<template>
  <div class="reservation-page">
    <el-alert type="success" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">库存预占防止超卖</el-tag>
        <el-tag size="small" type="warning" class="ml-8">下单预占/取消释放/收货扣减</el-tag>
        <el-tag size="small" class="ml-8">超时自动释放（定时任务）</el-tag>
        <el-tag size="small" type="danger" class="ml-8">数据库行锁保证并发安全</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input v-model="query.orderId" placeholder="订单ID" clearable style="width:140px" @keyup.enter="loadData" />
          <el-input v-model="query.productId" placeholder="商品ID" clearable style="width:140px" @keyup.enter="loadData" />
          <el-select v-model="query.status" placeholder="预占状态" clearable style="width:140px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <el-button type="warning" :icon="RefreshRight" @click="handleReleaseExpired">
          手动释放超时预占
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="预占ID" width="90" />
        <el-table-column prop="orderId" label="订单ID" width="90">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goOrderDetail(row.orderId)">
              {{ row.orderId }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="productId" label="商品ID" width="90" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="quantity" label="预占数量" width="100" align="center" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'expired': isExpired(row) }">{{ formatTime(row.expireTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="releaseReason" label="释放原因" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </el-card>

    <el-card shadow="never" class="info-card">
      <template #header>
        <span class="card-title">库存预占机制说明</span>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="预占时机">
          <el-tag type="success">订单创建时</el-tag>
          下单时立即预占对应商品库存，防止并发下单导致超卖
        </el-descriptions-item>
        <el-descriptions-item label="释放时机">
          <el-tag type="warning">订单取消时</el-tag>
          <el-tag type="warning" class="ml-8">预占超时时</el-tag>
          取消订单或超过30分钟未支付，自动释放预占库存
        </el-descriptions-item>
        <el-descriptions-item label="扣减时机">
          <el-tag type="success">确认收货时</el-tag>
          订单状态变为"已完成"时，将预占库存转为正式扣减
        </el-descriptions-item>
        <el-descriptions-item label="并发控制">
          <el-tag type="danger">数据库行锁</el-tag>
          使用 UPDATE ... WHERE 条件实现乐观锁，保证并发场景下库存数据一致性
        </el-descriptions-item>
        <el-descriptions-item label="定时任务">
          <el-tag type="info">每分钟执行</el-tag>
          自动扫描并释放超时预占记录，保证库存及时回流
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getStockReservationPage, releaseExpiredReservations } from '../api/stockReservation'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, orderId: null, productId: null, status: null })

const statusOptions = [
  { label: '预占中', value: 0 },
  { label: '已释放', value: 1 },
  { label: '已扣减', value: 2 }
]

const statusTagType = (s) => ['warning', 'info', 'success'][s] || ''
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'
const isExpired = (row) => row.status === 0 && row.expireTime && new Date(row.expireTime) < new Date()

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (params.orderId === '') params.orderId = null
    if (params.productId === '') params.productId = null
    const res = await getStockReservationPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.orderId = null
  query.productId = null
  query.status = null
  query.current = 1
  loadData()
}

const goOrderDetail = (orderId) => {
  router.push(`/orders/${orderId}`)
}

const handleReleaseExpired = async () => {
  try {
    const res = await releaseExpiredReservations()
    ElMessage.success(`成功释放 ${res.data} 条超时预占记录`)
    loadData()
  } catch (e) {
    ElMessage.error('释放失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.reservation-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card, .info-card { border-radius: 12px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area { display: flex; align-items: center; gap: 8px; }
.card-title { font-weight: 600; margin-right: 8px; }
.time-text { font-size: 13px; color: #718096; }
.expired { color: #f56c6c; text-decoration: line-through; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
