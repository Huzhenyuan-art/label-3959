<template>
  <div class="my-coupons-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">我的优惠券</el-tag>
        <el-tag size="small" type="warning" class="ml-8">自动过期处理</el-tag>
        <el-tag size="small" class="ml-8">按订单金额筛选可用券</el-tag>
      </template>
    </el-alert>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">我的优惠券</span>
              <div class="header-right">
                <el-input
                  v-model="orderAmount"
                  placeholder="输入订单金额，筛选可用券"
                  style="width: 200px"
                  clearable
                  @input="filterByOrderAmount"
                >
                  <template #prepend>¥</template>
                </el-input>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab" @tab-change="loadMyCoupons">
            <el-tab-pane label="可使用" name="0" />
            <el-tab-pane label="已使用" name="1" />
            <el-tab-pane label="已过期" name="2" />
          </el-tabs>

          <div v-loading="loading" class="coupon-list">
            <el-empty v-if="coupons.length === 0" :description="getEmptyDesc()" />
            <el-row :gutter="16">
              <el-col :span="8" v-for="coupon in coupons" :key="coupon.id" @click="viewDetail(coupon)">
                <div class="coupon-card" :class="getCouponClass(coupon)">
                  <div class="coupon-left">
                    <div class="coupon-type">{{ coupon.couponTypeDesc }}</div>
                    <div class="coupon-value">
                      <span v-if="coupon.couponType === 1" class="value">
                        <small>¥</small>{{ Number(coupon.discountAmount).toFixed(0) }}
                      </span>
                      <span v-else class="value">
                        {{ Number(coupon.discountRate * 10).toFixed(1) }}<small>折</small>
                      </span>
                    </div>
                    <div class="coupon-condition">
                      满{{ Number(coupon.minAmount).toFixed(0) }}可用
                    </div>
                  </div>
                  <div class="coupon-right">
                    <div class="coupon-name">{{ coupon.couponName }}</div>
                    <div class="coupon-code">券码: {{ coupon.couponCode }}</div>
                    <div class="coupon-valid">
                      有效期: {{ formatDate(coupon.validStartTime) }} - {{ formatDate(coupon.validEndTime) }}
                    </div>
                    <div class="coupon-status">
                      <el-tag :type="getStatusType(coupon.status)" size="small">
                        {{ coupon.statusDesc }}
                      </el-tag>
                    </div>
                  </div>
                  <div v-if="coupon.status === 0 && !isAvailableForOrder(coupon)" class="coupon-mask">
                    <span>未达到使用门槛</span>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="优惠券详情" width="500px">
      <div v-if="currentCoupon" class="coupon-detail">
        <div class="detail-card" :class="getCouponClass(currentCoupon)">
          <div class="detail-value">
            <span v-if="currentCoupon.couponType === 1">
              <small>¥</small>{{ Number(currentCoupon.discountAmount).toFixed(0) }}
            </span>
            <span v-else>
              {{ Number(currentCoupon.discountRate * 10).toFixed(1) }}<small>折</small>
            </span>
          </div>
          <div class="detail-condition">满{{ Number(currentCoupon.minAmount).toFixed(0) }}可用</div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="优惠券名称">{{ currentCoupon.couponName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentCoupon.couponTypeDesc }}</el-descriptions-item>
          <el-descriptions-item label="券码">{{ currentCoupon.couponCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentCoupon.status)" size="small">
              {{ currentCoupon.statusDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="有效期">
            {{ formatDate(currentCoupon.validStartTime) }} - {{ formatDate(currentCoupon.validEndTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="领取时间">{{ formatDateTime(currentCoupon.createdTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentCoupon.usedTime" label="使用时间">
            {{ formatDateTime(currentCoupon.usedTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentCoupon.orderId" label="关联订单">
            <el-link type="primary" @click="goToOrder(currentCoupon.orderId)">
              订单 #{{ currentCoupon.orderId }}
            </el-link>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyCoupons, getMyCouponDetail } from '../api/coupon'

const router = useRouter()

const activeTab = ref('0')
const loading = ref(false)
const detailVisible = ref(false)
const currentCoupon = ref(null)
const orderAmount = ref(null)

const allCoupons = ref([])
const coupons = ref([])

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDateTime = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const types = { 0: 'success', 1: 'info', 2: 'info', 3: 'danger' }
  return types[status] || 'info'
}

const getCouponClass = (coupon) => {
  const classes = []
  if (coupon.status === 1) classes.push('coupon-used')
  if (coupon.status === 2) classes.push('coupon-expired')
  if (coupon.status === 3) classes.push('coupon-invalid')
  return classes
}

const getEmptyDesc = () => {
  const descs = { 0: '暂无可用优惠券', 1: '暂无已使用优惠券', 2: '暂无已过期优惠券' }
  return descs[activeTab.value] || '暂无优惠券'
}

const isAvailableForOrder = (coupon) => {
  if (orderAmount.value == null || orderAmount.value === '') return true
  return Number(orderAmount.value) >= Number(coupon.minAmount)
}

const filterByOrderAmount = () => {
  if (activeTab.value !== '0' || orderAmount.value == null || orderAmount.value === '') {
    coupons.value = allCoupons.value
    return
  }
  const amount = Number(orderAmount.value)
  coupons.value = allCoupons.value.filter(c => amount >= Number(c.minAmount))
}

const loadMyCoupons = async () => {
  loading.value = true
  try {
    const status = activeTab.value === '0' ? 0 : activeTab.value === '1' ? 1 : 2
    const res = await getMyCoupons(status)
    allCoupons.value = res.data
    filterByOrderAmount()
  } finally {
    loading.value = false
  }
}

const viewDetail = async (coupon) => {
  try {
    const res = await getMyCouponDetail(coupon.id)
    currentCoupon.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const goToOrder = (orderId) => {
  detailVisible.value = false
  router.push(`/orders/${orderId}`)
}

onMounted(() => {
  loadMyCoupons()
})
</script>

<style scoped>
.my-coupons-page {
  padding: 16px;
}

.feature-alert {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.coupon-list {
  margin-top: 16px;
}

.coupon-card {
  display: flex;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  cursor: pointer;
  position: relative;
  transition: transform 0.2s;
}

.coupon-card:hover {
  transform: translateY(-2px);
}

.coupon-card.coupon-used {
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}

.coupon-card.coupon-expired,
.coupon-card.coupon-invalid {
  background: linear-gradient(135deg, #909399 0%, #73767a 100%);
  box-shadow: 0 4px 12px rgba(144, 147, 153, 0.2);
}

.coupon-left {
  width: 140px;
  padding: 20px 16px;
  color: #fff;
  text-align: center;
  border-right: 2px dashed rgba(255, 255, 255, 0.5);
}

.coupon-type {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.coupon-value {
  font-weight: bold;
  margin-bottom: 4px;
}

.coupon-value .value {
  font-size: 32px;
  line-height: 1;
}

.coupon-value .value small {
  font-size: 16px;
}

.coupon-condition {
  font-size: 12px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.coupon-code {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  font-family: monospace;
}

.coupon-valid {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.coupon-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  border-radius: 12px;
}

.coupon-detail {
  text-align: center;
}

.detail-card {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: #fff;
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.detail-card.coupon-used {
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
}

.detail-card.coupon-expired,
.detail-card.coupon-invalid {
  background: linear-gradient(135deg, #909399 0%, #73767a 100%);
}

.detail-value {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 8px;
}

.detail-value small {
  font-size: 24px;
}

.detail-condition {
  font-size: 14px;
  opacity: 0.9;
}

.ml-8 {
  margin-left: 8px;
}
</style>
