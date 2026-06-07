<template>
  <div class="coupon-center-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">优惠券模板管理</el-tag>
        <el-tag size="small" type="warning" class="ml-8">用户领取校验（限领次数/库存/有效期）</el-tag>
        <el-tag size="small" class="ml-8">事务一致性</el-tag>
      </template>
    </el-alert>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">优惠券中心</span>
              <div v-if="isAdmin">
                <el-button type="primary" :icon="Plus" @click="openCreateTemplate">创建券模板</el-button>
              </div>
            </div>
          </template>

          <div v-if="isAdmin" class="admin-tabs">
            <el-tabs v-model="activeTab" @tab-change="loadData">
              <el-tab-pane label="可领取优惠券" name="available" />
              <el-tab-pane label="全部模板" name="all" />
            </el-tabs>
          </div>

          <div v-if="isAdmin && activeTab === 'all'" class="template-list">
            <div class="toolbar">
              <el-input v-model="query.name" placeholder="优惠券名称" clearable style="width:180px" @keyup.enter="loadTemplates" />
              <el-select v-model="query.type" placeholder="类型" clearable style="width:120px">
                <el-option label="满减券" :value="1" />
                <el-option label="折扣券" :value="2" />
              </el-select>
              <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
                <el-option label="草稿" :value="0" />
                <el-option label="进行中" :value="1" />
                <el-option label="已过期" :value="2" />
                <el-option label="已停用" :value="3" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadTemplates">搜索</el-button>
            </div>

            <el-table :data="templateList" v-loading="templateLoading" stripe border>
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="name" label="名称" min-width="160" />
              <el-table-column prop="type" label="类型" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.type === 1 ? 'success' : 'warning'" size="small">
                    {{ row.type === 1 ? '满减券' : '折扣券' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="优惠" width="120">
                <template #default="{ row }">
                  <span v-if="row.type === 1" class="discount-text">
                    满{{ Number(row.minAmount).toFixed(0) }}减{{ Number(row.discountAmount).toFixed(0) }}
                  </span>
                  <span v-else class="discount-text">
                    {{ Number(row.discountRate * 10).toFixed(1) }}折
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="库存" width="140">
                <template #default="{ row }">
                  <span>{{ row.receivedCount }}/{{ row.totalCount }}</span>
                  <el-progress :percentage="Math.round(row.receivedCount / row.totalCount * 100)" :show-text="false" :stroke-width="6" />
                </template>
              </el-table-column>
              <el-table-column prop="perUserLimit" label="每人限领" width="90" align="center" />
              <el-table-column label="有效期" width="200">
                <template #default="{ row }">
                  <span v-if="row.validDays">{{ row.validDays }}天(领取后)</span>
                  <span v-else>
                    {{ formatDate(row.validStartTime) }} ~ {{ formatDate(row.validEndTime) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="toggleStatus(row)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-model:current-page="query.current"
              v-model:page-size="query.size"
              :total="templateTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadTemplates"
              @current-change="loadTemplates"
              class="pagination"
            />
          </div>

          <div v-else class="coupon-grid">
            <div v-loading="loading">
              <el-empty v-if="availableTemplates.length === 0" description="暂无可用优惠券" />
              <el-row :gutter="16">
                <el-col :span="8" v-for="template in availableTemplates" :key="template.id">
                  <div class="coupon-card" :class="{ 'coupon-disabled': template.receivedCount >= template.totalCount }">
                    <div class="coupon-left">
                      <div class="coupon-type">{{ template.type === 1 ? '满减券' : '折扣券' }}</div>
                      <div class="coupon-value">
                        <span v-if="template.type === 1" class="value">
                          <small>¥</small>{{ Number(template.discountAmount).toFixed(0) }}
                        </span>
                        <span v-else class="value">
                          {{ Number(template.discountRate * 10).toFixed(1) }}<small>折</small>
                        </span>
                      </div>
                      <div class="coupon-condition">
                        满{{ Number(template.minAmount).toFixed(0) }}可用
                      </div>
                    </div>
                    <div class="coupon-right">
                      <div class="coupon-name">{{ template.name }}</div>
                      <div class="coupon-valid">
                        <span v-if="template.validDays">领取后{{ template.validDays }}天有效</span>
                        <span v-else>{{ formatDate(template.validStartTime) }}前有效</span>
                      </div>
                      <div class="coupon-stock">
                        已领 {{ template.receivedCount }}/{{ template.totalCount }}
                      </div>
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Present"
                        @click="handleReceive(template)"
                        :disabled="template.receivedCount >= template.totalCount"
                      >
                        立即领取
                      </el-button>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="createDialogVisible" title="创建优惠券模板" width="600px">
      <el-form :model="templateForm" :rules="templateRules" ref="templateFormRef" label-width="120px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="templateForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="优惠券类型" prop="type">
          <el-radio-group v-model="templateForm.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="templateForm.type === 1" label="减免金额" prop="discountAmount">
          <el-input-number v-model="templateForm.discountAmount" :min="1" :precision="2" />
          <span class="form-tip">元</span>
        </el-form-item>
        <el-form-item v-if="templateForm.type === 2" label="折扣率" prop="discountRate">
          <el-input-number v-model="templateForm.discountRate" :min="0.01" :max="0.99" :step="0.01" :precision="2" />
          <span class="form-tip">（如0.8表示8折）</span>
        </el-form-item>
        <el-form-item label="满减门槛" prop="minAmount">
          <el-input-number v-model="templateForm.minAmount" :min="0" :precision="2" />
          <span class="form-tip">元（0表示无门槛）</span>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="templateForm.totalCount" :min="1" />
          <span class="form-tip">张</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number v-model="templateForm.perUserLimit" :min="1" />
          <span class="form-tip">张</span>
        </el-form-item>
        <el-form-item label="有效期设置" prop="validType">
          <el-radio-group v-model="validType">
            <el-radio value="fixed">固定时间段</el-radio>
            <el-radio value="days">领取后N天</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="validType === 'fixed'" label="开始时间" prop="validStartTime">
          <el-date-picker
            v-model="templateForm.validStartTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item v-if="validType === 'fixed'" label="结束时间" prop="validEndTime">
          <el-date-picker
            v-model="templateForm.validEndTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item v-if="validType === 'days'" label="有效天数" prop="validDays">
          <el-input-number v-model="templateForm.validDays" :min="1" />
          <span class="form-tip">天</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTemplate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Present } from '@element-plus/icons-vue'
import {
  createCouponTemplate,
  getCouponTemplates,
  getAvailableTemplates,
  updateTemplateStatus,
  receiveCoupon
} from '../api/coupon'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const activeTab = ref('available')
const loading = ref(false)
const templateLoading = ref(false)
const createDialogVisible = ref(false)
const templateFormRef = ref()
const validType = ref('fixed')

const availableTemplates = ref([])
const templateList = ref([])
const templateTotal = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  name: '',
  type: null,
  status: null
})

const templateForm = reactive({
  name: '',
  type: 1,
  discountAmount: null,
  discountRate: null,
  minAmount: 0,
  totalCount: 100,
  perUserLimit: 1,
  validStartTime: null,
  validEndTime: null,
  validDays: null,
  description: ''
})

const templateRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠券类型', trigger: 'change' }],
  discountAmount: [{ required: true, message: '请输入减免金额', trigger: 'blur' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '请输入每人限领数量', trigger: 'blur' }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'info', 3: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '草稿', 1: '进行中', 2: '已过期', 3: '已停用' }
  return texts[status] || '未知'
}

const loadData = () => {
  if (activeTab.value === 'all') {
    loadTemplates()
  } else {
    loadAvailableTemplates()
  }
}

const loadAvailableTemplates = async () => {
  loading.value = true
  try {
    const res = await getAvailableTemplates()
    availableTemplates.value = res.data
  } finally {
    loading.value = false
  }
}

const loadTemplates = async () => {
  templateLoading.value = true
  try {
    const res = await getCouponTemplates(query)
    templateList.value = res.data.records
    templateTotal.value = res.data.total
  } finally {
    templateLoading.value = false
  }
}

const openCreateTemplate = () => {
  templateForm.name = ''
  templateForm.type = 1
  templateForm.discountAmount = null
  templateForm.discountRate = null
  templateForm.minAmount = 0
  templateForm.totalCount = 100
  templateForm.perUserLimit = 1
  templateForm.validStartTime = null
  templateForm.validEndTime = null
  templateForm.validDays = null
  templateForm.description = ''
  validType.value = 'fixed'
  createDialogVisible.value = true
}

const handleCreateTemplate = async () => {
  await templateFormRef.value.validate()
  const data = { ...templateForm }
  if (validType.value === 'days') {
    data.validStartTime = null
    data.validEndTime = null
  } else {
    data.validDays = null
  }
  await createCouponTemplate(data)
  ElMessage.success('创建成功')
  createDialogVisible.value = false
  loadData()
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 3 : 1
  await ElMessageBox.confirm(
    `确定要${newStatus === 1 ? '启用' : '停用'}该优惠券模板吗？`,
    '确认操作',
    { type: 'warning' }
  )
  await updateTemplateStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadTemplates()
}

const handleReceive = async (template) => {
  try {
    await receiveCoupon(template.id)
    ElMessage.success('领取成功')
    loadAvailableTemplates()
  } catch (e) {
    ElMessage.error(e.message || '领取失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.coupon-center-page {
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

.admin-tabs {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}

.coupon-grid {
  margin-top: 16px;
}

.coupon-card {
  display: flex;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  position: relative;
}

.coupon-card.coupon-disabled {
  background: linear-gradient(135deg, #ccc 0%, #999 100%);
  box-shadow: 0 4px 12px rgba(153, 153, 153, 0.2);
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

.coupon-valid {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.coupon-stock {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.form-tip {
  margin-left: 8px;
  color: #999;
  font-size: 13px;
}

.ml-8 {
  margin-left: 8px;
}
</style>
