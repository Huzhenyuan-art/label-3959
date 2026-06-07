<template>
  <div class="refund-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">退款审核管理</el-tag>
        <el-tag size="small" type="warning" class="ml-8">库存回滚</el-tag>
        <el-tag size="small" type="danger" class="ml-8">通知推送</el-tag>
        <el-tag size="small" type="info" class="ml-8">乐观锁</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-input v-model="query.refundNo" placeholder="退款单号搜索" clearable style="width: 200px" @keyup.enter="loadData" />
          <el-input v-model="query.username" placeholder="用户名搜索" clearable style="width: 150px" @keyup.enter="loadData" />
          <el-input v-model="query.orderId" placeholder="订单号搜索" clearable style="width: 120px" @keyup.enter="loadData" />
          <el-select v-model="query.status" placeholder="退款状态" clearable style="width: 130px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="退款ID" width="90" />
        <el-table-column prop="refundNo" label="退款单号" min-width="160" show-overflow-tooltip />
        <el-table-column label="用户信息" width="130">
          <template #default="{ row }">
            <div class="user-info">
              <span class="username">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ Number(row.refundAmount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundTypeLabel" label="退款类型" width="100" />
        <el-table-column prop="refundReason" label="退款原因" min-width="120" show-overflow-tooltip />
        <el-table-column prop="statusLabel" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联订单" width="150">
          <template #default="{ row }">
            <div>
              <el-tag size="small">{{ row.orderStatusLabel }}</el-tag>
              <div class="order-amount">¥{{ Number(row.orderTotalAmount).toFixed(2) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="auditUsername" label="审核人" width="100" />
        <el-table-column prop="createdTime" label="申请时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row.id)">详情</el-button>
            <el-button
              link
              type="warning"
              size="small"
              @click="openAudit(row)"
              :disabled="row.status !== 0"
              v-if="isAdmin"
            >
              审核
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
              :disabled="row.status !== 0"
              v-if="!isAdmin"
            >
              取消申请
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="退款审核" width="500px">
      <div v-if="auditForm.refundId" class="audit-content">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="退款单号">
            {{ currentRefund?.refundNo }}
          </el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="amount">¥{{ Number(currentRefund?.refundAmount).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退款原因">
            {{ currentRefund?.refundReason }}
          </el-descriptions-item>
          <el-descriptions-item label="详细说明" v-if="currentRefund?.refundDesc">
            {{ currentRefund?.refundDesc }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form :model="auditForm" label-width="100px" style="margin-top: 16px">
          <el-form-item label="审核结果" prop="approved" :rules="[{ required: true, message: '请选择审核结果' }]">
            <el-radio-group v-model="auditForm.approved">
              <el-radio :value="true" label="通过">
                <el-tag type="success">审核通过</el-tag>
              </el-radio>
              <el-radio :value="false" label="拒绝">
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
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>

        <el-alert
          v-if="auditForm.approved === true"
          type="warning"
          :closable="false"
          style="margin-top: 8px"
        >
          审核通过后将：1) 将订单状态改为"已取消" 2) 回滚商品库存 3) 释放库存预占 4) 通知用户
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAudit" :loading="submitting">确认审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getRefundPage, auditRefund, cancelRefund } from '../api/refund'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const { isAdmin } = authStore
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const auditDialogVisible = ref(false)
const currentRefund = ref(null)

const query = reactive({
  current: 1,
  size: 10,
  refundNo: '',
  orderId: null,
  username: '',
  status: null
})

const auditForm = reactive({
  refundId: null,
  approved: null,
  auditRemark: ''
})

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '审核通过', value: 1 },
  { label: '审核拒绝', value: 2 },
  { label: '已取消', value: 3 }
]

const statusTagType = (s) => ['warning', 'success', 'danger', 'info'][s] || ''

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...query,
      status: query.status ?? undefined,
      orderId: query.orderId || undefined
    }
    const res = await getRefundPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.refundNo = ''
  query.orderId = null
  query.username = ''
  query.status = null
  query.current = 1
  loadData()
}

const goDetail = (id) => router.push(`/refunds/${id}`)

const openAudit = (row) => {
  currentRefund.value = row
  auditForm.refundId = row.id
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

  const confirmText = auditForm.approved
    ? '确认通过该退款申请？审核通过后订单状态将变更为已取消，库存将回滚。'
    : `确认拒绝该退款申请？拒绝原因：${auditForm.auditRemark}`

  try {
    await ElMessageBox.confirm(confirmText, '确认审核', { type: 'warning' })
  } catch {
    return
  }

  submitting.value = true
  try {
    await auditRefund(auditForm)
    ElMessage.success('审核操作成功')
    auditDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消该退款申请？', '确认取消', { type: 'warning' })
  } catch {
    return
  }

  try {
    await cancelRefund(row.id)
    ElMessage.success('退款申请已取消')
    loadData()
  } catch {}
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

onMounted(loadData)
</script>

<style scoped>
.refund-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.main-card { border-radius: 12px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.user-info { display: flex; flex-direction: column; }
.username { font-weight: 600; font-size: 14px; }
.amount { color: #e6a23c; font-weight: 700; }
.order-amount { color: #e6a23c; font-size: 12px; margin-top: 4px; }
.time-text { font-size: 13px; color: #718096; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.audit-content .amount { color: #e6a23c; font-weight: 700; font-size: 18px; }
</style>
