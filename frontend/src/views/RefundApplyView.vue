<template>
  <div class="refund-apply-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">用户退款申请</el-tag>
        <el-tag size="small" type="warning" class="ml-8">事务控制</el-tag>
        <el-tag size="small" type="danger" class="ml-8">库存回滚</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">申请退款</span>
          <el-button link @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回订单详情
          </el-button>
        </div>
      </template>

      <el-descriptions :column="1" border size="small" class="order-info">
        <el-descriptions-item label="订单号">
          #{{ orderInfo.id }}
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span class="amount">¥{{ Number(orderInfo.totalAmount).toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusTagType(orderInfo.status)" size="small">
            {{ orderInfo.statusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品信息">
          <div v-for="item in orderInfo.items" :key="item.id" class="order-item">
            <span>{{ item.productName }}</span>
            <span class="quantity">x{{ item.quantity }}</span>
            <span class="price">¥{{ Number(item.price).toFixed(2) }}</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider>退款信息</el-divider>

      <el-form :model="form" ref="formRef" label-width="100px" class="refund-form">
        <el-form-item label="退款类型" prop="refundType" :rules="[{ required: true, message: '请选择退款类型' }]">
          <el-radio-group v-model="form.refundType">
            <el-radio :value="1">全额退款</el-radio>
            <el-radio :value="2">部分退款</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="退款金额" prop="refundAmount" :rules="[
          { required: true, message: '请输入退款金额' },
          { validator: validateAmount, trigger: 'blur' }
        ]">
          <el-input-number
            v-model="form.refundAmount"
            :min="0.01"
            :max="Number(orderInfo.totalAmount)"
            :step="1"
            :precision="2"
            style="width: 200px"
          />
          <span class="tip">最多可退 ¥{{ Number(orderInfo.totalAmount).toFixed(2) }}</span>
        </el-form-item>

        <el-form-item label="退款原因" prop="refundReason" :rules="[{ required: true, message: '请选择退款原因' }]">
          <el-select v-model="form.refundReason" placeholder="请选择退款原因" style="width: 300px">
            <el-option label="商品质量问题" value="商品质量问题" />
            <el-option label="商品与描述不符" value="商品与描述不符" />
            <el-option label="发错货/漏发货" value="发错货/漏发货" />
            <el-option label="不想要了/拍错了" value="不想要了/拍错了" />
            <el-option label="其他原因" value="其他原因" />
          </el-select>
        </el-form-item>

        <el-form-item label="详细说明" prop="refundDesc">
          <el-input
            v-model="form.refundDesc"
            type="textarea"
            :rows="4"
            placeholder="请详细描述退款原因（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="凭证图片" prop="proofImages">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :limit="3"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="tip">最多上传3张图片，支持jpg、png格式（演示：不上传实际文件）</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交申请
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { getOrderDetail } from '../api/order'
import { applyRefund } from '../api/refund'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const orderInfo = ref({ items: [] })

const form = reactive({
  orderId: route.params.orderId,
  refundType: 1,
  refundAmount: null,
  refundReason: '',
  refundDesc: '',
  proofImages: ''
})

const statusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger'][s] || ''

const validateAmount = (rule, value, callback) => {
  if (value <= 0) {
    callback(new Error('退款金额必须大于0'))
  } else if (value > Number(orderInfo.value.totalAmount)) {
    callback(new Error('退款金额不能超过订单金额'))
  } else {
    callback()
  }
}

const handleFileChange = (file, files) => {
  form.proofImages = files.map(f => f.name).join(',')
}

const handleFileRemove = (file, files) => {
  form.proofImages = files.map(f => f.name).join(',')
}

const loadOrderDetail = async () => {
  const res = await getOrderDetail(route.params.orderId)
  orderInfo.value = res.data
  form.refundAmount = Number(res.data.totalAmount)
}

const handleSubmit = async () => {
  await formRef.value.validate()
  
  try {
    await ElMessageBox.confirm(
      `确认提交退款申请？退款金额：¥${form.refundAmount.toFixed(2)}`,
      '确认提交',
      { type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await applyRefund(form)
    ElMessage.success('退款申请提交成功，请等待管理员审核')
    router.push(`/orders/${form.orderId}`)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push(`/orders/${form.orderId}`)
}

onMounted(() => {
  if (route.params.orderId) {
    loadOrderDetail()
  } else {
    router.push('/orders')
  }
})
</script>

<style scoped>
.refund-apply-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 16px; font-weight: 600; }
.order-info { margin-top: 16px; }
.order-item { display: flex; gap: 16px; padding: 4px 0; }
.order-item .quantity { color: #718096; }
.order-item .price { color: #e6a23c; font-weight: 600; }
.amount { color: #e6a23c; font-weight: 700; font-size: 18px; }
.refund-form { margin-top: 16px; }
.tip { margin-left: 12px; color: #718096; font-size: 13px; }
</style>
