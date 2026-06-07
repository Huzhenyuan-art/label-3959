<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="app-layout">
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <el-icon size="24" color="#409EFF"><DataBoard /></el-icon>
        <span>权限管理系统</span>
      </div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#1a1f2e"
        text-color="#a0aec0"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <div class="tech-badge">Spring Boot 3 + Spring Security</div>
        <div class="tech-badge">JWT + MyBatis Plus 3.5</div>
        <div class="tech-badge">Vue 3 + Element Plus + Pinia</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <div class="header-right">
          <el-tag :type="authStore.isAdmin ? 'danger' : 'primary'" size="small">
            {{ authStore.isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ authStore.userInfo?.username }}
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from './store/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
    }
  } else if (command === 'profile') {
    ElMessage.info('个人信息功能开发中')
  }
}
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f0f2f5;
}

.app-layout { height: 100vh; }

.app-aside {
  background: #1a1f2e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid #2d3748;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.el-menu {
  border-right: none !important;
  flex: 1;
}

.el-menu-item {
  border-radius: 8px;
  margin: 4px 12px;
  width: calc(100% - 24px) !important;
  transition: all 0.2s;
}

.el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.15) !important;
  border-left: 3px solid #409EFF;
}

.aside-footer {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-top: 1px solid #2d3748;
}

.tech-badge {
  background: rgba(64, 158, 255, 0.1);
  color: #68d391;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 20px;
  text-align: center;
  border: 1px solid rgba(104, 211, 145, 0.2);
}

.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e8ecf0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}

.user-info:hover {
  color: #409EFF;
}

.user-info .arrow {
  font-size: 12px;
}

.app-main {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}
</style>
