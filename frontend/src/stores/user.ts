import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import type { User, LoginForm, RegisterForm } from '@/types/user'
import { userApi } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const loading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const userInfo = computed(() => user.value)
  const isAdmin = computed(() => user.value?.role === 1)

  // 初始化用户信息
  const initUserInfo = () => {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      getUserInfo()
    }
  }

  // 登录
  const login = async (loginForm: LoginForm) => {
    try {
      loading.value = true
      const response = await userApi.login(loginForm)

      token.value = response.data.token
      user.value = response.data.user

      // 保存到本地存储
      localStorage.setItem('token', token.value)

      ElMessage.success('登录成功')
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
      return false
    } finally {
      loading.value = false
    }
  }

  // 注册
  const register = async (registerForm: RegisterForm) => {
    try {
      loading.value = true
      await userApi.register(registerForm)

      ElMessage.success('注册成功，请登录')
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败')
      return false
    } finally {
      loading.value = false
    }
  }

  // 获取用户信息
  const getUserInfo = async () => {
    try {
      const response = await userApi.getUserInfo()
      user.value = response.data
      return response.data
    } catch (error: any) {
      console.error('获取用户信息失败:', error)
      logout()
      throw error
    }
  }

  // 更新用户信息
  const updateUserInfo = async (userInfo: Partial<User>) => {
    try {
      loading.value = true
      const response = await userApi.updateUserInfo(userInfo)

      user.value = { ...user.value!, ...response.data }
      ElMessage.success('更新成功')
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
      return false
    } finally {
      loading.value = false
    }
  }

  // 修改密码
  const changePassword = async (oldPassword: string, newPassword: string) => {
    try {
      loading.value = true
      await userApi.changePassword({
        oldPassword,
        newPassword
      })

      ElMessage.success('密码修改成功')
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '密码修改失败')
      return false
    } finally {
      loading.value = false
    }
  }

  // 登出
  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    ElMessage.success('已退出登录')
  }

  // 刷新Token
  const refreshToken = async () => {
    try {
      const response = await userApi.refreshToken()
      token.value = response.data.token
      localStorage.setItem('token', token.value)
      return true
    } catch (error) {
      logout()
      return false
    }
  }

  return {
    // 状态
    user: readonly(user),
    token: readonly(token),
    loading: readonly(loading),

    // 计算属性
    isLoggedIn,
    userInfo,
    isAdmin,

    // 方法
    initUserInfo,
    login,
    register,
    getUserInfo,
    updateUserInfo,
    changePassword,
    logout,
    refreshToken
  }
})
