import { http } from './request'
import type {
  InterviewTemplate,
  InterviewSession,
  InterviewQuestion,
  InterviewSessionRequest,
  SubmitAnswerRequest
} from '@/types/interview'

// 面试模板请求类型
export interface InterviewTemplateRequest {
  name: string
  description?: string
  category: string
  difficulty: number
  duration: number
  questionCount: number
  tags?: string // JSON字符串格式
  config?: any
  isPublic?: number
}

export interface InterviewTemplateQueryParams {
  page?: number
  size?: number
  keyword?: string
  category?: string
  difficulty?: number
  isPublic?: number
  createBy?: number
}

export const interviewApi = {

  // 模板管理
  getTemplateList(category?: string, difficulty?: number) {
    return http.get<InterviewTemplate[]>('/interview/templates', {
      params: { category, difficulty }
    })
  },

  getTemplateDetail(templateId: number) {
    return http.get<InterviewTemplate>(`/interview/templates/${templateId}`)
  },

  createTemplate(data: Partial<InterviewTemplate>) {
    return http.post<InterviewTemplate>('/interview/templates', data)
  },

  updateTemplate(templateId: number, data: Partial<InterviewTemplate>) {
    return http.put<InterviewTemplate>(`/interview/templates/${templateId}`, data)
  },

  deleteTemplate(templateId: number) {
    return http.delete<boolean>(`/interview/templates/${templateId}`)
  },

  // 面试记录管理
  deleteInterviewSession(sessionId: number | string) {
    return http.delete<boolean>(`/interview/session/${sessionId}`)
  },

  restartInterview(sessionId: number) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/restart`)
  },

  // 新的面试模板管理 (管理员功能)
  // 创建面试模板
  createInterviewTemplate(data: InterviewTemplateRequest) {
    return http.post<InterviewTemplate>('/admin/interview-template', data)
  },

  // 更新面试模板
  updateInterviewTemplate(templateId: number, data: InterviewTemplateRequest) {
    return http.put<InterviewTemplate>(`/admin/interview-template/${templateId}`, data)
  },

  // 删除面试模板
  deleteInterviewTemplate(templateId: number) {
    return http.delete<void>(`/admin/interview-template/${templateId}`)
  },

  // 获取面试模板详情
  getInterviewTemplateDetail(templateId: number) {
    return http.get<InterviewTemplate>(`/admin/interview-template/${templateId}`)
  },

  // 分页查询面试模板
  getInterviewTemplateList(params: InterviewTemplateQueryParams) {
    return http.get<{
      records: InterviewTemplate[]
      total: number
      current: number
      size: number
      pages: number
    }>('/admin/interview-template/list', { params })
  },

  // 更新模板状态
  updateInterviewTemplateStatus(templateId: number, isPublic: number) {
    return http.post<void>(`/admin/interview-template/${templateId}/status`, { isPublic })
  },

  // 获取模板分类
  getInterviewTemplateCategories() {
    return http.get<string[]>('/admin/interview-template/categories')
  },

  // 用户面试模板功能
  // 获取公开模板
  getPublicInterviewTemplates() {
    return http.get<InterviewTemplate[]>('/interview-template/public')
  },

  // 按分类获取模板
  getInterviewTemplatesByCategory(category: string) {
    return http.get<InterviewTemplate[]>(`/interview-template/category/${category}`)
  },

  // 按难度获取模板
  getInterviewTemplatesByDifficulty(difficulty: number) {
    return http.get<InterviewTemplate[]>(`/interview-template/difficulty/${difficulty}`)
  },

  // 获取热门模板
  getPopularInterviewTemplates(limit = 10) {
    return http.get<InterviewTemplate[]>('/interview-template/popular', {
      params: { limit }
    })
  },

  // 搜索模板
  searchInterviewTemplates(tags: string[]) {
    return http.get<InterviewTemplate[]>('/interview-template/search', {
      params: { tags }
    })
  },

  // 复制模板
  copyInterviewTemplate(templateId: number) {
    return http.post<InterviewTemplate>(`/interview-template/${templateId}/copy`)
  },

  // 获取我的模板
  getMyInterviewTemplates() {
    return http.get<InterviewTemplate[]>('/interview-template/my')
  },

  // 获取推荐模板
  getRecommendedInterviewTemplates(limit = 5) {
    return http.get<InterviewTemplate[]>('/interview-template/recommended', {
      params: { limit }
    })
  },

  // 获取用户模板分类
  getUserInterviewTemplateCategories() {
    return http.get<string[]>('/interview-template/categories')
  },

  // 面试会话管理
  // 创建面试会话
  createInterviewSession(sessionData: InterviewSessionRequest) {
    return http.post<InterviewSession>('/interview/session', sessionData)
  },

  // 开始面试
  startInterview(sessionId: number | string) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/start`)
  },

  // 结束面试
  finishInterview(sessionId: number | string) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/finish`)
  },

  // 取消面试
  cancelInterview(sessionId: number | string) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/cancel`)
  },

  // 暂停面试
  pauseInterview(sessionId: number | string) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/pause`)
  },

  // 继续面试
  resumeInterview(sessionId: number | string) {
    return http.post<InterviewSession>(`/interview/session/${sessionId}/resume`)
  },

  // 获取面试会话详情
  getInterviewSession(sessionId: number | string) {
    return http.get<InterviewSession>(`/interview/session/${sessionId}`)
  },

  // 获取下一个问题
  getNextQuestion(sessionId: number | string) {
    return http.get<InterviewQuestion>(`/interview/session/${sessionId}/next-question`)
  },

  // 提交回答
  submitAnswer(answerData: SubmitAnswerRequest) {
    return http.post<InterviewQuestion>('/interview/answer', answerData)
  },

  // 获取问答记录
  getQaList(sessionId: number | string) {
    return http.get<InterviewQuestion[]>(`/interview/session/${sessionId}/qa-list`)
  },

  // 生成面试报告
  generateReport(sessionId: number | string) {
    return http.get<string>(`/interview/session/${sessionId}/report`)
  },

  // 获取详细面试报告
  getDetailedReport(sessionId: number | string) {
    return http.get<any>(`/interview/session/${sessionId}/detailed-report`)
  },

  // 刷新评估
  refreshEvaluation(data: {
    qaId: number
    question: string
    answer: string
    jobPosition: string
  }) {
    return http.post<InterviewQuestion>('/interview/refresh-evaluation', data)
  },



  // 获取面试历史
  getInterviewHistory(page: number = 1, size: number = 20) {
    return http.get<InterviewSession[]>(`/interview/history?page=${page}&size=${size}`)
  },

  // 获取当前面试
  getCurrentSession() {
    return http.get<InterviewSession>('/interview/current')
  },

  // 获取最新完成的面试会话（用于查看报告）
  getLatestCompletedSession() {
    return http.get<InterviewSession>('/interview/latest-completed-session')
  },

  // 获取面试统计数据
  getInterviewStatistics() {
    return http.get<{
      totalInterviews: number
      completedInterviews: number
      todayInterviews: number
      averageScore: number
      totalDuration: number
      completionRate: number
    }>('/interview/statistics')
  },

  // 我的模板管理（用户个人模板）
  // 创建我的模板
  createMyTemplate(data: InterviewTemplateRequest) {
    return http.post<InterviewTemplate>('/interview-template/my', data)
  },

  // 更新我的模板
  updateMyTemplate(templateId: number, data: InterviewTemplateRequest) {
    return http.put<InterviewTemplate>(`/interview-template/my/${templateId}`, data)
  },

  // 删除我的模板
  deleteMyTemplate(templateId: number) {
    return http.delete<void>(`/interview-template/my/${templateId}`)
  },

  // 获取我的模板详情
  getMyTemplateDetail(templateId: number) {
    return http.get<InterviewTemplate>(`/interview-template/my/${templateId}`)
  },

  // 分页获取我的模板
  getMyTemplateList(params: InterviewTemplateQueryParams) {
    return http.get<{
      records: InterviewTemplate[]
      total: number
      current: number
      size: number
      pages: number
    }>('/interview-template/my/list', { params })
  },
}
