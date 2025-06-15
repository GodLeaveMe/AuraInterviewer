import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import type {
  InterviewTemplate,
  InterviewSession,
  InterviewQuestion,
  InterviewSessionRequest
} from '@/types/interview'
import { interviewApi } from '@/api/interview'
import { ElMessage } from 'element-plus'

export const useInterviewStore = defineStore('interview', () => {
  // 状态
  const currentSession = ref<InterviewSession | null>(null)
  const templates = ref<InterviewTemplate[]>([])
  const history = ref<InterviewSession[]>([])
  const currentQuestion = ref<InterviewQuestion | null>(null)
  const qaList = ref<InterviewQuestion[]>([])
  const loading = ref(false)

  // 计算属性
  const isInterviewing = computed(() => currentSession.value?.status === 1)
  const sessionProgress = computed(() => {
    if (!currentSession.value) return 0
    const { answeredCount, questionCount } = currentSession.value
    return questionCount > 0 ? (answeredCount / questionCount) * 100 : 0
  })

  // 获取面试模板列表
  const getTemplates = async (category?: string, difficulty?: number) => {
    try {
      loading.value = true
      const response = await interviewApi.getPublicInterviewTemplates()
      templates.value = response.data
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '获取模板列表失败')
      return []
    } finally {
      loading.value = false
    }
  }

  // 创建面试会话
  const createSession = async (request: InterviewSessionRequest) => {
    try {
      loading.value = true
      const response = await interviewApi.createInterviewSession(request)
      currentSession.value = response.data
      ElMessage.success('面试会话创建成功')
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '创建面试会话失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 开始面试
  const startInterview = async (sessionId: number) => {
    try {
      loading.value = true
      const response = await interviewApi.startInterview(sessionId)
      currentSession.value = response.data
      
      // 获取第一个问题
      await getNextQuestion(sessionId)
      
      ElMessage.success('面试已开始')
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '开始面试失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 结束面试
  const finishInterview = async (sessionId: number) => {
    try {
      loading.value = true
      const response = await interviewApi.finishInterview(sessionId)
      currentSession.value = response.data
      currentQuestion.value = null
      ElMessage.success('面试已结束')
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '结束面试失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 取消面试
  const cancelInterview = async (sessionId: number) => {
    try {
      loading.value = true
      const response = await interviewApi.cancelInterview(sessionId)
      currentSession.value = response.data
      currentQuestion.value = null
      ElMessage.success('面试已取消')
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '取消面试失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 获取下一个问题
  const getNextQuestion = async (sessionId: number) => {
    try {
      const response = await interviewApi.getNextQuestion(sessionId)
      currentQuestion.value = response.data
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '获取问题失败')
      return null
    }
  }

  // 提交回答
  const submitAnswer = async (answer: string, answerType: 'text' = 'text', thinkingTime?: number) => {
    if (!currentSession.value || !currentQuestion.value) {
      ElMessage.error('当前没有进行中的面试')
      return null
    }

    try {
      loading.value = true
      const response = await interviewApi.submitAnswer({
        sessionId: currentSession.value.id,
        questionOrder: currentQuestion.value.questionOrder,
        answer,
        answerType: 1, // 文本回答
        thinkingTime
      })

      // 更新问答列表
      const index = qaList.value.findIndex(qa => qa.id === response.data.id)
      if (index >= 0) {
        qaList.value[index] = response.data
      } else {
        qaList.value.push(response.data)
      }

      // 更新会话信息
      if (currentSession.value) {
        currentSession.value.answeredCount += 1
      }

      ElMessage.success('回答提交成功')
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '提交回答失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 获取当前面试会话
  const getCurrentSession = async () => {
    try {
      const response = await interviewApi.getCurrentSession()
      currentSession.value = response.data

      if (response.data) {
        // 获取问答列表
        await getQaList(Number(response.data.id))

        // 如果面试进行中，获取当前问题
        if (response.data.status === 1) {
          await getNextQuestion(Number(response.data.id))
        }
      }

      return response.data
    } catch (error: any) {
      console.error('获取当前面试会话失败:', error)
      // 如果没有当前会话，返回null而不是抛出错误
      currentSession.value = null
      return null
    }
  }

  // 获取问答列表
  const getQaList = async (sessionId: number) => {
    try {
      const response = await interviewApi.getQaList(sessionId)
      qaList.value = response.data
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '获取问答记录失败')
      return []
    }
  }

  // 获取面试历史
  const getHistory = async (page = 1, size = 10) => {
    try {
      loading.value = true
      const response = await interviewApi.getInterviewHistory(page, size)
      history.value = response.data
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '获取面试历史失败')
      return []
    } finally {
      loading.value = false
    }
  }

  // 生成面试报告
  const generateReport = async (sessionId: number | string) => {
    try {
      loading.value = true
      const response = await interviewApi.generateReport(sessionId)
      return response.data
    } catch (error: any) {
      ElMessage.error(error.message || '生成面试报告失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  // 清空当前面试状态
  const clearCurrentInterview = () => {
    currentSession.value = null
    currentQuestion.value = null
    qaList.value = []
  }

  return {
    // 状态
    currentSession: readonly(currentSession),
    templates: readonly(templates),
    history: readonly(history),
    currentQuestion: readonly(currentQuestion),
    qaList: readonly(qaList),
    loading: readonly(loading),

    // 计算属性
    isInterviewing,
    sessionProgress,

    // 方法
    getTemplates,
    createSession,
    startInterview,
    finishInterview,
    cancelInterview,
    getNextQuestion,
    submitAnswer,
    getCurrentSession,
    getQaList,
    getHistory,
    generateReport,
    clearCurrentInterview
  }
})
