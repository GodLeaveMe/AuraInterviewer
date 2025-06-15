<template>
  <div class="main-layout">
    <!-- 侧边导航栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon size="32" color="#409eff"><Monitor /></el-icon>
          <span v-if="!sidebarCollapsed" class="logo-text">AI面试官</span>
        </div>
        <el-button 
          v-if="!sidebarCollapsed"
          text 
          @click="toggleSidebar"
          class="collapse-btn"
        >
          <el-icon><Fold /></el-icon>
        </el-button>
      </div>
      
      <nav class="sidebar-nav">
        <el-menu
          :default-active="activeMenu"
          :collapse="sidebarCollapsed"
          :unique-opened="true"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <template #title>仪表板</template>
          </el-menu-item>
          
          <el-sub-menu index="interview">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span>面试管理</span>
            </template>
            <el-menu-item index="/interview">开始面试</el-menu-item>
            <el-menu-item index="/interview/templates">面试模板</el-menu-item>
            <el-menu-item index="/interview/my-templates">我的模板</el-menu-item>
            <el-menu-item index="/interview/history">面试历史</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="profile">
            <template #title>
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </template>
            <el-menu-item index="/profile">个人资料</el-menu-item>
            <el-menu-item index="/profile/api-config">API配置</el-menu-item>
            <el-menu-item index="/settings">系统设置</el-menu-item>
          </el-sub-menu>

          <!-- 管理员菜单 -->
          <el-sub-menu v-if="userStore.isAdmin" index="admin">
            <template #title>
              <el-icon><Tools /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/ai-config">AI配置管理</el-menu-item>

            <el-menu-item index="/admin/interview-template">面试模板管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </nav>
    </aside>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <div class="header-left">
          <el-button 
            v-if="sidebarCollapsed"
            text 
            @click="toggleSidebar"
            class="expand-btn"
          >
            <el-icon><Expand /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 通知 -->
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
            <el-button text>
              <el-icon size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
          
          <!-- 用户菜单 -->
          <el-dropdown @command="handleUserCommand" class="user-dropdown">
            <span class="user-info">
              <el-avatar :src="userStore.userInfo?.avatar" :size="32">
                {{ userStore.userInfo?.nickname?.[0] || userStore.userInfo?.username?.[0] }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  House,
  ChatDotRound,
  User,
  Setting,
  Monitor,
  Fold,
  Expand,
  Bell,
  ArrowDown,
  SwitchButton,
  Tools
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarCollapsed = ref(false)
const notificationCount = ref(0)

// 当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/interview')) {
    if (path === '/interview') return '/interview'
    if (path.includes('/history')) return '/interview/history'
    return '/interview'
  }
  return path
})

// 面包屑导航
const breadcrumbs = computed(() => {
  const path = route.path
  const breadcrumbs = [{ title: '首页', path: '/dashboard' }]
  
  if (path.startsWith('/interview')) {
    breadcrumbs.push({ title: '面试管理', path: '/interview' })
    if (path.includes('/history')) {
      breadcrumbs.push({ title: '面试历史', path: '/interview/history' })
    } else if (path.includes('/templates') && !path.includes('/my-templates')) {
      breadcrumbs.push({ title: '面试模板', path: '/interview/templates' })
    } else if (path.includes('/my-templates')) {
      breadcrumbs.push({ title: '我的模板', path: '/interview/my-templates' })
    } else if (path.includes('/room')) {
      breadcrumbs.push({ title: '面试房间', path: path })
    } else if (path.includes('/report')) {
      breadcrumbs.push({ title: '面试报告', path: path })
    }
  } else if (path === '/profile') {
    breadcrumbs.push({ title: '个人资料', path: '/profile' })
  } else if (path === '/settings') {
    breadcrumbs.push({ title: '系统设置', path: '/settings' })
  } else if (path.startsWith('/admin')) {
    breadcrumbs.push({ title: '系统管理', path: '/admin' })
    if (path === '/admin/users') {
      breadcrumbs.push({ title: '用户管理', path: '/admin/users' })
    } else if (path === '/admin/ai-config') {
      breadcrumbs.push({ title: 'AI配置管理', path: '/admin/ai-config' })
    } else if (path === '/admin/interview-template') {
      breadcrumbs.push({ title: '面试模板管理', path: '/admin/interview-template' })
    }
  } else if (path.startsWith('/admin')) {
    breadcrumbs.push({ title: '系统管理', path: '/admin' })
    if (path === '/admin/users') {
      breadcrumbs.push({ title: '用户管理', path: '/admin/users' })
    } else if (path === '/admin/ai-config') {
      breadcrumbs.push({ title: 'AI配置管理', path: '/admin/ai-config' })
    } else if (path === '/admin/interview-template') {
      breadcrumbs.push({ title: '面试模板管理', path: '/admin/interview-template' })
    }
  }
  
  return breadcrumbs
})

// 切换侧边栏
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 处理用户菜单命令
const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 240px;
  background: #001529;
  transition: width 0.3s ease;
  
  &.collapsed {
    width: 64px;
  }
  
  .sidebar-header {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    border-bottom: 1px solid #1f2937;
    
    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      color: white;
      
      .logo-text {
        font-size: 18px;
        font-weight: 600;
      }
    }
    
    .collapse-btn {
      color: #9ca3af;
      
      &:hover {
        color: white;
      }
    }
  }
  
  .sidebar-nav {
    height: calc(100vh - 64px);
    overflow-y: auto;
    
    .sidebar-menu {
      border: none;
      background: transparent;
      
      :deep(.el-menu-item) {
        color: #9ca3af;
        
        &:hover {
          background: #1f2937;
          color: white;
        }
        
        &.is-active {
          background: #409eff;
          color: white;
        }
      }
      
      :deep(.el-sub-menu) {
        .el-sub-menu__title {
          color: #9ca3af;
          
          &:hover {
            background: #1f2937;
            color: white;
          }
        }
        
        .el-menu-item {
          background: #111827;
          
          &:hover {
            background: #1f2937;
          }
          
          &.is-active {
            background: #409eff;
          }
        }
      }
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 64px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .expand-btn {
      color: #6b7280;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .notification-badge {
      .el-button {
        color: #6b7280;
        
        &:hover {
          color: #409eff;
        }
      }
    }
    
    .user-dropdown {
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 6px;
        transition: background-color 0.2s;
        
        &:hover {
          background: #f3f4f6;
        }
        
        .username {
          font-weight: 500;
          color: #374151;
        }
      }
    }
  }
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    height: 100vh;
    
    &.collapsed {
      transform: translateX(-100%);
    }
  }
  
  .main-content {
    margin-left: 0;
  }
  
  .page-content {
    padding: 16px;
  }
}
</style>
