import request from './index'

export const getStockReservationPage = (params) => request.get('/stock-reservations/page', { params })
export const getStockReservationsByOrderId = (orderId) => request.get(`/stock-reservations/order/${orderId}`)
export const releaseExpiredReservations = () => request.post('/stock-reservations/release-expired')
