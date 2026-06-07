import request, { createApiCall } from './index'

const basePath = '/refunds'

export const getRefundPage = createApiCall(params => request.get(`${basePath}/page`, { params }))
export const getRefundDetail = createApiCall(id => request.get(`${basePath}/${id}`))
export const applyRefund = createApiCall(data => request.post(`${basePath}/apply`, data))
export const auditRefund = createApiCall(data => request.post(`${basePath}/audit`, data))
export const cancelRefund = createApiCall(id => request.put(`${basePath}/${id}/cancel`))
