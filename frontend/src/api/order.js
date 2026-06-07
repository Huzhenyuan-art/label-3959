import request from './index'

export const getOrderPage = (params) => request.get('/orders/page', { params })
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const createOrder = (data) => request.post('/orders', data)
export const updateOrderStatus = (id, data) => request.put(`/orders/${id}/status`, data)
export const processRefund = (id, data) => request.put(`/orders/${id}/refund`, data)
