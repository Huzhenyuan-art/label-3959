import request from './index'

export const getRefundPage = (params) => request.get('/refunds/page', { params })
export const getRefundDetail = (id) => request.get(`/refunds/${id}`)
export const applyRefund = (data) => request.post('/refunds/apply', data)
export const auditRefund = (data) => request.post('/refunds/audit', data)
export const cancelRefund = (id) => request.put(`/refunds/${id}/cancel`)
