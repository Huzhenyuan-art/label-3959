<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <el-icon size="24" color="#409EFF"><DataBoard /></el-icon>
        <span>MyBatis Plus</span>
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
        <el-menu-item index="/users">
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
        <div class="tech-badge">Spring Boot 3</div>
        <div class="tech-badge">MyBatis Plus 3.5</div>
        <div class="tech-badge">Vue 3 + Element Plus</div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="app-header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <el-tag type="success" size="small">
          <el-icon><CircleCheck /></el-icon> 服务运行中
        </el-tag>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

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

.app-main {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}
</style>
