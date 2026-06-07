<template>
  <div class="favorite-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">独立收藏数据表</el-tag>
        <el-tag size="small" type="warning" class="ml-8">用户权限隔离</el-tag>
        <el-tag size="small" class="ml-8">联查商品实时价格与库存</el-tag>
        <el-tag size="small" type="info" class="ml-8">不可购买商品标识</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="favorite-header">
        <div class="header-left">
          <span class="favorite-count">共 {{ favoriteList.length }} 件收藏商品</span>
        </div>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          删除选中
        </el-button>
      </div>

      <div v-loading="loading" class="favorite-content">
        <el-empty v-if="favoriteList.length === 0" description="暂无收藏商品，去商品列表逛逛吧">
          <el-button type="primary" @click="goToProducts">去逛逛</el-button>
        </el-empty>

        <div v-else class="favorite-items">
          <div
            v-for="item in favoriteList"
            :key="item.id"
            class="favorite-item"
            :class="{ 'selected': selectedIds.includes(item.id), 'unavailable': !getCanPurchase(item) }"
          >
            <el-checkbox
              :model-value="selectedIds.includes(item.id)"
              @change="(val) => handleSelectItem(item.id, val)"
              class="item-checkbox"
            />

            <div class="item-info">
              <div class="item-name">
                {{ item.productName }}
                <el-tag v-if="!getCanPurchase(item)" type="danger" size="small" class="unavailable-tag">
                  暂时缺货
                </el-tag>
              </div>
              <el-tag size="small" type="info">{{ item.productCategory }}</el-tag>
              <div v-if="item.productDescription" class="item-desc">
                {{ item.productDescription }}
              </div>
            </div>

            <div class="item-price">
              <span class="price">¥{{ Number(item.productPrice).toFixed(2) }}</span>
            </div>

            <div class="item-stock">
              <el-tag
                :type="getAvailableStock(item) < 10 ? 'danger' : getAvailableStock(item) < 50 ? 'warning' : 'success'"
                size="small"
              >
                可用库存: {{ getAvailableStock(item) }}
              </el-tag>
              <span v-if="(item.productReservedStock || 0) > 0" class="reserved-tip">
                (预占: {{ item.productReservedStock || 0 }})
              </span>
            </div>

            <div class="item-time">
              <span class="time-text">{{ formatTime(item.createdTime) }}</span>
            </div>

            <div class="item-actions">
              <el-button
                link
                type="success"
                size="small"
                :icon="ShoppingCart"
                @click="handleAddToCart(item)"
                :disabled="!getCanPurchase(item)"
              >
                加入购物车
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(item)"
              >
                取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="cartDialogVisible" title="加入购物车" width="400px">
      <el-form :model="cartForm" ref="cartFormRef" label-width="80px">
        <el-form-item label="商品名称">
          <span class="form-text">{{ cartForm.productName }}</span>
        </el-form-item>
        <el-form-item label="总库存">
          <el-tag :type="cartForm.productStock < 50 ? 'warning' : 'success'" size="small">
            {{ cartForm.productStock }} 件
          </el-tag>
        </el-form-item>
        <el-form-item label="可用库存">
          <el-tag :type="cartForm.availableStock < 10 ? 'danger' : cartForm.availableStock < 50 ? 'warning' : 'success'" size="small">
            {{ cartForm.availableStock }} 件
          </el-tag>
        </el-form-item>
        <el-form-item label="预占库存">
          <el-tag type="info" size="small">
            {{ cartForm.reservedStock }} 件
          </el-tag>
        </el-form-item>
        <el-form-item label="购买数量" prop="quantity" :rules="[{ required: true, message: '请输入数量' }]">
          <el-input-number
            v-model="cartForm.quantity"
            :min="1"
            :max="cartForm.availableStock"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cartDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddToCart" :loading="cartSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, Delete } from '@element-plus/icons-vue'
import { getMyFavorites, removeFavorite, removeFavoriteByProductId } from '../api/favorite'
import { addToCart } from '../api/cart'

const router = useRouter()
const loading = ref(false)
const favoriteList = ref([])
const selectedIds = ref([])
const cartDialogVisible = ref(false)
const cartSubmitting = ref(false)
const cartFormRef = ref()

const cartForm = ref({
  productId: null,
  productName: '',
  productStock: 0,
  reservedStock: 0,
  availableStock: 0,
  quantity: 1
})

const getAvailableStock = (item) => {
  const total = item.productStock || 0
  const reserved = item.productReservedStock || 0
  return Math.max(0, total - reserved)
}

const getCanPurchase = (item) => {
  return getAvailableStock(item) > 0
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyFavorites()
    favoriteList.value = res.data
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const handleSelectItem = (id, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定取消收藏「${item.productName}」？`, '确认')
  await removeFavorite(item.id)
  ElMessage.success('已取消收藏')
  loadData()
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  await ElMessageBox.confirm(`确定取消选中的 ${selectedIds.value.length} 件商品？`, '确认')
  for (const id of selectedIds.value) {
    await removeFavorite(id)
  }
  ElMessage.success('取消收藏成功')
  loadData()
}

const handleAddToCart = (item) => {
  if (!getCanPurchase(item)) {
    ElMessage.warning('该商品暂时缺货，无法购买')
    return
  }
  cartForm.value.productId = item.productId
  cartForm.value.productName = item.productName
  cartForm.value.productStock = item.productStock
  cartForm.value.reservedStock = item.productReservedStock || 0
  cartForm.value.availableStock = getAvailableStock(item)
  cartForm.value.quantity = 1
  cartDialogVisible.value = true
}

const handleConfirmAddToCart = async () => {
  await cartFormRef.value.validate()
  cartSubmitting.value = true
  try {
    await addToCart({ productId: cartForm.value.productId, quantity: cartForm.value.quantity })
    ElMessage.success('已加入购物车')
    cartDialogVisible.value = false
  } finally {
    cartSubmitting.value = false
  }
}

const goToProducts = () => {
  router.push('/products')
}

onMounted(loadData)
</script>

<style scoped>
.favorite-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }

.favorite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}
.favorite-count { color: #909399; font-size: 14px; }

.favorite-content { min-height: 300px; }

.favorite-items { display: flex; flex-direction: column; gap: 12px; }

.favorite-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  gap: 16px;
  transition: all 0.2s;
}
.favorite-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}
.favorite-item.unavailable {
  opacity: 0.6;
  background: #f5f7fa;
}

.item-checkbox { flex-shrink: 0; }

.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
}
.unavailable-tag {
  flex-shrink: 0;
}
.item-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  width: 120px;
  text-align: center;
}
.price {
  color: #e6a23c;
  font-weight: 600;
  font-size: 18px;
}

.item-stock {
  width: 180px;
  text-align: center;
}
.reserved-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-left: 4px;
}

.item-time {
  width: 160px;
  text-align: center;
}
.time-text {
  font-size: 12px;
  color: #909399;
}

.item-actions {
  width: 180px;
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}

.form-text { color: #303133; font-weight: 500; }
</style>
