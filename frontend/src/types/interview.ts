// 面试相关类型定义

export interface InterviewTemplate {
  id?: number
  name: string
  description?: string
  category: string
  difficulty: number
  duration: number
  questionCount: number
  tags?: string // JSON字符串格式
  config?: any
  isPublic?: number
  usageCount?: number
  createBy?: number
  createTime?: string
  updateTime?: string
}

export interface InterviewSession {
  id: number | string
  userId: number | string
  templateId: number | string
  title: string
  jobPosition?: string
  status: number // 0-未开始，1-进行中，2-已完成，3-已取消，4-已暂停
  startTime?: string
  endTime?: string
  duration?: number
  questionCount: number
  answeredCount: number
  score?: number
  aiModel?: string | number
  interviewMode?: 'text'
  settings?: InterviewSettings
  recordingUrl?: string
  summary?: string
  feedback?: string
  createTime: string
  updateTime: string
}

export interface InterviewQa {
  id: number
  sessionId: number
  questionOrder: number
  question: string
  answer?: string
  answerType: number // 1-文本
  answerTime?: string
  thinkingTime?: number
  score?: number
  aiFeedback?: string
  aiThinking?: string // AI思维链
  keywords?: string[]
  emotionScore?: number
  confidenceScore?: number
  createTime: string
  updateTime: string
}

// 新增类型定义
export interface InterviewSettings {
  enableThinking: boolean
  enableRealTimeScore: boolean
  enableAutoNext: boolean
  interviewMode: 'text'
  aiModel: string | number
}

export interface InterviewSessionRequest {
  title: string
  templateId: number
  jobPosition: string
  duration: number
  questionCount: number
  interviewMode: 'text'
  aiModel: string | number
  settings: InterviewSettings
}

export interface InterviewQuestion {
  id: number | string
  sessionId: number | string
  questionOrder: number
  question: string
  answer?: string
  answerType?: number // 1-文本
  answerTime?: string
  thinkingTime?: number
  score?: number
  aiFeedback?: string
  aiThinking?: string // AI思维链
  keywords?: string[] // 关键词数组
  emotionScore?: number // 情感分数
  confidenceScore?: number // 自信度分数
  questionData?: string // 问题结构化数据
  answerData?: string // 回答结构化数据
  createTime: string
  updateTime: string
}

export interface AnswerRequest {
  sessionId: number
  questionOrder: number
  answer: string
  answerType: 'text'
  thinkingTime?: number
}

export interface CreateSessionRequest {
  title: string
  templateId: number

  aiModel?: string
  jobPosition?: string
  difficulty?: string
  duration?: number
  questionCount?: number
  customConfig?: string
}

export interface SubmitAnswerRequest {
  sessionId: number | string
  questionOrder: number
  answer?: string
  answerType?: number

  thinkingTime?: number
}

export interface InterviewStatusType {
  0: '未开始'
  1: '进行中'
  2: '已完成'
  3: '已取消'
  4: '已暂停'
}

// 面试状态常量
export const INTERVIEW_STATUS = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  COMPLETED: 2,
  CANCELLED: 3,
  PAUSED: 4
} as const

// 面试状态标签
export const INTERVIEW_STATUS_LABELS = {
  [INTERVIEW_STATUS.NOT_STARTED]: '未开始',
  [INTERVIEW_STATUS.IN_PROGRESS]: '进行中',
  [INTERVIEW_STATUS.COMPLETED]: '已完成',
  [INTERVIEW_STATUS.CANCELLED]: '已取消',
  [INTERVIEW_STATUS.PAUSED]: '已暂停'
} as const

// 面试状态颜色
export const INTERVIEW_STATUS_COLORS = {
  [INTERVIEW_STATUS.NOT_STARTED]: 'info',
  [INTERVIEW_STATUS.IN_PROGRESS]: 'warning',
  [INTERVIEW_STATUS.COMPLETED]: 'success',
  [INTERVIEW_STATUS.CANCELLED]: 'danger',
  [INTERVIEW_STATUS.PAUSED]: 'primary'
} as const

export interface DifficultyType {
  1: '初级'
  2: '中级'
  3: '高级'
}

// 模板请求类型
export interface InterviewTemplateRequest {
  id?: number
  name: string
  description?: string
  category: string
  difficulty: string | number
  questionCount: number
  duration: number
  tags?: string[]
  isPublic?: number
}

// 模板查询参数类型
export interface InterviewTemplateQueryParams {
  current?: number
  size?: number
  category?: string
  difficulty?: string
  keyword?: string
  orderBy?: string
  orderDirection?: string
}
