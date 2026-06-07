import request, { createApiCall } from './index'

const basePath = '/orders'

export const getOrderPage = createApiCall(params => request.get(`${basePath}/page`, { params }))
export const getOrderDetail = createApiCall(id => request.get(`${basePath}/${id}`))
export const createOrder = createApiCall(data => request.post(basePath, data))
export const updateOrderStatus = createApiCall((id, data) => request.put(`${basePath}/${id}/status`, data))
export const processRefund = createApiCall((id, data) => request.put(`${basePath}/${id}/refund`, data))
export const updateOrderRemark = createApiCall((id, data) => request.put(`${basePath}/${id}/remark`, data))
