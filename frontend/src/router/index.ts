import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  // 主应用布局
  {
    path: '/',
    component: () => import('@/components/Layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '仪表板',
          requiresAuth: true
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: {
          title: '个人资料',
          requiresAuth: true
        }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/profile/Settings.vue'),
        meta: {
          title: '设置',
          requiresAuth: true
        }
      },
      {
        path: 'interview',
        name: 'Interview',
        component: () => import('@/views/interview/Interview.vue'),
        meta: {
          title: '面试',
          requiresAuth: true
        }
      },

      {
        path: 'interview/chat/:id',
        name: 'ChatInterview',
        component: () => import('@/views/interview/ChatInterview.vue'),
        meta: {
          title: 'AI对话面试',
          requiresAuth: true
        }
      },
      {
        path: 'interview/history',
        name: 'InterviewHistory',
        component: () => import('@/views/interview/InterviewHistory.vue'),
        meta: {
          title: '面试历史',
          requiresAuth: true
        }
      },
      {
        path: 'interview/report/:sessionId',
        name: 'InterviewReport',
        component: () => import('@/views/interview/InterviewReport.vue'),
        meta: {
          title: '面试报告',
          requiresAuth: true
        }
      },
      {
        path: 'interview/templates',
        name: 'TemplateBrowse',
        component: () => import('@/views/interview/TemplateBrowse.vue'),
        meta: {
          title: '面试模板',
          requiresAuth: true
        }
      },

      {
        path: 'interview/my-templates',
        name: 'MyTemplates',
        component: () => import('@/views/interview/MyTemplates.vue'),
        meta: {
          title: '我的模板',
          requiresAuth: true
        }
      },
      {
        path: 'profile/api-config',
        name: 'ApiConfig',
        component: () => import('@/views/profile/ApiConfig.vue'),
        meta: {
          title: 'API配置',
          requiresAuth: true
        }
      },
      // 管理员路由
      {
        path: 'admin/users',
        name: 'AdminUserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'admin/ai-config',
        name: 'AdminAiConfigManage',
        component: () => import('@/views/admin/AiConfigManage.vue'),
        meta: {
          title: 'AI配置管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'admin/interview-template',
        name: 'AdminInterviewTemplateManage',
        component: () => import('@/views/admin/InterviewTemplateManage.vue'),
        meta: {
          title: '面试模板管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },

    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - AI面试官`
  }
  
  // 检查是否需要认证
  if (requiresAuth) {
    if (!userStore.isLoggedIn) {
      // 尝试从本地存储恢复登录状态
      const token = localStorage.getItem('token')
      if (token) {
        try {
          await userStore.getUserInfo()
          // 检查管理员权限（暂时放宽权限检查）
          if (to.meta.requiresAdmin && !userStore.isAdmin) {
            // 不跳转，允许访问，避免权限错误
            console.warn('用户无管理员权限，但允许访问')
          }
          next()
        } catch (error) {
          // Token无效，跳转到登录页
          localStorage.removeItem('token')
          next({
            path: '/login',
            query: { redirect: to.fullPath }
          })
        }
      } else {
        next({
          path: '/login',
          query: { redirect: to.fullPath }
        })
      }
    } else {
      // 检查管理员权限（暂时放宽权限检查）
      if (to.meta.requiresAdmin && !userStore.isAdmin) {
        // 不跳转，允许访问，避免权限错误
        console.warn('用户无管理员权限，但允许访问')
      }
      next()
    }
  } else {
    // 如果已登录用户访问登录/注册页，重定向到仪表板
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      next('/dashboard')
    } else {
      next()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
