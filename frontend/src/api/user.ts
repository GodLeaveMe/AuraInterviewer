import { http } from './request'
import type { LoginForm, RegisterForm, LoginResponse, User } from '@/types/user'

export const userApi = {
  // 用户注册
  register(data: RegisterForm) {
    return http.post('/user/register', data)
  },

  // 用户登录
  login(data: LoginForm) {
    return http.post<LoginResponse>('/user/login', data)
  },

  // 用户登出
  logout() {
    return http.post('/user/logout')
  },

  // 获取用户信息
  getUserInfo() {
    return http.get<User>('/user/info')
  },

  // 更新用户信息
  updateUserInfo(data: Partial<User>) {
    return http.put<User>('/user/info', data)
  },

  // 更新个人资料
  updateProfile(data: any) {
    return http.put<User>('/user/profile', data)
  },

  // 修改密码
  changePassword(data: { oldPassword: string; newPassword: string }) {
    return http.post('/user/change-password', data)
  },

  // 刷新Token
  refreshToken() {
    return http.post<LoginResponse>('/user/refresh-token')
  },

  // 检查用户名是否存在
  checkUsername(username: string) {
    return http.get<{ exists: boolean }>('/user/check-username', {
      params: { username }
    })
  },

  // 检查邮箱是否存在
  checkEmail(email: string) {
    return http.get<{ exists: boolean }>('/user/check-email', {
      params: { email }
    })
  }
}
