import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../store/auth'

export const HTTP_STATUS = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500
}

export const API_ERROR_MESSAGES = {
  NETWORK_ERROR: '网络错误，请稍后重试',
  UNAUTHORIZED: '登录已过期，请重新登录',
  FORBIDDEN: '权限不足，无法访问',
  REQUEST_FAILED: '请求失败'
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000
})

request.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  err => {
    return Promise.reject(err)
  }
)

request.interceptors.response.use(
  res => {
    if (res.config.responseType === 'blob') {
      return res
    }
    const data = res.data
    if (data.code !== HTTP_STATUS.SUCCESS) {
      if (!res.config.skipErrorToast) {
        ElMessage.error(data.message || API_ERROR_MESSAGES.REQUEST_FAILED)
      }
      return Promise.reject({ message: data.message, errors: data.errors, response: res })
    }
    return data
  },
  err => {
    handleResponseError(err)
    return Promise.reject(err)
  }
)

function handleResponseError(err) {
  const status = err.response?.status
  const skipToast = err.config?.skipErrorToast

  if (status === HTTP_STATUS.UNAUTHORIZED) {
    const authStore = useAuthStore()
    authStore.logout()
    if (!skipToast) {
      ElMessage.error(API_ERROR_MESSAGES.UNAUTHORIZED)
    }
    router.push('/login')
  } else if (status === HTTP_STATUS.FORBIDDEN) {
    if (!skipToast) {
      ElMessage.error(API_ERROR_MESSAGES.FORBIDDEN)
    }
  } else if (!skipToast) {
    ElMessage.error(err.message || API_ERROR_MESSAGES.NETWORK_ERROR)
  }
}

export function createApiCall(fn, options = {}) {
  return async (...args) => {
    try {
      const result = await fn(...args)
      return options.returnRaw ? result : result.data
    } catch (error) {
      if (options.onError) {
        options.onError(error)
      }
      if (options.throwError !== false) {
        throw error
      }
      return null
    }
  }
}

export function createCrudApi(basePath) {
  return {
    getPage: createApiCall(params => request.get(`${basePath}/page`, { params })),
    getList: createApiCall(() => request.get(basePath)),
    getById: createApiCall(id => request.get(`${basePath}/${id}`)),
    create: createApiCall(data => request.post(basePath, data)),
    update: createApiCall((id, data) => request.put(`${basePath}/${id}`, data)),
    remove: createApiCall(id => request.delete(`${basePath}/${id}`))
  }
}

export async function confirmAction(message, title = '确认操作') {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

export function withLoading(fn, setLoading) {
  return async (...args) => {
    try {
      setLoading?.(true)
      return await fn(...args)
    } finally {
      setLoading?.(false)
    }
  }
}

export default request
