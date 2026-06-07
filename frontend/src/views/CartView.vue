<template>
  <div class="cart-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">独立购物车数据表</el-tag>
        <el-tag size="small" type="warning" class="ml-8">用户权限隔离</el-tag>
        <el-tag size="small" class="ml-8">事务结算生成订单</el-tag>
        <el-tag size="small" type="info" class="ml-8">自动扣减库存</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="cart-header">
        <div class="header-left">
          <el-checkbox v-model="isAllSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">
            全选
          </el-checkbox>
          <span class="cart-count">共 {{ cartList.length }} 件商品</span>
        </div>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          删除选中
        </el-button>
      </div>

      <div v-loading="loading" class="cart-content">
        <el-empty v-if="cartList.length === 0" description="购物车空空如也，去商品列表添加商品吧">
          <el-button type="primary" @click="goToProducts">去购物</el-button>
        </el-empty>

        <div v-else class="cart-items">
          <div
            v-for="item in cartList"
            :key="item.id"
            class="cart-item"
            :class="{ 'selected': selectedIds.includes(item.id) }"
          >
            <el-checkbox
              :model-value="selectedIds.includes(item.id)"
              @change="(val) => handleSelectItem(item.id, val)"
              class="item-checkbox"
            />

            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <el-tag size="small" type="info">{{ item.productCategory }}</el-tag>
            </div>

            <div class="item-price">
              <span class="price">¥{{ Number(item.productPrice).toFixed(2) }}</span>
            </div>

            <div class="item-quantity">
              <el-button
                size="small"
                :icon="Minus"
                :disabled="item.quantity <= 1"
                @click="handleUpdateQuantity(item, item.quantity - 1)"
              />
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="item.productStock"
                size="small"
                :controls="false"
                style="width: 60px"
                @change="(val) => handleUpdateQuantity(item, val)"
              />
              <el-button
                size="small"
                :icon="Plus"
                :disabled="item.quantity >= item.productStock"
                @click="handleUpdateQuantity(item, item.quantity + 1)"
              />
              <span class="stock-tip">库存: {{ item.productStock }}</span>
            </div>

            <div class="item-total">
              <span class="total-price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
            </div>

            <el-button
              link
              type="danger"
              size="small"
              :icon="Delete"
              @click="handleDelete(item)"
              class="item-delete"
            />
          </div>
        </div>
      </div>

      <div v-if="cartList.length > 0" class="cart-footer">
        <div class="footer-left">
          <el-checkbox v-model="isAllSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">
            全选
          </el-checkbox>
          <span>已选 <span class="selected-count">{{ selectedIds.length }}</span> 件</span>
        </div>
        <div class="footer-right">
          <span class="total-label">合计：</span>
          <span class="total-amount">¥{{ selectedTotal.toFixed(2) }}</span>
          <el-button
            type="primary"
            size="large"
            :disabled="selectedIds.length === 0"
            :loading="checkoutLoading"
            @click="handleCheckout"
          >
            结算
          </el-button>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="checkoutDialogVisible" title="确认订单" width="500px">
      <div class="checkout-preview">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品明细">
            <div v-for="item in checkoutItems" :key="item.id" class="checkout-item">
              <span>{{ item.productName }}</span>
              <span>x {{ item.quantity }}</span>
              <span class="checkout-price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="订单总额">
            <span class="checkout-total">¥{{ selectedTotal.toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-form class="checkout-form" label-width="80px">
          <el-form-item label="备注">
            <el-input v-model="checkoutRemark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="checkoutDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkoutLoading" @click="confirmCheckout">
          提交订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Minus, Delete } from '@element-plus/icons-vue'
import {
  getMyCart,
  updateCartQuantity,
  removeFromCart,
  batchRemoveCart,
  checkoutCart
} from '../api/cart'

const router = useRouter()
const loading = ref(false)
const checkoutLoading = ref(false)
const cartList = ref([])
const selectedIds = ref([])
const checkoutDialogVisible = ref(false)
const checkoutRemark = ref('')

const isAllSelected = computed({
  get: () => cartList.value.length > 0 && selectedIds.value.length === cartList.value.length,
  set: (val) => {}
})

const isIndeterminate = computed(() => {
  return selectedIds.value.length > 0 && selectedIds.value.length < cartList.value.length
})

const selectedTotal = computed(() => {
  return cartList.value
    .filter(item => selectedIds.value.includes(item.id))
    .reduce((sum, item) => sum + item.productPrice * item.quantity, 0)
})

const checkoutItems = computed(() => {
  return cartList.value.filter(item => selectedIds.value.includes(item.id))
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyCart()
    cartList.value = res.data
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

const handleSelectAll = (checked) => {
  if (checked) {
    selectedIds.value = cartList.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

const handleUpdateQuantity = async (item, quantity) => {
  if (quantity < 1 || quantity > item.productStock) {
    return
  }
  try {
    await updateCartQuantity(item.id, { quantity })
    item.quantity = quantity
    ElMessage.success('数量已更新')
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

const handleDelete = async (item) => {
  await ElMessageBox.confirm(`确定删除商品「${item.productName}」？`, '确认')
  await removeFromCart(item.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 件商品？`, '确认')
  await batchRemoveCart({ ids: selectedIds.value })
  ElMessage.success('删除成功')
  loadData()
}

const handleCheckout = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  const invalidItems = cartList.value.filter(
    item => selectedIds.value.includes(item.id) && item.quantity > item.productStock
  )
  if (invalidItems.length > 0) {
    ElMessage.warning(`商品【${invalidItems[0].productName}】库存不足`)
    return
  }
  checkoutRemark.value = ''
  checkoutDialogVisible.value = true
}

const confirmCheckout = async () => {
  checkoutLoading.value = true
  try {
    const res = await checkoutCart({
      cartIds: selectedIds.value,
      remark: checkoutRemark.value
    })
    ElMessage.success('订单创建成功！')
    checkoutDialogVisible.value = false
    router.push(`/orders/${res.data.id}`)
  } finally {
    checkoutLoading.value = false
  }
}

const goToProducts = () => {
  router.push('/products')
}

onMounted(loadData)
</script>

<style scoped>
.cart-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.cart-count { color: #909399; font-size: 14px; }

.cart-content { min-height: 300px; }

.cart-items { display: flex; flex-direction: column; gap: 12px; }

.cart-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  gap: 16px;
  transition: all 0.2s;
}
.cart-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
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
}

.item-price {
  width: 100px;
  text-align: center;
}
.price {
  color: #e6a23c;
  font-weight: 600;
  font-size: 16px;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 260px;
}
.stock-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.item-total {
  width: 120px;
  text-align: center;
}
.total-price {
  color: #f56c6c;
  font-weight: 700;
  font-size: 18px;
}

.item-delete { flex-shrink: 0; }

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-top: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.footer-left { display: flex; align-items: center; gap: 16px; }
.selected-count {
  color: #409eff;
  font-weight: 600;
  margin: 0 4px;
}
.footer-right { display: flex; align-items: center; gap: 16px; }
.total-label { font-size: 14px; color: #606266; }
.total-amount {
  color: #f56c6c;
  font-weight: 700;
  font-size: 22px;
}

.checkout-preview { padding: 8px 0; }
.checkout-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e4e7ed;
}
.checkout-item:last-child { border-bottom: none; }
.checkout-price {
  color: #e6a23c;
  font-weight: 600;
}
.checkout-total {
  color: #f56c6c;
  font-weight: 700;
  font-size: 20px;
}
.checkout-form { margin-top: 16px; }
</style>
