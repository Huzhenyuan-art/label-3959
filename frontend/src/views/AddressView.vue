<template>
  <div class="address-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small" type="success">收货地址管理</el-tag>
        <el-tag size="small" type="warning" class="ml-8">默认地址设置</el-tag>
        <el-tag size="small" class="ml-8">用户权限隔离</el-tag>
        <el-tag size="small" type="info" class="ml-8">逻辑删除</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3>收货地址管理</h3>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          新增地址
        </el-button>
      </div>

      <div v-loading="loading" class="address-list">
        <el-empty v-if="addressList.length === 0" description="暂无收货地址，点击上方按钮添加">
          <el-button type="primary" @click="openCreateDialog">添加地址</el-button>
        </el-empty>

        <div v-else class="address-items">
          <div
            v-for="address in addressList"
            :key="address.id"
            class="address-item"
            :class="{ 'is-default': address.isDefault === 1 }"
          >
            <div class="address-header">
              <div class="receiver-info">
                <span class="receiver-name">{{ address.receiverName }}</span>
                <span class="receiver-phone">{{ address.receiverPhone }}</span>
                <el-tag v-if="address.isDefault === 1" type="danger" size="small" class="default-tag">
                  默认
                </el-tag>
              </div>
              <div class="address-actions">
                <el-button
                  v-if="address.isDefault !== 1"
                  size="small"
                  type="primary"
                  link
                  @click="handleSetDefault(address.id)"
                >
                  设为默认
                </el-button>
                <el-button size="small" type="primary" link @click="openEditDialog(address)">
                  编辑
                </el-button>
                <el-button size="small" type="danger" link @click="handleDelete(address)">
                  删除
                </el-button>
              </div>
            </div>
            <div class="address-detail">
              {{ buildFullAddress(address) }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑收货地址' : '新增收货地址'"
      width="520px"
    >
      <el-form :model="form" ref="formRef" label-width="100px">
        <el-form-item
          label="收件人"
          prop="receiverName"
          :rules="[{ required: true, message: '请输入收件人姓名' }]"
        >
          <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" maxlength="50" />
        </el-form-item>

        <el-form-item
          label="手机号"
          prop="receiverPhone"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
        >
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="请输入省份" maxlength="50" />
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" maxlength="50" />
        </el-form-item>

        <el-form-item label="区县" prop="district">
          <el-input v-model="form.district" placeholder="请输入区县" maxlength="50" />
        </el-form-item>

        <el-form-item
          label="详细地址"
          prop="detailAddress"
          :rules="[{ required: true, message: '请输入详细地址' }]"
        >
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、门牌号等"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
          <span class="form-tip">设置为默认地址后，下单时将自动选中</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMyAddresses,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '../api/address'

const loading = ref(false)
const submitting = ref(false)
const addressList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyAddresses()
    addressList.value = res.data
  } finally {
    loading.value = false
  }
}

const buildFullAddress = (address) => {
  let full = ''
  if (address.province) full += address.province
  if (address.city) full += address.city
  if (address.district) full += address.district
  full += address.detailAddress
  return full
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: 0
  })
}

const openCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (address) => {
  isEdit.value = true
  Object.assign(form, { ...address })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAddress(form.id, form)
      ElMessage.success('地址更新成功')
    } else {
      await createAddress(form)
      ElMessage.success('地址创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (id) => {
  await ElMessageBox.confirm('确定将该地址设为默认地址？', '确认')
  await setDefaultAddress(id)
  ElMessage.success('已设为默认地址')
  loadData()
}

const handleDelete = async (address) => {
  const message = address.isDefault === 1
    ? '该地址是默认地址，删除后系统将自动设置其他地址为默认，确定删除？'
    : '确定删除该收货地址？'
  await ElMessageBox.confirm(message, '确认删除', { type: 'warning' })
  await deleteAddress(address.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.address-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-alert {
  border-radius: 8px;
}

.ml-8 {
  margin-left: 8px;
}

.main-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.address-list {
  min-height: 200px;
}

.address-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-item {
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.2s;
}

.address-item:hover {
  border-color: #409eff;
}

.address-item.is-default {
  border-color: #f56c6c;
  background: #fef0f0;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.receiver-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.receiver-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.receiver-phone {
  font-size: 14px;
  color: #606266;
}

.default-tag {
  margin-left: 8px;
}

.address-actions {
  display: flex;
  gap: 8px;
}

.address-detail {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
