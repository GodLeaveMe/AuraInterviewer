// 面试相关常量

/**
 * 面试模板分类
 */
export const INTERVIEW_CATEGORIES = [
  '技术面试',
  '行为面试',
  '算法面试',
  '系统设计',
  '项目经验',
  '综合面试',
  '基础知识',
  '实战编程'
] as const

/**
 * 分类标签类型映射
 */
export const CATEGORY_TAG_TYPES: Record<string, string> = {
  '技术面试': 'primary',
  '行为面试': 'success',
  '算法面试': 'warning',
  '系统设计': 'danger',
  '项目经验': 'info',
  '综合面试': '',
  '基础知识': 'primary',
  '实战编程': 'success'
}

/**
 * 难度级别
 */
export const DIFFICULTY_LEVELS = [
  { value: 1, label: '初级', type: 'success' },
  { value: 2, label: '中级', type: 'warning' },
  { value: 3, label: '高级', type: 'danger' }
] as const

/**
 * 面试状态
 */
export const INTERVIEW_STATUS = {
  WAITING: 0,      // 等待开始
  IN_PROGRESS: 1,  // 进行中
  COMPLETED: 2,    // 已完成
  PAUSED: 3,       // 已暂停
  CANCELLED: 4     // 已取消
} as const

/**
 * 状态文本映射
 */
export const STATUS_TEXT: Record<number, string> = {
  [INTERVIEW_STATUS.WAITING]: '等待开始',
  [INTERVIEW_STATUS.IN_PROGRESS]: '进行中',
  [INTERVIEW_STATUS.COMPLETED]: '已完成',
  [INTERVIEW_STATUS.PAUSED]: '已暂停',
  [INTERVIEW_STATUS.CANCELLED]: '已取消'
}

/**
 * 状态标签类型映射
 */
export const STATUS_TAG_TYPES: Record<number, string> = {
  [INTERVIEW_STATUS.WAITING]: 'info',
  [INTERVIEW_STATUS.IN_PROGRESS]: 'warning',
  [INTERVIEW_STATUS.COMPLETED]: 'success',
  [INTERVIEW_STATUS.PAUSED]: 'warning',
  [INTERVIEW_STATUS.CANCELLED]: 'danger'
}

/**
 * 回答类型
 */
export const ANSWER_TYPE = {
  TEXT: 1   // 文字回答
} as const

/**
 * 获取分类标签类型
 */
export function getCategoryTagType(category: string): string {
  return CATEGORY_TAG_TYPES[category] || 'default'
}

/**
 * 获取难度标签类型
 */
export function getDifficultyTagType(difficulty: number): string {
  const level = DIFFICULTY_LEVELS.find(l => l.value === difficulty)
  return level?.type || 'default'
}

/**
 * 获取难度文本
 */
export function getDifficultyText(difficulty: number): string {
  const level = DIFFICULTY_LEVELS.find(l => l.value === difficulty)
  return level?.label || '未知'
}

/**
 * 获取状态文本
 */
export function getStatusText(status: number): string {
  return STATUS_TEXT[status] || '未知'
}

/**
 * 获取状态标签类型
 */
export function getStatusTagType(status: number): string {
  return STATUS_TAG_TYPES[status] || 'default'
}
