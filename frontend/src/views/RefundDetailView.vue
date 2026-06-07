<template>
  <div class="refund-detail-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">退款单详情</span>
          <el-button link @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <div v-if="refundDetail">
        <el-steps :active="refundDetail.status + 1" finish-status="success" class="refund-steps">
          <el-step title="提交申请" :description="formatTime(refundDetail.createdTime)" />
          <el-step
            title="审核"
            :description="refundDetail.status > 0 ? formatTime(refundDetail.auditTime) : '待审核'"
          />
          <el-step
            v-if="refundDetail.status === 1"
            title="退款完成"
            description="款项已原路退回"
          />
          <el-step
            v-else-if="refundDetail.status === 2"
            title="审核拒绝"
            :description="refundDetail.auditRemark"
          />
          <el-step
            v-else-if="refundDetail.status === 3"
            title="已取消"
            description="用户取消申请"
          />
        </el-steps>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-card shadow="never" class="sub-card">
              <template #header>
                <span class="sub-title">退款信息</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="退款单号">
                  {{ refundDetail.refundNo }}
                </el-descriptions-item>
                <el-descriptions-item label="退款金额">
                  <span class="amount">¥{{ Number(refundDetail.refundAmount).toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="退款类型">
                  <el-tag size="small">{{ refundDetail.refundTypeLabel }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="退款原因">
                  {{ refundDetail.refundReason }}
                </el-descriptions-item>
                <el-descriptions-item label="详细说明" v-if="refundDetail.refundDesc">
                  {{ refundDetail.refundDesc }}
                </el-descriptions-item>
                <el-descriptions-item label="申请时间">
                  {{ formatTime(refundDetail.createdTime) }}
                </el-descriptions-item>
                <el-descriptions-item label="退款状态">
                  <el-tag :type="statusTagType(refundDetail.status)" size="small">
                    {{ refundDetail.statusLabel }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="never" class="sub-card">
              <template #header>
                <span class="sub-title">关联订单</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="订单号">
                  <el-link type="primary" @click="goOrderDetail(refundDetail.orderId)">
                    #{{ refundDetail.orderId }}
                  </el-link>
                </el-descriptions-item>
                <el-descriptions-item label="订单金额">
                  ¥{{ Number(refundDetail.orderTotalAmount).toFixed(2) }}
                </el-descriptions-item>
                <el-descriptions-item label="订单状态">
                  <el-tag :type="orderStatusTagType(refundDetail.orderStatus)" size="small">
                    {{ refundDetail.orderStatusLabel }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="申请人">
                  {{ refundDetail.username }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never" class="sub-card" v-if="refundDetail.status > 0">
              <template #header>
                <span class="sub-title">审核信息</span>
              </template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="审核人">
                  {{ refundDetail.auditUsername || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="审核时间">
                  {{ formatTime(refundDetail.auditTime) }}
                </el-descriptions-item>
                <el-descriptions-item label="审核备注" v-if="refundDetail.auditRemark">
                  {{ refundDetail.auditRemark }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>

        <div class="action-bar" v-if="refundDetail.status === 0">
          <el-button
            v-if="!isAdmin"
            type="danger"
            @click="handleCancel"
          >
            取消申请
          </el-button>
          <el-button
            v-if="isAdmin"
            type="warning"
            @click="openAudit"
          >
            立即审核
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="退款审核" width="500px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核结果" prop="approved" :rules="[{ required: true, message: '请选择审核结果' }]">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :value="true">
              <el-tag type="success">审核通过</el-tag>
            </el-radio>
            <el-radio :value="false">
              <el-tag type="danger">审核拒绝</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="auditRemark" :rules="auditForm.approved === false ? [{ required: true, message: '请填写拒绝原因' }] : []">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="auditForm.approved === false ? '请填写拒绝原因' : '请填写审核备注（选填）'"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="auditForm.approved === true"
        type="warning"
        :closable="false"
      >
        审核通过后将变更订单状态、回滚库存并通知用户
      </el-alert>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAudit" :loading="submitting">确认审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getRefundDetail, cancelRefund, auditRefund } from '../api/refund'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isAdmin } = authStore
const loading = ref(false)
const submitting = ref(false)
const refundDetail = ref(null)
const auditDialogVisible = ref(false)

const auditForm = reactive({
  refundId: null,
  approved: null,
  auditRemark: ''
})

const statusTagType = (s) => ['warning', 'success', 'danger', 'info'][s] || ''
const orderStatusTagType = (s) => ['warning', 'primary', 'info', 'success', 'danger'][s] || ''

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getRefundDetail(route.params.id)
    refundDetail.value = res.data
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/refunds')
}

const goOrderDetail = (orderId) => {
  router.push(`/orders/${orderId}`)
}

const openAudit = () => {
  auditForm.refundId = refundDetail.value.id
  auditForm.approved = null
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

const handleAudit = async () => {
  if (auditForm.approved === null) {
    ElMessage.warning('请选择审核结果')
    return
  }
  if (auditForm.approved === false && !auditForm.auditRemark) {
    ElMessage.warning('请填写拒绝原因')
    return
  }

  try {
    await ElMessageBox.confirm(
      auditForm.approved ? '确认通过该退款申请？' : '确认拒绝该退款申请？',
      '确认审核',
      { type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await auditRefund(auditForm)
    ElMessage.success('审核操作成功')
    auditDialogVisible.value = false
    loadDetail()
  } finally {
    submitting.value = false
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确认取消该退款申请？', '确认取消', { type: 'warning' })
  } catch {
    return
  }

  try {
    await cancelRefund(route.params.id)
    ElMessage.success('退款申请已取消')
    loadDetail()
  } catch {}
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

onMounted(() => {
  if (route.params.id) {
    loadDetail()
  } else {
    router.push('/refunds')
  }
})
</script>

<style scoped>
.refund-detail-page { display: flex; flex-direction: column; gap: 16px; }
.main-card { border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 16px; font-weight: 600; }
.sub-card { margin-top: 16px; }
.sub-title { font-weight: 600; }
.amount { color: #e6a23c; font-weight: 700; font-size: 18px; }
.refund-steps { margin-bottom: 24px; }
.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
