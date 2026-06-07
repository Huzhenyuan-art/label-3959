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

    <el-dialog v-model="checkoutDialogVisible" title="确认订单" width="560px">
      <div class="checkout-preview">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品明细">
            <div v-for="item in checkoutItems" :key="item.id" class="checkout-item">
              <span>{{ item.productName }}</span>
              <span>x {{ item.quantity }}</span>
              <span class="checkout-price">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="商品总额">
            <span class="checkout-total">¥{{ selectedTotal.toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="address-section">
          <div class="address-section-title">
            <span>选择收货地址</span>
            <el-button type="primary" link size="small" @click="goToAddressManage">
              管理地址
            </el-button>
          </div>
          <div v-if="loadingAddresses" class="loading-text">加载中...</div>
          <div v-else-if="addressList.length === 0" class="no-address">
            <el-empty description="暂无收货地址，请先添加收货地址" :image-size="80">
              <el-button type="primary" @click="goToAddressManage">去添加</el-button>
            </el-empty>
          </div>
          <div v-else class="address-list">
            <div
              v-for="address in addressList"
              :key="address.id"
              class="address-item"
              :class="{ 'address-selected': selectedAddressId === address.id }"
              @click="selectedAddressId = address.id"
            >
              <div class="address-item-left">
                <div class="address-receiver">
                  <span class="receiver-name">{{ address.receiverName }}</span>
                  <span class="receiver-phone">{{ address.receiverPhone }}</span>
                  <el-tag v-if="address.isDefault === 1" type="danger" size="small" class="default-tag">
                    默认
                  </el-tag>
                </div>
                <div class="address-detail">{{ buildFullAddress(address) }}</div>
              </div>
              <div v-if="selectedAddressId === address.id" class="address-check">
                <el-icon color="#67c23a" size="20"><CircleCheck /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <div class="coupon-section">
          <div class="coupon-section-title">
            <span>选择优惠券</span>
            <span v-if="loadingCoupons" class="loading-text">加载中...</span>
          </div>
          <div v-if="!loadingCoupons && availableCoupons.length === 0" class="no-coupon">
            暂无可用优惠券
          </div>
          <div v-else class="coupon-list">
            <div
              v-for="coupon in availableCoupons"
              :key="coupon.id"
              class="coupon-item"
              :class="{ 'coupon-selected': selectedCouponId === coupon.id }"
              @click="selectedCouponId = selectedCouponId === coupon.id ? null : coupon.id"
            >
              <div class="coupon-item-left">
                <div class="coupon-type">{{ coupon.couponTypeDesc }}</div>
                <div class="coupon-value">
                  <span v-if="coupon.couponType === 1">
                    <small>¥</small>{{ Number(coupon.discountAmount).toFixed(0) }}
                  </span>
                  <span v-else>
                    {{ Number(coupon.discountRate * 10).toFixed(1) }}<small>折</small>
                  </span>
                </div>
                <div class="coupon-condition">
                  满{{ Number(coupon.minAmount).toFixed(0) }}可用
                </div>
              </div>
              <div class="coupon-item-right">
                <div class="coupon-name">{{ coupon.couponName }}</div>
                <div class="coupon-valid">
                  {{ formatDate(coupon.validStartTime) }} - {{ formatDate(coupon.validEndTime) }}
                </div>
              </div>
              <div v-if="selectedCouponId === coupon.id" class="coupon-check">
                <el-icon color="#67c23a" size="20"><CircleCheck /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <div class="price-summary">
          <div class="price-row">
            <span class="price-label">商品总额：</span>
            <span class="price-value">¥{{ selectedTotal.toFixed(2) }}</span>
          </div>
          <div class="price-row discount" v-if="discountAmount > 0">
            <span class="price-label">优惠券抵扣：</span>
            <span class="price-value">-¥{{ discountAmount.toFixed(2) }}</span>
          </div>
          <div class="price-row final">
            <span class="price-label">实付金额：</span>
            <span class="price-value">¥{{ finalAmount.toFixed(2) }}</span>
          </div>
        </div>

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
import { Plus, Minus, Delete, CircleCheck } from '@element-plus/icons-vue'
import {
  getMyCart,
  updateCartQuantity,
  removeFromCart,
  batchRemoveCart,
  checkoutCart,
  getAvailableCouponsForOrder
} from '../api/cart'
import { getMyAddresses, getDefaultAddress } from '../api/address'

const router = useRouter()
const loading = ref(false)
const checkoutLoading = ref(false)
const cartList = ref([])
const selectedIds = ref([])
const checkoutDialogVisible = ref(false)
const checkoutRemark = ref('')
const availableCoupons = ref([])
const selectedCouponId = ref(null)
const loadingCoupons = ref(false)
const addressList = ref([])
const selectedAddressId = ref(null)
const loadingAddresses = ref(false)

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

const selectedCoupon = computed(() => {
  return availableCoupons.value.find(c => c.id === selectedCouponId.value)
})

const discountAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  if (selectedCoupon.value.couponType === 1) {
    return Number(selectedCoupon.value.discountAmount) || 0
  } else {
    return selectedTotal.value * (1 - Number(selectedCoupon.value.discountRate))
  }
})

const finalAmount = computed(() => {
  return Math.max(0, selectedTotal.value - discountAmount.value)
})

const selectedAddress = computed(() => {
  return addressList.value.find(a => a.id === selectedAddressId.value)
})

const buildFullAddress = (address) => {
  if (!address) return ''
  let full = ''
  if (address.province) full += address.province
  if (address.city) full += address.city
  if (address.district) full += address.district
  full += address.detailAddress
  return full
}

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

const handleCheckout = async () => {
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
  selectedCouponId.value = null
  availableCoupons.value = []
  addressList.value = []
  selectedAddressId.value = null

  loadingCoupons.value = true
  loadingAddresses.value = true
  try {
    const [couponRes, addressRes] = await Promise.all([
      getAvailableCouponsForOrder(selectedTotal.value),
      getMyAddresses()
    ])
    availableCoupons.value = couponRes.data
    addressList.value = addressRes.data

    if (addressList.value.length > 0) {
      const defaultAddr = addressList.value.find(a => a.isDefault === 1)
      selectedAddressId.value = defaultAddr ? defaultAddr.id : addressList.value[0].id
    }
  } finally {
    loadingCoupons.value = false
    loadingAddresses.value = false
  }

  checkoutDialogVisible.value = true
}

const goToAddressManage = () => {
  checkoutDialogVisible.value = false
  router.push('/addresses')
}

const confirmCheckout = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  checkoutLoading.value = true
  try {
    const res = await checkoutCart({
      cartIds: selectedIds.value,
      remark: checkoutRemark.value,
      userCouponId: selectedCouponId.value,
      addressId: selectedAddressId.value
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

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
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

.coupon-section {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.coupon-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}
.loading-text {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.no-coupon {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
}
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.coupon-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.coupon-item:hover {
  border-color: #409eff;
}
.coupon-item.coupon-selected {
  border-color: #67c23a;
  background: #f0f9eb;
}
.coupon-item-left {
  width: 100px;
  padding-right: 12px;
  text-align: center;
  border-right: 1px dashed #e4e7ed;
}
.coupon-item .coupon-type {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.coupon-item .coupon-value {
  color: #f56c6c;
  font-weight: 700;
  font-size: 24px;
  line-height: 1;
  margin-bottom: 4px;
}
.coupon-item .coupon-value small {
  font-size: 14px;
}
.coupon-item .coupon-condition {
  font-size: 11px;
  color: #909399;
}
.coupon-item-right {
  flex: 1;
  padding-left: 12px;
}
.coupon-item-right .coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.coupon-item-right .coupon-valid {
  font-size: 12px;
  color: #909399;
}
.coupon-check {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.address-section {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.address-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}
.loading-text {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.no-address {
  padding: 20px;
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.address-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.address-item:hover {
  border-color: #409eff;
}
.address-item.address-selected {
  border-color: #67c23a;
  background: #f0f9eb;
}
.address-item-left {
  flex: 1;
  padding-right: 30px;
}
.address-receiver {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.address-receiver .receiver-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.address-receiver .receiver-phone {
  font-size: 14px;
  color: #606266;
}
.address-receiver .default-tag {
  margin-left: 8px;
}
.address-detail {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.address-check {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.price-summary {
  margin-top: 16px;
  padding: 16px;
  background: #fffbe6;
  border: 1px solid #faecd8;
  border-radius: 8px;
}
.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}
.price-row .price-label {
  font-size: 14px;
  color: #606266;
}
.price-row .price-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.price-row.discount .price-value {
  color: #67c23a;
}
.price-row.final {
  border-top: 1px dashed #e4e7ed;
  margin-top: 6px;
  padding-top: 12px;
}
.price-row.final .price-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.price-row.final .price-value {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}
</style>
