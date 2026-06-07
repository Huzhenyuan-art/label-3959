<template>
  <div class="order-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">XML 多表联查（order+user）</el-tag>
        <el-tag size="small" type="warning" class="ml-8">动态 SQL &lt;if&gt;/&lt;where&gt;</el-tag>
        <el-tag size="small" class="ml-8">乐观锁状态更新 @Version</el-tag>
        <el-tag size="small" type="danger" class="ml-8">事务 @Transactional</el-tag>
        <el-tag size="small" type="info" class="ml-8">分页 + 联查</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input v-model="query.username" placeholder="用户名搜索" clearable style="width:180px" @keyup.enter="loadData" />
          <el-select v-model="query.status" placeholder="订单状态" clearable style="width:140px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-date-picker
            v-model="query.createdTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:280px"
            @change="loadData"
          />
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建订单</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="订单ID" width="90" />
        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <div class="user-info">
              <span class="username">{{ row.username }}</span>
              <span class="email">{{ row.userEmail }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ Number(row.totalAmount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusLabel" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本(乐观锁)" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row.id)">查看详情</el-button>
            <el-button link type="warning" size="small" @click="openUpdateStatus(row)" :disabled="row.status === 3 || row.status === 4">
              更新状态
            </el-button>
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

    <!-- 新建订单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建订单（演示事务+批量插入明细）" width="560px">
      <el-form :model="createForm" ref="createFormRef" label-width="100px">
        <el-form-item v-if="isAdmin" label="用户ID" prop="userId" :rules="[{ required: true, message: '请输入用户ID' }]">
          <el-input-number v-model="createForm.userId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" />
        </el-form-item>
        <el-divider>订单明细（演示批量插入）</el-divider>
        <div v-for="(item, idx) in createForm.items" :key="idx" class="item-row">
          <el-select v-model="item.productId" placeholder="选择商品" style="width:180px" @change="(v) => onProductChange(v, idx)">
            <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" style="width:100px" placeholder="数量" />
          <span class="item-price" v-if="item.price">¥{{ item.price }}</span>
          <el-button link type="danger" :icon="Delete" @click="removeItem(idx)" v-if="createForm.items.length > 1" />
        </div>
        <el-button text type="primary" :icon="Plus" @click="addItem" style="margin-top:8px">添加明细</el-button>
        <el-divider />
        <el-descriptions :column="1" size="small">
          <el-descriptions-item label="预计总金额">
            <span class="amount">¥{{ calcTotal }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">提交订单</el-button>
      </template>
    </el-dialog>

    <!-- 更新状态弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="更新订单状态（乐观锁演示）" width="400px">
      <el-alert type="warning" :closable="false" style="margin-bottom:16px">
        当前版本号：v{{ statusForm.version }}，提交后版本号将自动 +1（乐观锁）
      </el-alert>
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" style="width:100%">
            <el-option v-for="s in statusOptions.filter(s => s.value > statusForm.currentStatus)" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateStatus" :loading="submitting">确认更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, Delete } from '@element-plus/icons-vue'
import { getOrderPage, createOrder, updateOrderStatus } from '../api/order'
import { getProductList } from '../api/product'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const { isAdmin, userInfo } = authStore
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const productList = ref([])
const createDialogVisible = ref(false)
const statusDialogVisible = ref(false)
const createFormRef = ref()

const query = reactive({ current: 1, size: 10, username: '', status: null, createdTimeRange: [] })

const createForm = reactive({
  userId: null,
  remark: '',
  items: [{ productId: null, productName: '', quantity: 1, price: null }]
})

const statusForm = reactive({ id: null, status: 1, version: null, currentStatus: 0 })

const statusOptions = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已发货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '退款中', value: 5 }
]

const statusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger', 'warning'][s] || ''

const calcTotal = computed(() =>
  createForm.items.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 1), 0).toFixed(2)
)

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...query,
      status: query.status ?? undefined,
      createdTimeStart: query.createdTimeRange?.[0] ?? undefined,
      createdTimeEnd: query.createdTimeRange?.[1] ?? undefined
    }
    delete params.createdTimeRange
    const res = await getOrderPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.username = ''
  query.status = null
  query.createdTimeRange = []
  query.current = 1
  loadData()
}

const goDetail = (id) => router.push(`/orders/${id}`)

const openCreate = async () => {
  if (!productList.value.length) {
    const res = await getProductList()
    productList.value = res.data
  }
  createForm.userId = isAdmin ? null : userInfo.id
  createForm.remark = ''
  createForm.items = [{ productId: null, productName: '', quantity: 1, price: null }]
  createDialogVisible.value = true
}

const onProductChange = (productId, idx) => {
  const p = productList.value.find(p => p.id === productId)
  if (p) {
    createForm.items[idx].productName = p.name
    createForm.items[idx].price = p.price
  }
}

const addItem = () => {
  createForm.items.push({ productId: null, productName: '', quantity: 1, price: null })
}

const removeItem = (idx) => {
  createForm.items.splice(idx, 1)
}

const handleCreate = async () => {
  await createFormRef.value.validate()
  submitting.value = true
  try {
    await createOrder({
      order: { userId: createForm.userId, remark: createForm.remark },
      items: createForm.items.map(i => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity,
        price: i.price
      }))
    })
    ElMessage.success('订单创建成功（事务已提交，明细批量写入）')
    createDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const openUpdateStatus = (row) => {
  statusForm.id = row.id
  statusForm.version = row.version
  statusForm.currentStatus = row.status
  statusForm.status = row.status + 1
  statusDialogVisible.value = true
}

const handleUpdateStatus = async () => {
  submitting.value = true
  try {
    await updateOrderStatus(statusForm.id, { status: statusForm.status, version: statusForm.version })
    ElMessage.success('状态更新成功（乐观锁版本号已自动 +1）')
    statusDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

onMounted(loadData)
</script>

<style scoped>
.order-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area { display: flex; align-items: center; gap: 8px; }
.user-info { display: flex; flex-direction: column; }
.username { font-weight: 600; font-size: 14px; }
.email { font-size: 12px; color: #718096; }
.amount { color: #e6a23c; font-weight: 700; }
.time-text { font-size: 13px; color: #718096; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.item-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.item-price { color: #e6a23c; font-weight: 600; min-width: 80px; }
</style>
