import request from './index'

export const getNotificationPage = (params) => request.get('/notifications/page', { params })
export const getUnreadCount = () => request.get('/notifications/unread-count')
export const markAsRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllAsRead = () => request.put('/notifications/read-all')
