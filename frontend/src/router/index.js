import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserView from '../views/UserView.vue'
import ProductView from '../views/ProductView.vue'
import CartView from '../views/CartView.vue'
import OrderView from '../views/OrderView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import NotificationView from '../views/NotificationView.vue'
import ReviewView from '../views/ReviewView.vue'
import CouponCenterView from '../views/CouponCenterView.vue'
import MyCouponsView from '../views/MyCouponsView.vue'
import AddressView from '../views/AddressView.vue'
import StockReservationView from '../views/StockReservationView.vue'
import RefundView from '../views/RefundView.vue'
import RefundApplyView from '../views/RefundApplyView.vue'
import RefundDetailView from '../views/RefundDetailView.vue'
import OperationLogView from '../views/OperationLogView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', component: LoginView, meta: { title: '登录', requiresAuth: false } },
  { path: '/', component: HomeView, meta: { title: '首页概览', requiresAuth: true } },
  { path: '/users', component: UserView, meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true } },
  { path: '/products', component: ProductView, meta: { title: '商品管理', requiresAuth: true } },
  { path: '/cart', component: CartView, meta: { title: '购物车', requiresAuth: true } },
  { path: '/orders', component: OrderView, meta: { title: '订单管理', requiresAuth: true } },
  { path: '/orders/:id', component: OrderDetailView, meta: { title: '订单详情', requiresAuth: true } },
  { path: '/orders/:orderId/refund/apply', component: RefundApplyView, meta: { title: '申请退款', requiresAuth: true } },
  { path: '/refunds', component: RefundView, meta: { title: '退款管理', requiresAuth: true } },
  { path: '/refunds/:id', component: RefundDetailView, meta: { title: '退款详情', requiresAuth: true } },
  { path: '/reviews', component: ReviewView, meta: { title: '商品评价', requiresAuth: true } },
  { path: '/notifications', component: NotificationView, meta: { title: '消息中心', requiresAuth: true } },
  { path: '/coupons', component: CouponCenterView, meta: { title: '优惠券中心', requiresAuth: true } },
  { path: '/my-coupons', component: MyCouponsView, meta: { title: '我的优惠券', requiresAuth: true } },
  { path: '/addresses', component: AddressView, meta: { title: '收货地址', requiresAuth: true } },
  { path: '/stock-reservations', component: StockReservationView, meta: { title: '库存预占管理', requiresAuth: true } },
  { path: '/operation-logs', component: OperationLogView, meta: { title: '操作审计日志', requiresAuth: true, requiresAdmin: true } }
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
