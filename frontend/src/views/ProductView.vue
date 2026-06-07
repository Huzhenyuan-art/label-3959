<template>
  <div class="product-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">分页插件 IPage</el-tag>
        <el-tag size="small" type="warning" class="ml-8">LambdaQueryWrapper 多条件过滤</el-tag>
        <el-tag size="small" class="ml-8">@Select 注解自定义 SQL</el-tag>
        <el-tag size="small" type="info" class="ml-8">自动填充 createdTime/updatedTime</el-tag>
      </template>
    </el-alert>

    <el-row :gutter="16">
      <!-- 左侧：分类统计 -->
      <el-col :span="6">
        <el-card shadow="never" class="stats-card">
          <template #header>
            <span class="card-title">分类统计</span>
            <el-tag size="small" type="warning">@Select 自定义 SQL</el-tag>
          </template>
          <div v-loading="statsLoading">
            <div v-for="s in categoryStats" :key="s.category" class="stat-item">
              <div class="stat-category">{{ s.category }}</div>
              <div class="stat-meta">
                <span>{{ s.count }} 件</span>
                <span>¥{{ Number(s.avgPrice).toFixed(0) }} 均价</span>
              </div>
              <el-progress :percentage="Math.min(100, Number(s.count) * 20)" :show-text="false" :stroke-width="6" />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：商品列表 -->
      <el-col :span="18">
        <el-card shadow="never" class="main-card">
          <div class="toolbar">
            <div class="search-area">
              <el-input v-model="query.name" placeholder="商品名称" clearable style="width:180px" @keyup.enter="loadData" />
              <el-select v-model="query.category" placeholder="分类" clearable style="width:120px">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </div>
            <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
          </div>

          <el-table :data="tableData" v-loading="loading" stripe border>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="商品名称" min-width="160" />
            <el-table-column prop="category" label="分类" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="110">
              <template #default="{ row }">
                <span class="price">¥{{ Number(row.price).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.stock < 50 ? 'warning' : 'success'" size="small">{{ row.stock }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" size="small" :icon="ShoppingCart" @click="openAddToCart(row)" :disabled="row.stock <= 0">
                  加入购物车
                </el-button>
                <el-button link type="warning" size="small" :icon="ChatDotRound" @click="openViewReviews(row)">
                  查看评价
                </el-button>
                <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            class="pagination"
            v-model:current-page="query.current"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @change="loadData"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 加入购物车弹窗 -->
    <el-dialog v-model="cartDialogVisible" title="加入购物车" width="400px">
      <el-form :model="cartForm" ref="cartFormRef" label-width="80px">
        <el-form-item label="商品名称">
          <span class="form-text">{{ cartForm.productName }}</span>
        </el-form-item>
        <el-form-item label="库存">
          <el-tag :type="cartForm.productStock < 50 ? 'warning' : 'success'" size="small">
            {{ cartForm.productStock }} 件
          </el-tag>
        </el-form-item>
        <el-form-item label="购买数量" prop="quantity" :rules="[{ required: true, message: '请输入数量' }]">
          <el-input-number
            v-model="cartForm.quantity"
            :min="1"
            :max="cartForm.productStock"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cartDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddToCart" :loading="cartSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="500px">
      <el-form :model="form" ref="formRef" label-width="90px">
        <el-form-item label="商品名称" prop="name" :rules="[{ required: true, message: '请输入商品名称' }]">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category" :rules="[{ required: true, message: '请输入分类' }]">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :precision="2" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看评价弹窗 -->
    <el-dialog v-model="reviewDialogVisible" :title="`「${reviewProductName}」的评价" width="700px" top="5vh">
      <div v-if="reviewStats" class="review-stats">
        <div class="stats-overview">
          <div class="avg-rating">
            <span class="avg-number">{{ Number(reviewStats.avgRating).toFixed(1) }}</span>
            <el-rate v-model="avgRatingDisplay" disabled :max="5" :show-score="false" />
            <span class="total-count">{{ reviewStats.totalCount }} 条评价</span>
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

      <div class="review-filter">
        <el-select v-model="reviewQuery.rating" placeholder="按评分筛选" clearable style="width: 140px" @change="loadProductReviews">
          <el-option v-for="r in ratingOptions" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
      </div>

      <el-table :data="reviewList" v-loading="reviewLoading" stripe border empty-text="该商品暂无评价">
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
        <el-table-column prop="content" label="评价内容" min-width="280" />
        <el-table-column prop="createdTime" label="评价时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="review-pagination"
        v-model:current-page="reviewQuery.current"
        v-model:page-size="reviewQuery.size"
        :total="reviewTotal"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @change="loadProductReviews"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, ShoppingCart, ChatDotRound } from '@element-plus/icons-vue'
import { getProductPage, createProduct, updateProduct, deleteProduct, getCategoryStats } from '../api/product'
import { addToCart } from '../api/cart'
import { getReviewPage, getReviewStats } from '../api/review'

const loading = ref(false)
const statsLoading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const categoryStats = ref([])
const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const cartDialogVisible = ref(false)
const cartSubmitting = ref(false)
const cartFormRef = ref()

const query = reactive({ current: 1, size: 10, name: '', category: '' })
const form = reactive({ id: null, name: '', category: '', price: 0, stock: 0, description: '' })
const cartForm = reactive({ productId: null, productName: '', productStock: 0, quantity: 1 })

const reviewDialogVisible = ref(false)
const reviewLoading = ref(false)
const reviewList = ref([])
const reviewTotal = ref(0)
const reviewStats = ref(null)
const reviewProductName = ref('')
const currentProductId = ref(null)
const reviewQuery = reactive({ current: 1, size: 10, rating: null })

const ratingOptions = [
  { value: 5, label: '5星 - 非常好' },
  { value: 4, label: '4星 - 好' },
  { value: 3, label: '3星 - 一般' },
  { value: 2, label: '2星 - 差' },
  { value: 1, label: '1星 - 非常差' }
]

const avgRatingDisplay = computed(() => reviewStats.value ? Math.round(Number(reviewStats.value.avgRating)) : 0)

const getRatingCount = (r) => {
  if (!reviewStats.value) return 0
  const map = { 1: 'rating1Count', 2: 'rating2Count', 3: 'rating3Count', 4: 'rating4Count', 5: 'rating5Count' }
  return reviewStats.value[map[r]] || 0
}

const getRatingPercent = (r) => {
  if (!reviewStats.value || reviewStats.value.totalCount === 0) return 0
  return Math.round((getRatingCount(r) / reviewStats.value.totalCount) * 100)
}

const getRatingColor = (r) => {
  const colors = { 5: '#67C23A', 4: '#909399', 3: '#E6A23C', 2: '#F56C6C', 1: '#F56C6C' }
  return colors[r] || '#909399'
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProductPage({ ...query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getCategoryStats()
    categoryStats.value = res.data
    categories.value = res.data.map(s => s.category)
  } finally {
    statsLoading.value = false
  }
}

const resetQuery = () => {
  query.name = ''
  query.category = ''
  query.current = 1
  loadData()
}

const openCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', category: '', price: 0, stock: 0, description: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(form.id, form)
      ElMessage.success('更新成功（updatedTime 已自动填充）')
    } else {
      await createProduct(form)
      ElMessage.success('创建成功（createdTime/updatedTime 已自动填充）')
    }
    dialogVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」？`, '确认')
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  loadData()
  loadStats()
}

const openAddToCart = (row) => {
  cartForm.productId = row.id
  cartForm.productName = row.name
  cartForm.productStock = row.stock
  cartForm.quantity = 1
  cartDialogVisible.value = true
}

const handleAddToCart = async () => {
  await cartFormRef.value.validate()
  cartSubmitting.value = true
  try {
    await addToCart({ productId: cartForm.productId, quantity: cartForm.quantity })
    ElMessage.success('已加入购物车')
    cartDialogVisible.value = false
  } finally {
    cartSubmitting.value = false
  }
}

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
  if (!currentProductId.value) return
  reviewLoading.value = true
  try {
    const [reviewRes, statsRes] = await Promise.all([
      getReviewPage({ ...reviewQuery, productId: currentProductId.value }),
      getReviewStats(currentProductId.value)
    ])
    reviewList.value = reviewRes.data.records
    reviewTotal.value = reviewRes.data.total
    reviewStats.value = statsRes.data
  } finally {
    reviewLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.product-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.stats-card, .main-card { border-radius: 12px; height: 100%; }
.form-text { color: #303133; font-weight: 500; }

.card-title { font-weight: 600; margin-right: 8px; }

.stat-item { margin-bottom: 16px; }
.stat-category { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.stat-meta { display: flex; justify-content: space-between; font-size: 12px; color: #718096; margin-bottom: 6px; }

.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area { display: flex; align-items: center; gap: 8px; }

.price { color: #e6a23c; font-weight: 600; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.time-text { font-size: 13px; color: #718096; }

.review-stats { margin-bottom: 16px; padding: 16px; background: #fafafa; border-radius: 8px; }
.review-stats .stats-overview { display: flex; gap: 32px; align-items: flex-start; }
.review-stats .avg-rating { display: flex; flex-direction: column; align-items: center; min-width: 140px; }
.review-stats .avg-number { font-size: 40px; font-weight: 700; color: #e6a23c; line-height: 1; }
.review-stats .avg-rating :deep(.el-rate) { margin: 6px 0; }
.review-stats .total-count { font-size: 12px; color: #909399; }
.review-stats .rating-distribution { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.review-stats .rating-bar { display: flex; align-items: center; gap: 10px; }
.review-stats .rating-label { width: 36px; font-size: 12px; color: #606266; }
.review-stats .rating-bar :deep(.el-progress) { flex: 1; }
.review-stats .rating-count { width: 40px; font-size: 12px; color: #909399; text-align: right; }

.review-filter { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.review-pagination { margin-top: 16px; justify-content: flex-end; }
</style>
