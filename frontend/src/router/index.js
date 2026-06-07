import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import UserView from '../views/UserView.vue'
import ProductView from '../views/ProductView.vue'
import OrderView from '../views/OrderView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'

const routes = [
  { path: '/', component: HomeView, meta: { title: '首页概览' } },
  { path: '/users', component: UserView, meta: { title: '用户管理' } },
  { path: '/products', component: ProductView, meta: { title: '商品管理' } },
  { path: '/orders', component: OrderView, meta: { title: '订单管理' } },
  { path: '/orders/:id', component: OrderDetailView, meta: { title: '订单详情' } }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
