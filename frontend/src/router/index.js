import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserView from '../views/UserView.vue'
import ProductView from '../views/ProductView.vue'
import OrderView from '../views/OrderView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import NotificationView from '../views/NotificationView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', component: LoginView, meta: { title: '登录', requiresAuth: false } },
  { path: '/', component: HomeView, meta: { title: '首页概览', requiresAuth: true } },
  { path: '/users', component: UserView, meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true } },
  { path: '/products', component: ProductView, meta: { title: '商品管理', requiresAuth: true } },
  { path: '/orders', component: OrderView, meta: { title: '订单管理', requiresAuth: true } },
  { path: '/orders/:id', component: OrderDetailView, meta: { title: '订单详情', requiresAuth: true } },
  { path: '/notifications', component: NotificationView, meta: { title: '消息中心', requiresAuth: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  document.title = to.meta.title ? `${to.meta.title} - 权限管理系统` : '权限管理系统'

  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/')
    return
  }

  if (to.path === '/login' && authStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
