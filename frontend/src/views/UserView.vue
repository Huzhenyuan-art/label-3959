<template>
  <div class="user-page">
    <!-- 特性说明 -->
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        演示特性：<el-tag size="small">基础 CRUD</el-tag>
        <el-tag size="small" type="danger" class="ml-8">逻辑删除 @TableLogic</el-tag>
        <el-tag size="small" type="warning" class="ml-8">自动填充 @TableField</el-tag>
        <el-tag size="small" type="success" class="ml-8">乐观锁 @Version</el-tag>
        <el-tag size="small" type="info" class="ml-8">LambdaQueryWrapper 条件查询</el-tag>
        <el-tag size="small" class="ml-8">分页查询</el-tag>
        <el-tag size="small" class="ml-8">批量插入 saveBatch</el-tag>
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <!-- 视图切换栏 -->
      <div class="view-switch">
        <el-radio-group v-model="viewMode" size="large">
          <el-radio-button value="normal" @change="switchToNormalView">
            <el-icon><RefreshLeft /></el-icon>
            <span style="margin-left:4px">正常用户</span>
          </el-radio-button>
          <el-radio-button value="deleted" @change="switchToDeletedView">
            <el-icon><Delete /></el-icon>
            <span style="margin-left:4px">已删除用户</span>
          </el-radio-button>
        </el-radio-group>
        <el-tag v-if="isDeletedView" type="danger" size="large" style="margin-left:12px">
          当前查看的是已逻辑删除的用户
        </el-tag>
      </div>

      <!-- 搜索栏 -->
      <div class="toolbar">
        <div class="search-area">
          <el-input v-model="query.username" placeholder="搜索用户名" clearable style="width:200px" @keyup.enter="loadData" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-select v-model="query.role" placeholder="角色" clearable style="width:120px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
          <el-input-number v-model="query.minAge" placeholder="最小年龄" :min="1" :max="120" clearable style="width:120px" />
          <span style="color:#909399">-</span>
          <el-input-number v-model="query.maxAge" placeholder="最大年龄" :min="1" :max="120" clearable style="width:120px" />
          <el-button type="primary" @click="loadData" :icon="Search">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <div class="action-area">
          <el-button v-if="!isDeletedView" type="success" @click="showBatchDialog" :icon="CopyDocument">批量插入演示</el-button>
          <el-button v-if="!isDeletedView" type="primary" @click="openCreate" :icon="Plus">新增用户</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe border class="data-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本(乐观锁)" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间(自动填充)" min-width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isDeletedView ? 120 : 160" fixed="right">
          <template #default="{ row }">
            <template v-if="!isDeletedView">
              <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="handleDelete(row)">逻辑删除</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="success" link @click="handleRestore(row)">
                <el-icon><RefreshLeft /></el-icon>
                <span>恢复用户</span>
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户（含乐观锁）' : '新增用户（自动填充）'" width="480px">
      <el-form :model="form" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="ADMIN">管理员</el-radio>
            <el-radio value="USER" :disabled="isEditingSelf">普通用户</el-radio>
          </el-radio-group>
          <el-tooltip v-if="isEditingSelf" content="不能将自己降为普通用户" placement="right">
            <el-icon class="ml-4" style="color:#f56c6c"><QuestionFilled /></el-icon>
          </el-tooltip>
        </el-form-item>
        <el-form-item v-if="isEdit" label="当前版本">
          <el-tag type="warning">v{{ form.version }}（提交时自动 +1）</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量插入演示弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量插入演示 (saveBatch)" width="420px">
      <el-alert type="info" :closable="false" style="margin-bottom:16px">
        将一次性插入 3 条测试用户，演示 MyBatis Plus saveBatch 批量操作
      </el-alert>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="用户1">批量用户A / batch_a@test.com / 20岁</el-descriptions-item>
        <el-descriptions-item label="用户2">批量用户B / batch_b@test.com / 25岁</el-descriptions-item>
        <el-descriptions-item label="用户3">批量用户C / batch_c@test.com / 30岁</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchCreate" :loading="submitting">执行批量插入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, CopyDocument, QuestionFilled, Delete, RefreshLeft } from '@element-plus/icons-vue'
import { getUserPage, createUser, updateUser, deleteUser, batchCreateUsers, getDeletedUserPage, restoreUser } from '../api/user'
import { useAuthStore } from '../store/auth'

const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const viewMode = ref('normal')

const query = reactive({ current: 1, size: 10, username: '', status: null, role: null, minAge: null, maxAge: null })
const form = reactive({ id: null, username: '', email: '', age: 18, status: 1, role: 'USER', version: null })

const isEditingSelf = computed(() => {
  return isEdit.value && form.id === authStore.userInfo?.id
})

const isDeletedView = computed(() => viewMode.value === 'deleted')

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...query,
      status: query.status ?? undefined,
      role: query.role ?? undefined,
      minAge: query.minAge ?? undefined,
      maxAge: query.maxAge ?? undefined
    }
    const res = isDeletedView.value
      ? await getDeletedUserPage(params)
      : await getUserPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.username = ''
  query.status = null
  query.role = null
  query.minAge = null
  query.maxAge = null
  query.current = 1
  loadData()
}

const openCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', email: '', age: 18, status: 1, role: 'USER', version: null })
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEditingSelf.value && form.role === 'USER') {
    ElMessage.error('不能将自己降为普通用户')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser(form.id, form)
      ElMessage.success('更新成功（乐观锁版本号已自动 +1）')
    } else {
      await createUser(form)
      ElMessage.success('创建成功（createdTime 已自动填充）')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定逻辑删除用户「${row.username}」？（deleted 字段设为 1，数据仍保留）`, '确认')
  await deleteUser(row.id)
  ElMessage.success('逻辑删除成功，该用户在所有查询中已被过滤')
  loadData()
}

const showBatchDialog = () => { batchDialogVisible.value = true }

const handleBatchCreate = async () => {
  submitting.value = true
  try {
    const users = [
      { username: '批量用户A', email: 'batch_a@test.com', age: 20, status: 1, role: 'USER' },
      { username: '批量用户B', email: 'batch_b@test.com', age: 25, status: 1, role: 'USER' },
      { username: '批量用户C', email: 'batch_c@test.com', age: 30, status: 1, role: 'USER' }
    ]
    await batchCreateUsers(users)
    ElMessage.success('批量插入成功，saveBatch 一次写入 3 条')
    batchDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const switchToDeletedView = () => {
  viewMode.value = 'deleted'
  query.current = 1
  loadData()
}

const switchToNormalView = () => {
  viewMode.value = 'normal'
  query.current = 1
  loadData()
}

const handleRestore = async (row) => {
  await ElMessageBox.confirm(
    `确定恢复用户「${row.username}」？恢复后该用户可正常登录（deleted 字段设为 0，版本号自动 +1）`,
    '确认恢复',
    { type: 'warning' }
  )
  await restoreUser(row.id, { version: row.version })
  ElMessage.success('恢复成功（乐观锁版本号已自动 +1）')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.user-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.ml-8 { margin-left: 8px; }
.ml-4 { margin-left: 4px; }
.main-card { border-radius: 12px; }
.view-switch { display: flex; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area, .action-area { display: flex; align-items: center; gap: 8px; }
.data-table { border-radius: 8px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.time-text { font-size: 13px; color: #718096; }
</style>
