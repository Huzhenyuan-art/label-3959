<template>
  <div class="operation-log-page">
    <el-alert type="info" :closable="false" class="feature-alert">
      <template #title>
        操作审计日志：自动记录管理员对用户、商品、订单、优惠券等敏感操作，
        支持按操作类型、时间范围分页检索，写入过程异步不阻塞主业务
      </template>
    </el-alert>

    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="search-area">
          <el-select v-model="query.operationCategory" placeholder="操作分类" clearable style="width:140px" @change="loadData">
            <el-option v-for="cat in categories" :key="cat.category" :label="cat.category" :value="cat.category" />
          </el-select>
          <el-select v-model="query.operationType" placeholder="操作类型" clearable style="width:160px" @change="loadData" :disabled="!query.operationCategory">
            <el-option
              v-for="type in filteredTypes"
              :key="type.code"
              :label="type.desc"
              :value="type.code"
            />
          </el-select>
          <el-input v-model="query.operatorName" placeholder="操作人" clearable style="width:140px" @keyup.enter="loadData" />
          <el-select v-model="query.status" placeholder="操作状态" clearable style="width:120px" @change="loadData">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:320px"
            @change="handleDateChange"
          />
          <el-button type="primary" @click="loadData" :icon="Search">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border class="data-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="operationCategory" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.operationCategory }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationDesc" label="操作类型" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="operatorRole" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.operatorRole === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.operatorRole === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="110" />
        <el-table-column prop="targetId" label="目标ID" width="80" />
        <el-table-column prop="requestMethod" label="方法" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getMethodTagType(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUri" label="请求URI" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="120" />
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时(ms)" width="90" align="right" />
        <el-table-column prop="operationTime" label="操作时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.operationTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="loadData"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="900px" top="5vh">
      <el-descriptions :column="2" border v-if="currentLog" size="small">
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(currentLog.operationTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作分类">{{ currentLog.operationCategory }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog.operationDesc }} ({{ currentLog.operationType }})</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作人ID">{{ currentLog.operatorId }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="currentLog.operatorRole === 'ADMIN' ? 'danger' : 'primary'" size="small">
            {{ currentLog.operatorRole === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标类型 / ID">{{ currentLog.targetType }} / {{ currentLog.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">
          <el-tag :type="getMethodTagType(currentLog.requestMethod)" size="small">{{ currentLog.requestMethod }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求URI">{{ currentLog.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog.duration }} ms</el-descriptions-item>
        <el-descriptions-item label="User Agent" :span="2">
          <span class="text-muted">{{ currentLog.userAgent || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="currentLog?.errorMessage" class="error-section">
        <div class="section-title">错误信息</div>
        <div class="error-content">{{ currentLog.errorMessage }}</div>
      </div>

      <div v-if="currentLog?.requestParams" class="params-section">
        <div class="section-title">请求参数</div>
        <pre class="json-content">{{ formatJson(currentLog.requestParams) }}</pre>
      </div>

      <el-tabs v-if="currentLog && (currentLog.beforeData || currentLog.afterData)" class="data-tabs">
        <el-tab-pane label="变更前数据" name="before">
          <pre v-if="currentLog.beforeData" class="json-content">{{ formatJson(currentLog.beforeData) }}</pre>
          <el-empty v-else description="无变更前数据" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="变更后数据" name="after">
          <pre v-if="currentLog.afterData" class="json-content">{{ formatJson(currentLog.afterData) }}</pre>
          <el-empty v-else description="无变更后数据" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="数据对比" name="diff">
          <div v-if="currentLog.beforeData && currentLog.afterData">
            <json-diff-viewer :old-data="currentLog.beforeData" :new-data="currentLog.afterData" />
          </div>
          <el-empty v-else description="数据对比需要变更前后数据都存在" :image-size="80" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getOperationLogPage, getOperationTypes, getOperationCategories } from '../api/operationLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref(null)
const types = ref([])
const categories = ref([])
const dateRange = ref([])

const query = reactive({
  current: 1,
  size: 10,
  operationType: '',
  operationCategory: '',
  operatorName: '',
  targetId: null,
  targetType: '',
  startTime: null,
  endTime: null,
  status: null
})

const filteredTypes = computed(() => {
  if (!query.operationCategory) return []
  return types.value.filter(t => t.category === query.operationCategory)
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...query,
      operationType: query.operationType || undefined,
      operationCategory: query.operationCategory || undefined,
      operatorName: query.operatorName || undefined,
      status: query.status ?? undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined
    }
    const res = await getOperationLogPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.current = 1
  query.operationType = ''
  query.operationCategory = ''
  query.operatorName = ''
  query.status = null
  query.startTime = null
  query.endTime = null
  dateRange.value = []
  loadData()
}

const handleDateChange = (val) => {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = null
    query.endTime = null
  }
}

const viewDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 19) : '-'

const formatJson = (obj) => {
  if (typeof obj === 'string') {
    try {
      obj = JSON.parse(obj)
    } catch (e) {
      return obj
    }
  }
  return JSON.stringify(obj, null, 2)
}

const getMethodTagType = (method) => {
  const map = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info'
  }
  return map[method] || 'info'
}

const loadTypes = async () => {
  const res = await getOperationTypes()
  types.value = res.data
}

const loadCategories = async () => {
  const res = await getOperationCategories()
  categories.value = res.data
}

const JsonDiffViewer = {
  name: 'JsonDiffViewer',
  props: {
    oldData: [Object, Array, String],
    newData: [Object, Array, String]
  },
  setup(props) {
    const parseData = (data) => {
      if (typeof data === 'string') {
        try { return JSON.parse(data) } catch (e) { return data }
      }
      return data
    }

    const flatten = (obj, prefix = '') => {
      const result = {}
      if (obj === null || typeof obj !== 'object') {
        result[prefix || 'value'] = obj
        return result
      }
      if (Array.isArray(obj)) {
        obj.forEach((item, index) => {
          Object.assign(result, flatten(item, prefix ? `${prefix}[${index}]` : `[${index}]`))
        })
      } else {
        Object.keys(obj).forEach(key => {
          const path = prefix ? `${prefix}.${key}` : key
          Object.assign(result, flatten(obj[key], path))
        })
      }
      return result
    }

    const oldFlat = flatten(parseData(props.oldData))
    const newFlat = flatten(parseData(props.newData))

    const allKeys = new Set([...Object.keys(oldFlat), ...Object.keys(newFlat)])
    const changes = []

    allKeys.forEach(key => {
      const oldVal = oldFlat[key]
      const newVal = newFlat[key]
      const oldStr = JSON.stringify(oldVal)
      const newStr = JSON.stringify(newVal)

      if (oldStr !== newStr) {
        let type = 'modified'
        if (oldVal === undefined) type = 'added'
        else if (newVal === undefined) type = 'deleted'
        changes.push({ key, oldVal, newVal, type })
      }
    })

    const formatVal = (val) => val === undefined ? '<span class="diff-undefined">undefined</span>' :
      `<span class="diff-value">${JSON.stringify(val)}</span>`

    return { changes, formatVal }
  },
  template: `
    <div class="json-diff">
      <div v-if="changes.length === 0" class="diff-no-change">
        <el-empty description="数据无变化" :image-size="80" />
      </div>
      <div v-else class="diff-list">
        <div v-for="(item, index) in changes" :key="index" class="diff-item" :class="'diff-' + item.type">
          <div class="diff-key">{{ item.key }}</div>
          <div class="diff-values">
            <span v-if="item.type === 'added'" class="diff-added">
              新增: <span v-html="formatVal(item.newVal)"></span>
            </span>
            <span v-else-if="item.type === 'deleted'" class="diff-deleted">
              删除: <span v-html="formatVal(item.oldVal)"></span>
            </span>
            <span v-else class="diff-modified">
              <span class="diff-old">旧: <span v-html="formatVal(item.oldVal)"></span></span>
              <span class="diff-arrow">→</span>
              <span class="diff-new">新: <span v-html="formatVal(item.newVal)"></span></span>
            </span>
          </div>
        </div>
      </div>
    </div>
  `
}

onMounted(() => {
  loadTypes()
  loadCategories()
  loadData()
})
</script>

<style scoped>
.operation-log-page { display: flex; flex-direction: column; gap: 16px; }
.feature-alert { border-radius: 8px; }
.main-card { border-radius: 12px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.search-area { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.data-table { border-radius: 8px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.time-text { font-size: 13px; color: #718096; }
.text-muted { color: #909399; font-size: 12px; }

.section-title {
  font-weight: 600;
  margin: 20px 0 12px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.error-section {
  margin-top: 20px;
}

.error-content {
  background-color: #fef0f0;
  color: #f56c6c;
  padding: 12px 16px;
  border-radius: 4px;
  border: 1px solid #fde2e2;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.params-section {
  margin-top: 16px;
}

.json-content {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
  margin: 0;
}

.data-tabs {
  margin-top: 20px;
}

.json-diff {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
}

.diff-no-change {
  padding: 40px;
}

.diff-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.diff-item {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.diff-item:last-child {
  border-bottom: none;
}

.diff-added {
  background-color: #f0f9eb;
}

.diff-deleted {
  background-color: #fef0f0;
}

.diff-modified {
  background-color: #fdf6ec;
}

.diff-key {
  font-weight: 600;
  color: #303133;
}

.diff-values {
  font-size: 11px;
}

.diff-added {
  color: #67c23a;
}

.diff-deleted {
  color: #f56c6c;
  text-decoration: line-through;
}

.diff-modified .diff-old {
  color: #f56c6c;
  text-decoration: line-through;
}

.diff-modified .diff-arrow {
  margin: 0 8px;
  color: #909399;
}

.diff-modified .diff-new {
  color: #67c23a;
}

.diff-undefined {
  color: #c0c4cc;
  font-style: italic;
}

.diff-value {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 1px 4px;
  border-radius: 2px;
}
</style>
