import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, getNotificationPage, markAsRead, markAllAsRead } from '../api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const loading = ref(false)
  const list = ref([])
  const total = ref(0)

  const fetchUnreadCount = async () => {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data
    } catch (err) {
      console.error('获取未读消息数失败', err)
    }
  }

  const fetchNotificationPage = async (params) => {
    loading.value = true
    try {
      const res = await getNotificationPage(params)
      list.value = res.data.records
      total.value = res.data.total
      return res.data
    } finally {
      loading.value = false
    }
  }

  const markAsReadById = async (id) => {
    try {
      await markAsRead(id)
      const item = list.value.find(n => n.id === id)
      if (item) {
        item.read = true
        item.readTime = new Date().toISOString()
      }
      if (unreadCount.value > 0) {
        unreadCount.value--
      }
    } catch (err) {
      console.error('标记已读失败', err)
    }
  }

  const markAllRead = async () => {
    try {
      await markAllAsRead()
      list.value.forEach(n => {
        n.read = true
      })
      unreadCount.value = 0
    } catch (err) {
      console.error('标记全部已读失败', err)
    }
  }

  const startPolling = (interval = 30000) => {
    fetchUnreadCount()
    return setInterval(fetchUnreadCount, interval)
  }

  return {
    unreadCount,
    loading,
    list,
    total,
    fetchUnreadCount,
    fetchNotificationPage,
    markAsReadById,
    markAllRead,
    startPolling
  }
})
