import { http } from './request'
import type { ChatRequest, ChatResponse } from '@/types/api'

// AI配置相关类型
export interface AiConfig {
  id?: number
  name: string
  provider: string
  model: string
  modelType: string // chat, tts, reasoning, stt
  apiKey: string
  apiUrl?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  frequencyPenalty?: number
  presencePenalty?: number
  systemPrompt?: string
  isActive?: number
  priority?: number
  description?: string
  createTime?: string
  updateTime?: string
}

export interface AiConfigRequest {
  name: string
  provider: string
  model: string
  modelType: string // chat, tts, reasoning, stt
  apiKey: string
  apiUrl?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  frequencyPenalty?: number
  presencePenalty?: number
  systemPrompt?: string
  isActive?: number
  priority?: number
  description?: string
}

export interface AiConfigQueryParams {
  page?: number
  size?: number
  keyword?: string
  provider?: string
  isActive?: number
}

export interface AiModel {
  id: number
  name: string
  provider: string
  model: string
  description?: string
  isThinking?: boolean // 是否为深度思考模型
  maxTokens?: number
  temperature?: number
  isActive: boolean
  isDefault?: boolean
}

export const aiApi = {
  // AI聊天
  chat(data: ChatRequest) {
    return http.post<ChatResponse>('/ai/chat', data)
  },

  // 异步AI聊天
  chatAsync(data: ChatRequest) {
    return http.post<ChatResponse>('/ai/chat/async', data)
  },

  // 流式AI聊天
  chatStream(data: ChatRequest) {
    return fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(data)
    })
  },

  // 生成面试问题
  generateInterviewQuestion(params: {
    jobPosition: string
    difficulty: string
    previousQuestions?: string
  }) {
    return http.post<string>('/ai/interview/question', params)
  },

  // 评估面试回答
  evaluateAnswer(params: {
    question: string
    answer: string
    jobPosition: string
  }) {
    return http.post<string>('/ai/interview/evaluate', params)
  },

  // 生成面试总结
  generateInterviewSummary(params: {
    sessionData: string
  }) {
    return http.post<string>('/ai/interview/summary', params)
  },

  // 健康检查
  healthCheck() {
    return http.get<{
      available: boolean
      status: string
      timestamp: number
    }>('/ai/health')
  },

  // AI配置管理 (管理员功能)
  // 创建AI配置
  createAiConfig(data: AiConfigRequest) {
    return http.post<AiConfig>('/admin/ai-config', data)
  },

  // 更新AI配置
  updateAiConfig(configId: number, data: AiConfigRequest) {
    return http.put<AiConfig>(`/admin/ai-config/${configId}`, data)
  },

  // 删除AI配置
  deleteAiConfig(configId: number) {
    return http.delete<void>(`/admin/ai-config/${configId}`)
  },

  // 获取AI配置详情
  getAiConfigDetail(configId: number) {
    return http.get<AiConfig>(`/admin/ai-config/${configId}`)
  },

  // 分页查询AI配置
  getAiConfigList(params: AiConfigQueryParams) {
    return http.get<{
      records: AiConfig[]
      total: number
      current: number
      size: number
      pages: number
    }>('/admin/ai-config/list', { params })
  },

  // 获取活跃的AI配置
  getActiveAiConfigs() {
    return http.get<AiConfig[]>('/admin/ai-config/active')
  },

  // 更新AI配置状态
  updateAiConfigStatus(configId: number, isActive: number) {
    return http.post<void>(`/admin/ai-config/${configId}/status`, { isActive })
  },

  // 测试AI配置连接
  testAiConfig(configId: number) {
    return http.post<boolean>(`/admin/ai-config/${configId}/test`)
  },

  // 测试AI配置连接（使用配置对象）
  testAiConfigWithData(data: AiConfigRequest) {
    return http.post<boolean>('/admin/ai-config/test', data)
  },

  // 获取默认AI配置
  getDefaultAiConfig() {
    return http.get<AiConfig>('/admin/ai-config/default')
  },

  // 设置默认AI配置
  setDefaultAiConfig(configId: number) {
    return http.post<void>(`/admin/ai-config/${configId}/set-default`)
  },

  // 获取可用的AI模型列表（用户可选择的模型）
  getAvailableModels(params?: { provider?: string; apiKey?: string; apiUrl?: string }) {
    if (params && params.provider && params.apiKey) {
      // 管理员获取指定提供商的模型列表
      return http.post<any[]>('/admin/ai-config/available-models', params)
    } else {
      // 用户获取系统配置的模型列表
      return http.get<AiModel[]>('/ai/models/available')
    }
  },

  // 获取用户自定义的AI配置
  getUserAiConfigs() {
    return http.get<AiConfig[]>('/ai/config/user')
  },

  // 检查AI服务可用性
  checkServiceAvailability() {
    return http.get<boolean>('/ai/service/availability')
  },

  // 获取指定类型的AI配置列表
  getConfigsByType(modelType: string) {
    return http.get<AiConfig[]>(`/ai/config/type/${modelType}`)
  },

  // 获取TTS语音模型配置
  getTtsConfigs() {
    return http.get<AiConfig[]>('/ai/config/type/tts')
  },

  // 获取语音识别模型配置
  getSttConfigs() {
    return http.get<AiConfig[]>('/ai/config/type/stt')
  },

  // 获取深度思考模型配置
  getReasoningConfigs() {
    return http.get<AiConfig[]>('/ai/config/type/reasoning')
  },

  // 获取文本对话模型配置
  getChatConfigs() {
    return http.get<AiConfig[]>('/ai/config/type/chat')
  }
}
