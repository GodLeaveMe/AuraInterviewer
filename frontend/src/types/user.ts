// 用户相关类型定义

export interface User {
  id: number
  username: string
  email: string
  nickname?: string
  realName?: string
  avatar?: string
  phone?: string
  gender?: number
  birthday?: string
  profession?: string
  experienceYears?: number
  bio?: string
  status: number
  role?: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime: string
  updateTime: string
}

export interface LoginForm {
  username: string
  password: string
  rememberMe?: boolean
}

export interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  nickname?: string
  phone?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
