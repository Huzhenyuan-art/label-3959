<template>
  <div class="detail-page">
    <div class="back-bar">
      <el-button :icon="ArrowLeft" text @click="$router.back()">返回订单列表</el-button>
    </div>

    <el-alert type="success" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">XML 一对多 resultMap 联查</el-tag>
        <el-tag size="small" type="warning" class="ml-8">JOIN order + user + order_item + product</el-tag>
        <el-tag size="small" class="ml-8">collection 映射订单明细列表</el-tag>
      </template>
    </el-alert>

    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="order">
      <el-row :gutter="16">
        <!-- 订单基本信息 -->
        <el-col :span="12">
          <el-card shadow="never" class="info-card">
            <template #header>
              <span class="card-title">订单信息</span>
              <el-tag :type="statusTagType(order.status)" size="small">{{ order.statusLabel }}</el-tag>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单ID">
                <el-tag>{{ order.id }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="订单金额">
                <span class="amount">¥{{ Number(order.totalAmount).toFixed(2) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="乐观锁版本">
                <el-tag type="info">v{{ order.version }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(order.createdTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatTime(order.updatedTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- 用户信息（多表联查） -->
        <el-col :span="12">
          <el-card shadow="never" class="info-card">
            <template #header>
              <span class="card-title">用户信息</span>
              <el-tag type="warning" size="small">多表联查字段</el-tag>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="用户ID">{{ order.userId }}</el-descriptions-item>
              <el-descriptions-item label="用户名">
                <span class="highlight">{{ order.username }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ order.userEmail }}</el-descriptions-item>
            </el-descriptions>
            <el-alert type="info" :closable="false" style="margin-top:12px" size="small">
              <template #title>
                通过 LEFT JOIN user ON o.user_id = u.id 获取，无需二次查询
              </template>
            </el-alert>
          </el-card>
        </el-col>
      </el-row>

      <!-- 收货地址信息 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-title">收货地址</span>
          <el-tag type="success" size="small">下单时快照</el-tag>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="收件人">
            <span class="highlight">{{ order.receiverName || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ order.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ order.receiverAddress || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="order.receiverName" type="info" :closable="false" style="margin-top:12px" size="small">
          <template #title>
            地址信息为下单时的快照，即使后续修改地址，订单地址也不会变化
          </template>
        </el-alert>
      </el-card>

      <!-- 订单明细（一对多） -->
      <el-card shadow="never" class="items-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">订单明细</span>
            <div>
              <el-tag type="success" size="small">一对多 collection 映射</el-tag>
              <el-tag type="warning" size="small" class="ml-8">JOIN order_item + product</el-tag>
            </div>
          </div>
        </template>
        <el-table :data="order.items" border stripe>
          <el-table-column prop="id" label="明细ID" width="90" />
          <el-table-column prop="productId" label="商品ID" width="90" />
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="productCategory" label="分类（来自product表）" width="160">
            <template #default="{ row }">
              <el-tag size="small">{{ row.productCategory }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="下单单价" width="110">
            <template #default="{ row }">
              <span class="amount">¥{{ Number(row.price).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="amount">¥{{ Number(row.subtotal).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评价" width="120" align="center" v-if="order.status === 3">
            <template #default="{ row }">
              <el-button
                v-if="!reviewedItems[row.id]"
                type="primary"
                size="small"
                link
                @click="openReviewDialog(row)">
                去评价
              </el-button>
              <el-tag v-else type="success" size="small">已评价</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="total-bar">
          <span>合计：</span>
          <span class="total-amount">¥{{ Number(order.totalAmount).toFixed(2) }}</span>
        </div>
      </el-card>

      <!-- 退款信息 -->
      <el-card shadow="never" class="refund-card" v-if="pendingRefund || canApplyRefund">
        <template #header>
          <div class="card-header">
            <span class="card-title">退款管理</span>
            <el-tag v-if="pendingRefund" :type="statusTagType(pendingRefund.status)" size="small">
              {{ pendingRefund.statusLabel }}
            </el-tag>
          </div>
        </template>
        <div v-if="pendingRefund">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="退款单号">
              <el-link type="primary" @click="goRefundDetail(pendingRefund.id)">
                {{ pendingRefund.refundNo }}
              </el-link>
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">
              <span class="amount">¥{{ Number(pendingRefund.refundAmount).toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="退款原因">
              {{ pendingRefund.refundReason }}
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">
              {{ formatTime(pendingRefund.createdTime) }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="action-bar">
            <el-button
              v-if="!isAdmin && pendingRefund.status === 0"
              type="danger"
              @click="goRefundDetail(pendingRefund.id)"
            >
              取消申请
            </el-button>
            <el-button
              v-if="isAdmin && pendingRefund.status === 0"
              type="warning"
              @click="goRefundDetail(pendingRefund.id)"
            >
              立即审核
            </el-button>
          </div>
        </div>
        <div v-else-if="canApplyRefund" class="apply-area">
          <el-alert type="info" :closable="false">
            您可以对该订单申请退款，审核通过后订单状态将变更为"已取消"，库存将自动回滚。
          </el-alert>
          <el-button type="danger" style="margin-top: 12px" @click="goApplyRefund">
            申请退款
          </el-button>
        </div>
      </el-card>

      <!-- 库存预占记录 -->
      <el-card shadow="never" class="reservation-card" v-if="reservations.length > 0">
        <template #header>
          <div class="card-header">
            <span class="card-title">库存预占记录</span>
            <div>
              <el-tag type="success" size="small">防止超卖</el-tag>
              <el-tag type="warning" size="small" class="ml-8">下单预占/取消释放/收货扣减</el-tag>
            </div>
          </div>
        </template>
        <el-table :data="reservations" border stripe>
          <el-table-column prop="id" label="预占ID" width="90" />
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="quantity" label="预占数量" width="100" align="center" />
          <el-table-column prop="statusDesc" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="reservationTagType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expireTime" label="过期时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.expireTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="releaseReason" label="释放原因" min-width="120" show-overflow-tooltip />
          <el-table-column prop="createdTime" label="创建时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.createdTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- SQL 说明 -->
      <el-card shadow="never" class="sql-card">
        <template #header>
          <span>本页 SQL（OrderMapper.xml）</span>
        </template>
        <pre class="sql-code">SELECT
  o.id AS order_id, o.user_id,
  u.username, u.email AS user_email,   -- 联查用户表
  o.total_amount, o.status, o.version,
  oi.id AS item_id, oi.product_id,
  oi.product_name, p.category AS product_category,  -- 联查商品表
  oi.quantity, oi.price AS item_price,
  (oi.price * oi.quantity) AS subtotal
FROM `order` o
LEFT JOIN user u        ON o.user_id    = u.id AND u.deleted = 0
LEFT JOIN order_item oi ON oi.order_id  = o.id
LEFT JOIN product p     ON oi.product_id = p.id
WHERE o.id = #{id}</pre>
      </el-card>

      <el-dialog v-model="reviewDialogVisible" title="提交评价" width="500px">
        <el-form :model="reviewForm" ref="reviewFormRef" label-width="80px">
          <el-form-item label="商品名称">
            <span class="form-text">{{ reviewForm.productName }}</span>
          </el-form-item>
          <el-form-item label="评分" prop="rating" :rules="[{ required: true, message: '请选择评分' }]">
            <el-rate v-model="reviewForm.rating" :max="5" :show-text="true" :texts="ratingTexts" />
          </el-form-item>
          <el-form-item label="评价内容" prop="content" :rules="[{ max: 500, message: '评价内容不能超过500字' }]">
            <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="分享您的使用体验，帮助其他用户做出选择~" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitReview" :loading="reviewSubmitting">提交评价</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getOrderDetail } from '../api/order'
import { submitReview, getPendingReviews } from '../api/review'
import { getStockReservationsByOrderId } from '../api/stockReservation'
import { getRefundPage } from '../api/refund'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isAdmin } = authStore
const loading = ref(true)
const order = ref(null)
const reservations = ref([])
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref()
const reviewedItems = ref({})
const pendingRefund = ref(null)

const ratingTexts = ['非常差', '差', '一般', '好', '非常好']

const reviewForm = reactive({
  orderItemId: null,
  productName: '',
  rating: null,
  content: ''
})

const statusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger'][s] || ''
const reservationTagType = (s) => ['warning', 'info', 'success'][s] || ''
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const canApplyRefund = computed(() => {
  if (!order.value) return false
  if (pendingRefund.value) return false
  const status = order.value.status
  return (status === 1 || status === 2 || status === 3)
})

const loadReservations = async () => {
  try {
    const res = await getStockReservationsByOrderId(route.params.id)
    reservations.value = res.data
  } catch (e) {}
}

const loadReviewedItems = async () => {
  try {
    const res = await getPendingReviews()
    const pendingItemIds = new Set(res.data.map(item => item.orderItemId))
    const reviewed = {}
    if (order.value) {
      order.value.items.forEach(item => {
        if (!pendingItemIds.has(item.id)) {
          reviewed[item.id] = true
        }
      })
    }
    reviewedItems.value = reviewed
  } catch (e) {}
}

const loadPendingRefund = async () => {
  try {
    const res = await getRefundPage({ orderId: route.params.id, size: 100 })
    const refunds = res.data.records
    if (refunds && refunds.length > 0) {
      pendingRefund.value = refunds.find(r => r.status === 0) || refunds[0]
    }
  } catch (e) {}
}

const goApplyRefund = () => {
  router.push(`/orders/${route.params.id}/refund/apply`)
}

const goRefundDetail = (id) => {
  router.push(`/refunds/${id}`)
}

const openReviewDialog = (row) => {
  Object.assign(reviewForm, {
    orderItemId: row.id,
    productName: row.productName,
    rating: null,
    content: ''
  })
  reviewDialogVisible.value = true
}

const handleSubmitReview = async () => {
  await reviewFormRef.value.validate()
  if (!reviewForm.rating || reviewForm.rating < 1 || reviewForm.rating > 5) {
    ElMessage.error('请选择1-5星评分')
    return
  }
  reviewSubmitting.value = true
  try {
    await submitReview({
      orderItemId: reviewForm.orderItemId,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success('评价提交成功！')
    reviewedItems.value[reviewForm.orderItemId] = true
    reviewDialogVisible.value = false
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
    await Promise.all([
      loadReservations(),
      order.value.status === 3 ? loadReviewedItems() : Promise.resolve()
    ])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page { display: flex; flex-direction: column; gap: 16px; }
.back-bar { display: flex; align-items: center; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.info-card, .items-card, .sql-card { border-radius: 12px; }
.card-title { font-weight: 600; margin-right: 8px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.amount { color: #e6a23c; font-weight: 700; }
.highlight { color: #409EFF; font-weight: 600; }
.total-bar { display: flex; justify-content: flex-end; align-items: center; padding: 12px 16px; gap: 8px; font-size: 15px; font-weight: 600; }
.total-amount { color: #e6a23c; font-size: 22px; font-weight: 700; }
.sql-code {
  background: #1a1f2e;
  color: #68d391;
  padding: 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.8;
  overflow-x: auto;
  font-family: 'Fira Code', 'Consolas', monospace;
  white-space: pre-wrap;
}
.loading-area { padding: 24px; }
.form-text { color: #303133; font-weight: 500; }
.refund-card { border-radius: 12px; }
.apply-area { padding: 8px 0; }
.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
