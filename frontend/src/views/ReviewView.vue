<template>
  <div class="review-page">
    <el-alert type="success" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">唯一索引防重复评价</el-tag>
        <el-tag size="small" type="warning" class="ml-8">仅已完成订单可评价</el-tag>
        <el-tag size="small" type="primary" class="ml-8">AVG 聚合统计平均分</el-tag>
        <el-tag size="small" class="ml-8">多表联查 JOIN 4 张表</el-tag>
      </template>
    </el-alert>

    <div class="view-switch">
      <el-radio-group v-model="activeTab" size="large">
        <el-radio-button value="pending" @change="switchTab">
          <el-icon><Clock /></el-icon>
          <span style="margin-left:4px">待评价 ({{ pendingCount }})</span>
        </el-radio-button>
        <el-radio-button value="search" @change="switchTab">
          <el-icon><Search /></el-icon>
          <span style="margin-left:4px">商品评价查询</span>
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-show="activeTab === 'pending'">
      <el-card shadow="never" class="main-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">待评价商品</span>
            <el-tag type="info" size="small">仅显示状态为「已完成」订单中未评价的商品</el-tag>
          </div>
        </template>

        <el-table :data="pendingList" v-loading="pendingLoading" stripe border empty-text="暂无待评价商品">
          <el-table-column prop="productName" label="商品名称" min-width="180" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="price" label="单价" width="110">
            <template #default="{ row }">
              <span class="price">¥{{ Number(row.price).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderCreatedTime" label="订单时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.orderCreatedTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openReviewDialog(row)">去评价</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!pendingLoading && pendingList.length === 0" description="所有已完成订单的商品都已评价~" />
      </el-card>
    </div>

    <div v-show="activeTab === 'search'">
      <el-card shadow="never" class="main-card">
        <template #header>
          <span class="card-title">商品评价查询</span>
        </template>

        <div class="search-bar">
          <el-select v-model="selectedProductId" placeholder="选择商品" clearable style="width: 240px" @change="loadReviews">
            <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="query.rating" placeholder="按评分筛选" clearable style="width: 140px" @change="loadReviews">
            <el-option v-for="r in ratingOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
          <el-button type="primary" :icon="Refresh" @click="loadReviews">刷新</el-button>
        </div>

        <div v-if="selectedProductId && stats" class="stats-area">
          <div class="stats-overview">
            <div class="avg-rating">
              <span class="avg-number">{{ Number(stats.avgRating).toFixed(1) }}</span>
              <div class="stars">
                <el-rate v-model="avgRatingDisplay" disabled :max="5" :show-score="false" />
              </div>
              <span class="total-count">{{ stats.totalCount }} 条评价</span>
            </div>
            <div class="rating-distribution">
              <div v-for="r in [5,4,3,2,1]" :key="r" class="rating-bar">
                <span class="rating-label">{{ r }}星</span>
                <el-progress :percentage="getRatingPercent(r)" :show-text="false" :stroke-width="8" :color="getRatingColor(r)" />
                <span class="rating-count">{{ getRatingCount(r) }}</span>
              </div>
            </div>
          </div>
        </div>

        <el-table :data="reviewList" v-loading="reviewLoading" stripe border empty-text="请选择商品查看评价">
          <el-table-column prop="username" label="评价用户" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.username || '匿名用户' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rating" label="评分" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.rating" disabled :max="5" :show-score="false" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="250" />
          <el-table-column prop="createdTime" label="评价时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.createdTime) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pagination"
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="reviewTotal"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @change="loadReviews"
        />
      </el-card>
    </div>

    <el-dialog v-model="reviewDialogVisible" title="提交评价" width="500px">
      <el-form :model="reviewForm" ref="reviewFormRef" label-width="80px">
        <el-form-item label="商品名称">
          <span class="form-text">{{ reviewForm.productName }}</span>
        </el-form-item>
        <el-form-item label="订单编号">
          <el-tag size="small">{{ reviewForm.orderId }}</el-tag>
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
        <el-button type="primary" @click="handleSubmitReview" :loading="submitting">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Search, Refresh } from '@element-plus/icons-vue'
import { getProductList } from '../api/product'
import { submitReview as submitReviewApi, getReviewPage, getReviewStats, getPendingReviews } from '../api/review'

const activeTab = ref('pending')
const pendingLoading = ref(false)
const reviewLoading = ref(false)
const submitting = ref(false)
const pendingList = ref([])
const reviewList = ref([])
const reviewTotal = ref(0)
const productList = ref([])
const selectedProductId = ref(null)
const stats = ref(null)
const reviewDialogVisible = ref(false)
const reviewFormRef = ref()

const pendingCount = computed(() => pendingList.value.length)
const avgRatingDisplay = computed(() => stats.value ? Math.round(Number(stats.value.avgRating)) : 0)

const query = reactive({ current: 1, size: 10, rating: null })

const ratingOptions = [
  { value: 5, label: '5星 - 非常好' },
  { value: 4, label: '4星 - 好' },
  { value: 3, label: '3星 - 一般' },
  { value: 2, label: '2星 - 差' },
  { value: 1, label: '1星 - 非常差' }
]

const ratingTexts = ['非常差', '差', '一般', '好', '非常好']

const reviewForm = reactive({
  orderItemId: null,
  orderId: null,
  productId: null,
  productName: '',
  rating: null,
  content: ''
})

const getRatingCount = (r) => {
  if (!stats.value) return 0
  const map = { 1: 'rating1Count', 2: 'rating2Count', 3: 'rating3Count', 4: 'rating4Count', 5: 'rating5Count' }
  return stats.value[map[r]] || 0
}

const getRatingPercent = (r) => {
  if (!stats.value || stats.value.totalCount === 0) return 0
  return Math.round((getRatingCount(r) / stats.value.totalCount) * 100)
}

const getRatingColor = (r) => {
  const colors = { 5: '#67C23A', 4: '#909399', 3: '#E6A23C', 2: '#F56C6C', 1: '#F56C6C' }
  return colors[r] || '#909399'
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const switchTab = () => {
  if (activeTab.value === 'pending') {
    loadPendingReviews()
  } else {
    loadProductList()
  }
}

const loadPendingReviews = async () => {
  pendingLoading.value = true
  try {
    const res = await getPendingReviews()
    pendingList.value = res.data
  } finally {
    pendingLoading.value = false
  }
}

const loadProductList = async () => {
  try {
    const res = await getProductList()
    productList.value = res.data
  } catch (e) {}
}

const loadReviews = async () => {
  if (!selectedProductId.value) {
    reviewList.value = []
    reviewTotal.value = 0
    stats.value = null
    return
  }
  reviewLoading.value = true
  try {
    const [reviewRes, statsRes] = await Promise.all([
      getReviewPage({ ...query, productId: selectedProductId.value }),
      getReviewStats(selectedProductId.value)
    ])
    reviewList.value = reviewRes.data.records
    reviewTotal.value = reviewRes.data.total
    stats.value = statsRes.data
  } finally {
    reviewLoading.value = false
  }
}

const openReviewDialog = (row) => {
  Object.assign(reviewForm, {
    orderItemId: row.orderItemId,
    orderId: row.orderId,
    productId: row.productId,
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
  submitting.value = true
  try {
    await submitReviewApi({
      orderItemId: reviewForm.orderItemId,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success('评价提交成功！')
    reviewDialogVisible.value = false
    loadPendingReviews()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPendingReviews()
  loadProductList()
})
</script>

<style scoped>
.review-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 600; margin-right: 8px; }
.view-switch { margin-bottom: 8px; }
.search-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.price { color: #e6a23c; font-weight: 600; }
.time-text { font-size: 13px; color: #718096; }
.form-text { color: #303133; font-weight: 500; }
.pagination { margin-top: 16px; justify-content: flex-end; }

.stats-area { margin-bottom: 16px; padding: 20px; background: #fafafa; border-radius: 8px; }
.stats-overview { display: flex; gap: 40px; align-items: flex-start; }
.avg-rating { display: flex; flex-direction: column; align-items: center; min-width: 160px; }
.avg-number { font-size: 48px; font-weight: 700; color: #e6a23c; line-height: 1; }
.stars { margin: 8px 0; }
.total-count { font-size: 13px; color: #909399; }
.rating-distribution { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.rating-bar { display: flex; align-items: center; gap: 12px; }
.rating-label { width: 40px; font-size: 13px; color: #606266; }
.rating-bar :deep(.el-progress) { flex: 1; }
.rating-count { width: 50px; font-size: 13px; color: #909399; text-align: right; }
</style>
