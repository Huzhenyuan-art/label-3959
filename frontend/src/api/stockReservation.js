import request, { createApiCall } from './index'

const basePath = '/stock-reservations'

export const getStockReservationPage = createApiCall(params => request.get(`${basePath}/page`, { params }))
export const getStockReservationsByOrderId = createApiCall(orderId => request.get(`${basePath}/order/${orderId}`))
export const releaseExpiredReservations = createApiCall(() => request.post(`${basePath}/release-expired`))
