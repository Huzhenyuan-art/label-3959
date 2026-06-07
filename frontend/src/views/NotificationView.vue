<template>
  <div class="notification-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：
        <el-tag size="small" type="success" class="ml-8">未读消息实时轮询</el-tag>
        <el-tag size="small" type="warning" class="ml-8">消息分类筛选</el-tag>
        <el-tag size="small" class="ml-8">一键标记已读</el-tag>
        <el-tag size="small" type="danger" class="ml-8">业务场景自动推送</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-select v-model="query.isRead" placeholder="阅读状态" clearable style="width: 140px">
            <el-option label="未读" :value="false" />
            <el-option label="已读" :value="true" />
          </el-select>
          <el-select v-model="query.type" placeholder="消息类型" clearable style="width: 140px">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <div class="action-area">
          <el-button type="success" :icon="Check" @click="handleMarkAllRead" :disabled="notificationStore.unreadCount === 0">
            全部标为已读
          </el-button>
        </div>
      </div>

      <div class="stats-row">
        <el-statistic title="全部消息" :value="notificationStore.total" />
        <el-statistic title="未读消息" :value="notificationStore.unreadCount" value-color="#f56c6c" />
      </div>

      <el-table :data="notificationStore.list" v-loading="notificationStore.loading" stripe class="notification-table">
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <span class="unread-dot" v-if="!row.read"></span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="发送时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.read ? 'info' : 'danger'" size="small">{{ row.read ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)" :disabled="row.read">
              标记已读
            </el-button>
            <el-button link type="success" size="small" @click="handleViewDetail(row)" v-if="row.bizId && row.bizType">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="notificationStore.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="消息详情" width="500px">
      <div class="detail-content" v-if="currentNotification">
        <div class="detail-header">
          <el-tag :type="getTypeTagType(currentNotification.type)" size="small">
            {{ getTypeLabel(currentNotification.type) }}
          </el-tag>
          <span class="detail-title">{{ currentNotification.title }}</span>
        </div>
        <div class="detail-time">
          <el-icon><Clock /></el-icon>
          {{ formatTime(currentNotification.createdTime) }}
        </div>
        <el-divider />
        <div class="detail-body">{{ currentNotification.content }}</div>
        <div class="detail-footer" v-if="currentNotification.bizId && currentNotification.bizType">
          <el-button type="primary" @click="goToBizDetail">查看相关{{ getBizLabel(currentNotification.bizType) }}</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Check, Clock } from '@element-plus/icons-vue'
import { useNotificationStore } from '../store/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const detailDialogVisible = ref(false)
const currentNotification = ref(null)

const query = reactive({
  current: 1,
  size: 10,
  isRead: null,
  type: null
})

const typeOptions = [
  { label: '订单状态变更', value: 1 },
  { label: '退款处理结果', value: 2 },
  { label: '库存预警', value: 3 },
  { label: '系统通知', value: 4 }
]

const getTypeLabel = (type) => {
  const t = typeOptions.find(t => t.value === type)
  return t ? t.label : '未知类型'
}

const getTypeTagType = (type) => {
  const types = { 1: 'primary', 2: 'warning', 3: 'danger', 4: 'info' }
  return types[type] || ''
}

const getBizLabel = (bizType) => {
  const labels = { 'ORDER': '订单', 'REFUND': '退款', 'PRODUCT': '商品' }
  return labels[bizType] || '业务'
}

const loadData = async () => {
  const params = {
    current: query.current,
    size: query.size,
    isRead: query.isRead,
    type: query.type ?? undefined
  }
  await notificationStore.fetchNotificationPage(params)
}

const resetQuery = () => {
  query.current = 1
  query.isRead = null
  query.type = null
  loadData()
}

const handleView = async (row) => {
  await notificationStore.markAsReadById(row.id)
  ElMessage.success('已标记为已读')
}

const handleViewDetail = (row) => {
  currentNotification.value = row
  detailDialogVisible.value = true
  if (!row.read) {
    notificationStore.markAsReadById(row.id)
  }
}

const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有消息标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await notificationStore.markAllRead()
    ElMessage.success('所有消息已标记为已读')
    loadData()
  } catch {
  }
}

const goToBizDetail = () => {
  if (!currentNotification.value) return
  const { bizType, bizId } = currentNotification.value
  detailDialogVisible.value = false
  if (bizType === 'ORDER' || bizType === 'REFUND') {
    router.push(`/orders/${bizId}`)
  } else if (bizType === 'PRODUCT') {
    router.push('/products')
  }
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

onMounted(async () => {
  await notificationStore.fetchUnreadCount()
  loadData()
})
</script>

<style scoped>
.notification-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-alert {
  border-radius: 8px;
}

.ml-8 {
  margin-left: 8px;
}

.main-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.search-area, .action-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-row {
  display: flex;
  gap: 40px;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.stats-row :deep(.el-statistic__head),
.stats-row :deep(.el-statistic__content) {
  color: #fff !important;
}

.notification-table {
  margin-top: 16px;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
}

.time-text {
  font-size: 13px;
  color: #718096;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-content {
  padding: 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
}

.detail-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #718096;
}

.detail-body {
  padding: 16px;
  background: #f7f8fa;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  min-height: 80px;
}

.detail-footer {
  margin-top: 20px;
  text-align: right;
}
</style>
