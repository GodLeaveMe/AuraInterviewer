import { http } from './request'

export interface UserQueryParams {
  page?: number
  size?: number
  keyword?: string
  status?: number
  gender?: number
  profession?: string
  minExperienceYears?: number
  maxExperienceYears?: number
  startDate?: string
  endDate?: string
  sortField?: string
  sortOrder?: string
}

export interface User {
  id?: number
  username: string
  email: string
  password?: string
  nickname?: string
  realName?: string
  avatar?: string
  phone?: string
  gender?: number
  birthday?: string
  profession?: string
  experienceYears?: number
  bio?: string
  status?: number
  role?: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  updateTime?: string
}

export interface SystemStatistics {
  totalUsers: number
  activeUsers: number
  inactiveUsers: number
  todayNewUsers: number
  totalInterviews: number
  todayInterviews: number
  completedInterviews: number
}

export const adminApi = {
  // 用户管理
  getUserList(params: UserQueryParams) {
    return http.get<{
      records: User[]
      total: number
      current: number
      size: number
      pages: number
    }>('/user/admin/users', { params })
  },

  getUserDetail(userId: number) {
    return http.get<User>(`/user/admin/users/${userId}`)
  },

  createUser(data: User) {
    return http.post<User>('/user/admin/users/create', data)
  },

  updateUser(userId: number, data: User) {
    return http.put<User>(`/user/admin/users/${userId}`, data)
  },

  deleteUser(userId: number) {
    return http.delete<boolean>(`/user/admin/users/${userId}`)
  },

  toggleUserStatus(userId: number, status: number) {
    return http.put<boolean>(`/user/admin/users/${userId}/status`, null, {
      params: { status }
    })
  },

  resetUserPassword(userId: number, newPassword: string) {
    return http.post<string>(`/user/admin/users/${userId}/reset-password`, null, {
      params: { newPassword }
    })
  },

  batchDeleteUsers(userIds: number[]) {
    return http.delete<boolean>('/user/admin/users/batch', {
      data: { userIds }
    })
  },

  batchUpdateUserStatus(userIds: number[], status: number) {
    return http.put<boolean>('/user/admin/users/batch/status', {
      userIds,
      status
    })
  },

  // 系统统计
  getSystemStatistics() {
    return http.get<SystemStatistics>('/user/admin/statistics')
  },

  // 用户操作日志
  getUserOperationLogs(userId: number, page = 1, size = 10) {
    return http.get<{
      records: any[]
      total: number
      current: number
      size: number
    }>(`/user/admin/users/${userId}/logs`, {
      params: { page, size }
    })
  },

  // 导出用户数据
  exportUserData(params: UserQueryParams) {
    return http.get('/user/admin/users/export', {
      params,
      responseType: 'blob'
    })
  },

  // 图表数据
  getUserTrendData() {
    return http.get<Array<{date: string, count: number}>>('/user/admin/statistics/user-trend')
  },

  getGenderDistribution() {
    return http.get<Array<{gender: number, count: number}>>('/user/admin/statistics/gender-distribution')
  },

  getActivityData() {
    return http.get<{weeklyData: number[]}>('/user/admin/statistics/activity')
  },

  getUserStatusDistribution() {
    return http.get<Array<{ status: number; statusName: string; count: number }>>('/user/admin/statistics/user-status')
  }
}
