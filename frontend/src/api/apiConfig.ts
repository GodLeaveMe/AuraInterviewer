import request from './request'

export interface ApiConfig {
  id?: number
  name: string
  apiType: string
  apiKey: string
  baseUrl?: string
  model?: string
  modelType?: string // chat, tts, reasoning, stt
  maxTokens?: number
  temperature?: number
  systemPrompt?: string
  interviewerRole?: string
  interviewStyle?: string
  description?: string
  enabled: boolean
  isDefault: boolean
  sort?: number
}

export const apiConfigApi = {
  // 获取API配置列表（分页）
  getList(page = 1, size = 10, keyword = '') {
    return request.get('/admin/api-config/list', {
      params: { page, size, keyword }
    })
  },

  // 获取API配置详情
  get(id: number) {
    return request.get(`/admin/api-config/${id}`)
  },

  // 创建API配置
  add(data: ApiConfig) {
    return request.post('/admin/api-config', data)
  },

  // 更新API配置
  update(id: number, data: ApiConfig) {
    return request.put(`/admin/api-config/${id}`, data)
  },

  // 删除API配置
  delete(id: number) {
    return request.delete(`/admin/api-config/${id}`)
  },

  // 测试API配置
  test(data: ApiConfig) {
    return request.post('/admin/api-config/test', data)
  },

  // 切换API配置状态
  toggleStatus(id: number, enabled: boolean) {
    return request.post(`/admin/api-config/${id}/toggle-status`, null, {
      params: { enabled }
    })
  },

  // 设置默认配置
  setDefault(id: number) {
    return request.post(`/admin/api-config/${id}/set-default`)
  },

  // 获取启用的配置列表
  getEnabledConfigs() {
    return request.get('/admin/api-config/enabled')
  },

  // 获取默认配置
  getDefaultConfig() {
    return request.get('/admin/api-config/default')
  },

  // 获取API模型列表
  getModels(data: { apiType: string; apiKey: string; baseUrl?: string }) {
    return request.post('/admin/api-config/models', data)
  },

  // 根据模型类型获取配置列表
  getConfigsByType(modelType: string) {
    return request.get(`/admin/api-config/type/${modelType}`)
  },

  // 根据模型类型获取启用的配置列表
  getEnabledConfigsByType(modelType: string) {
    return request.get(`/admin/api-config/enabled/type/${modelType}`)
  }
}
