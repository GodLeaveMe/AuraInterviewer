<template>
  <div class="chat-interview">
    <!-- 头部信息 -->
    <div class="interview-header">
      <div class="session-info">
        <h2>{{ session?.jobPosition }} - AI面试</h2>
        <div class="session-meta">
          <span>面试时长: {{ formatDuration(elapsedTime) }}</span>
          <span>已回答: {{ answeredCount }}/{{ totalQuestions }}</span>
          <span class="status" :class="statusClass">{{ statusText }}</span>
          <span class="interview-mode" :class="'mode-' + session?.interviewMode">
            {{
              session?.interviewMode === 'text' ? '文字模式' :

              '未知模式'
            }}
          </span>
          <span v-if="aiModelName" class="ai-model">
            AI模型: {{ aiModelName }}
          </span>
        </div>
      </div>
      <div class="interview-actions">
        <el-button @click="pauseInterview" v-if="session?.status === 1" type="warning">
          暂停面试
        </el-button>
        <el-button @click="resumeInterview" v-if="session?.status === 4" type="primary">
          继续面试
        </el-button>
        <el-button @click="finishInterview" v-if="session?.status === 1 || session?.status === 4" type="success">
          结束面试
        </el-button>
        <el-button @click="viewReport" v-if="session?.status === 2" type="primary">
          查看报告
        </el-button>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-container" ref="chatContainer">
      <div class="chat-messages">
        <!-- 欢迎消息 -->
        <div class="message ai-message" v-if="messages.length === 0">
          <div class="message-avatar">
            <el-avatar :size="40" src="/ai-avatar.png">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <p>您好！欢迎参加{{ session?.jobPosition }}面试。我是您的AI面试官，让我们开始吧！</p>
            </div>
            <div class="message-time">{{ formatTime(new Date()) }}</div>
          </div>
        </div>

        <!-- 聊天消息 -->
        <div 
          v-for="(message, index) in messages" 
          :key="index"
          class="message"
          :class="message.type === 'ai' ? 'ai-message' : 'user-message'"
        >
          <div class="message-avatar" v-if="message.type === 'ai'">
            <el-avatar :size="40" src="/ai-avatar.png">AI</el-avatar>
          </div>
          
          <div class="message-content">
            <div class="message-bubble">
              <p>{{ message.content }}</p>
              
              <!-- AI评分显示 -->
              <div v-if="message.type === 'ai' && message.evaluation" class="evaluation-summary">
                <div class="score-badge">
                  <span class="score">{{ message.evaluation.score }}</span>
                  <span class="score-label">分</span>
                </div>
                <div class="evaluation-brief">
                  <el-tag v-for="strength in message.evaluation.strengths?.slice(0, 2)" 
                          :key="strength" type="success" size="small">
                    {{ strength }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>

          <div class="message-avatar" v-if="message.type === 'user'">
            <el-avatar :size="40" :src="userAvatar">{{ userName }}</el-avatar>
          </div>
        </div>

        <!-- AI正在输入 -->
        <div v-if="aiTyping" class="message ai-message typing">
          <div class="message-avatar">
            <el-avatar :size="40" src="/ai-avatar.png">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>



    <!-- 输入区域 -->
    <div class="chat-input" v-if="session?.status === 1 && session?.interviewMode === 'text'">
      <div class="input-container">
        <el-input
          v-model="currentAnswer"
          type="textarea"
          :rows="3"
          :placeholder="interviewCompleted ? '面试已完成，无法继续回答' : '请输入您的回答...'"
          :disabled="aiTyping || interviewCompleted"
          @keydown.ctrl.enter="submitAnswer"
          resize="none"
        />
        <div class="input-actions">
          <div class="input-tools">

            <span class="word-count">{{ currentAnswer.length }}/1000</span>
          </div>
          <el-button
            @click="submitAnswer"
            type="primary"
            :disabled="!currentAnswer.trim() || aiTyping || interviewCompleted"
            :loading="submitting"
          >
            {{ interviewCompleted ? '面试已完成' : '发送回答' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 面试暂停提示 -->
    <div v-if="session?.status === 4" class="interview-paused">
      <el-result
        icon="warning"
        title="面试已暂停"
        sub-title="面试已暂停，点击继续面试按钮可以恢复面试"
      >
        <template #extra>
          <el-button type="primary" @click="resumeInterview">继续面试</el-button>
          <el-button type="success" @click="finishInterview">结束面试</el-button>
        </template>
      </el-result>
    </div>

    <!-- 面试完成提示 -->
    <div v-if="session?.status === 2" class="interview-completed">
      <el-result
        icon="success"
        title="面试已完成"
        sub-title="感谢您参加本次面试，您可以查看详细的面试报告"
      >
        <template #extra>
          <el-button type="primary" @click="viewReport">查看面试报告</el-button>
          <el-button @click="backToList">返回面试列表</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { interviewApi } from '@/api/interview'
import { apiConfigApi } from '@/api/apiConfig'
import type { InterviewSession } from '@/types/interview'


// 定义消息类型
interface ChatMessage {
  type: 'ai' | 'user'
  content: string
  timestamp: string | Date
  questionId?: string | number
  evaluation?: any
  isEndMessage?: boolean
}

// 路由和基础数据
const route = useRoute()
const router = useRouter()
const sessionId = ref(route.params.id as string)

// 面试数据
const session = ref<InterviewSession | null>(null)
const messages = ref<ChatMessage[]>([])
const currentAnswer = ref('')
const aiTyping = ref(false)
const submitting = ref(false)
const interviewCompleted = ref(false)


// 计时器
const elapsedTime = ref(0)
const timer = ref<NodeJS.Timeout | null>(null)

// 用户信息
const userName = ref('用户')
const userAvatar = ref('')

// AI模型信息
const aiModels = ref<any[]>([])
const userModels = ref<any[]>([])

// 计算属性
const answeredCount = computed(() => {
  return messages.value.filter(m => m.type === 'user').length
})

const totalQuestions = computed(() => {
  return session.value?.questionCount || 10
})

const statusClass = computed(() => {
  switch (session.value?.status) {
    case 0: return 'status-pending'
    case 1: return 'status-active'
    case 2: return 'status-completed'
    case 3: return 'status-cancelled'
    case 4: return 'status-paused'
    default: return ''
  }
})

const statusText = computed(() => {
  switch (session.value?.status) {
    case 0: return '未开始'
    case 1: return '进行中'
    case 2: return '已完成'
    case 3: return '已取消'
    case 4: return '已暂停'
    default: return '未知'
  }
})

// AI模型名称
const aiModelName = computed(() => {
  if (!session.value?.aiModel) return ''

  const aiModelValue = String(session.value.aiModel)

  // 直接返回存储的模型名称（现在数据库中直接存储模型名称）
  return aiModelValue
})

// 聊天容器引用
const chatContainer = ref<HTMLElement | null>(null)


// 方法
const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

const formatTime = (date: string | Date) => {
  return new Date(date).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 加载AI模型
const loadAiModels = async () => {
  try {
    // 加载系统模型
    const systemResponse = await fetch('/api/ai/models')
    if (systemResponse.ok) {
      const systemData = await systemResponse.json()
      aiModels.value = systemData.data || []
    }

    // 加载用户模型 - 使用API配置的启用配置接口
    const userResponse = await apiConfigApi.getEnabledConfigs()
    userModels.value = userResponse.data || []
  } catch (error) {
    console.warn('加载AI模型失败:', error)
  }
}

// 加载面试会话
const loadSession = async () => {
  try {
    console.log('尝试加载面试会话ID:', sessionId.value)
    const response = await interviewApi.getInterviewSession(sessionId.value)
    console.log('加载面试会话响应:', response)
    session.value = response.data

    // 加载AI模型信息
    await loadAiModels()



    if (session.value.status === 0) {
      // 开始面试
      await startInterview()
    } else if (session.value.status === 1 || session.value.status === 4) {
      // 加载历史消息（进行中或暂停状态）
      await loadChatHistory()
    }
  } catch (error) {
    ElMessage.error('加载面试会话失败')
    router.push('/interview')
  }
}

// 开始面试
const startInterview = async () => {
  try {
    await interviewApi.startInterview(sessionId.value)
    if (session.value) {
      session.value.status = 1
    }
    
    // 开始计时
    startTimer()
    
    // 获取第一个问题
    await getNextQuestion()
  } catch (error) {
    ElMessage.error('开始面试失败')
  }
}

// 加载聊天历史
const loadChatHistory = async () => {
  try {
    const response = await interviewApi.getQaList(sessionId.value)
    const qaList = response.data
    
    messages.value = []
    
    for (const qa of qaList) {
      // 添加AI问题
      messages.value.push({
        type: 'ai',
        content: qa.question,
        timestamp: qa.createTime,
        questionId: qa.id
      })
      
      // 添加用户回答（如果有）
      if (qa.answer) {
        messages.value.push({
          type: 'user',
          content: qa.answer,
          timestamp: qa.answerTime || qa.createTime,
          questionId: qa.id
        })
        
        // 如果有评估结果，添加到AI消息中
        if (qa.answerData) {
          try {
            const evaluation = JSON.parse(qa.answerData)
            const lastAiMessage = messages.value.find(m => 
              m.type === 'ai' && m.questionId === qa.id
            )
            if (lastAiMessage) {
              lastAiMessage.evaluation = evaluation
            }
          } catch (e) {
            console.warn('解析评估数据失败:', e)
          }
        }
      }
    }
    
    scrollToBottom()
    
    // 如果面试还在进行中，开始计时
    if (session.value && session.value.status === 1) {
      startTimer()
    }
  } catch (error) {
    ElMessage.error('加载聊天历史失败')
  }
}

// 获取下一个问题
const getNextQuestion = async () => {
  try {
    aiTyping.value = true

    const response = await interviewApi.getNextQuestion(sessionId.value)
    const question = response.data

    if (question) {
      // 检查是否是特殊消息（结束、完成、取消、未开始）
      const specialIds = [-1, '-1', -2, '-2', -3, '-3', -4, '-4']
      const isSpecialMessage = specialIds.includes(question.id)
      const isEndMessage = question.id === -1 || question.id === '-1' // 达到最大题目数
      const isCompletedMessage = question.id === -2 || question.id === '-2' // 面试已完成
      const isCancelledMessage = question.id === -3 || question.id === '-3' // 面试已取消
      const isNotStartedMessage = question.id === -4 || question.id === '-4' // 面试未开始

      // 添加AI问题到聊天
      messages.value.push({
        type: 'ai',
        content: question.question,
        timestamp: new Date(),
        questionId: question.id,
        isEndMessage: isSpecialMessage
      })

      // 处理特殊消息
      if (isEndMessage) {
        interviewCompleted.value = true
        ElMessage.success('面试已完成所有题目！请点击结束面试按钮完成面试。')
      } else if (isCompletedMessage) {
        interviewCompleted.value = true
        // 面试已经结束，更新状态
        if (session.value) {
          session.value.status = 2
        }
        ElMessage.info('面试已完成，您可以查看面试报告。')
      } else if (isCancelledMessage) {
        interviewCompleted.value = true
        if (session.value) {
          session.value.status = 3
        }
        ElMessage.warning('面试已取消。')
      } else if (isNotStartedMessage) {
        interviewCompleted.value = true
        ElMessage.warning('面试尚未开始，请先开始面试。')
      }

      scrollToBottom()


    } else {
      // 这种情况不应该发生，因为后端现在总是返回消息
      ElMessage.error('无法获取面试问题')
    }
  } catch (error) {
    ElMessage.error('获取问题失败')
  } finally {
    aiTyping.value = false
  }
}

// 提交回答
const submitAnswer = async () => {
  if (!currentAnswer.value.trim()) {
    ElMessage.warning('请输入回答内容')
    return
  }

  if (interviewCompleted.value) {
    ElMessage.warning('面试已完成，无法继续回答')
    return
  }

  try {
    submitting.value = true
    
    // 添加用户回答到聊天
    const userMessage: ChatMessage = {
      type: 'user',
      content: currentAnswer.value,
      timestamp: new Date()
    }
    messages.value.push(userMessage)
    
    // 清空输入框
    const answer = currentAnswer.value
    currentAnswer.value = ''
    
    scrollToBottom()
    
    // 获取当前问题ID
    const lastAiMessage = messages.value.slice().reverse().find(m => m.type === 'ai')
    if (!lastAiMessage?.questionId) {
      ElMessage.error('无法找到当前问题')
      return
    }
    
    // 提交回答
    await interviewApi.submitAnswer({
      sessionId: sessionId.value,
      questionOrder: messages.value.filter(m => m.type === 'ai').length,
      answer: answer,
      answerType: 1, // 1-文本
      thinkingTime: 0
    })
    
    // 等待AI评估完成，然后获取下一个问题（如果面试未完成）
    setTimeout(async () => {
      if (!interviewCompleted.value) {
        await getNextQuestion()
      }
    }, 2000)
    
  } catch (error) {
    ElMessage.error('提交回答失败')
  } finally {
    submitting.value = false
  }
}



// 暂停面试
const pauseInterview = async () => {
  try {
    await ElMessageBox.confirm('确定要暂停面试吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await interviewApi.pauseInterview(sessionId.value)
    if (session.value) {
      session.value.status = 4 // 4-已暂停
    }
    stopTimer()

    ElMessage.success('面试已暂停')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('暂停面试失败')
    }
  }
}

// 继续面试
const resumeInterview = async () => {
  try {
    await interviewApi.resumeInterview(sessionId.value)
    if (session.value) {
      session.value.status = 1 // 1-进行中
    }
    startTimer()

    ElMessage.success('面试已继续')
  } catch (error) {
    ElMessage.error('继续面试失败')
  }
}

// 结束面试
const finishInterview = async () => {
  try {
    await ElMessageBox.confirm('确定要结束面试吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await interviewApi.finishInterview(sessionId.value)
    if (session.value) {
      session.value.status = 2
    }
    stopTimer()
    
    ElMessage.success('面试已结束')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('结束面试失败')
    }
  }
}

// 查看报告
const viewReport = () => {
  router.push(`/interview/report/${sessionId.value}`)
}

// 返回列表
const backToList = () => {
  router.push('/interview')
}

// 计时器相关
const startTimer = () => {
  if (timer.value) return
  
  timer.value = setInterval(() => {
    elapsedTime.value++
  }, 1000)
}

const stopTimer = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 生命周期
onMounted(() => {
  loadSession()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped lang="scss">
.chat-interview {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;

  .interview-header {
    background: white;
    padding: 16px 24px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    .session-info {
      h2 {
        margin: 0 0 8px 0;
        color: #303133;
        font-size: 20px;
      }

      .session-meta {
        display: flex;
        gap: 16px;
        font-size: 14px;
        color: #606266;

        .status {
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;

          &.status-pending {
            background: #f0f9ff;
            color: #0369a1;
          }

          &.status-active {
            background: #f0fdf4;
            color: #15803d;
          }

          &.status-completed {
            background: #fef3f2;
            color: #dc2626;
          }

          &.status-cancelled {
            background: #f9fafb;
            color: #6b7280;
          }

          &.status-paused {
            background: #fef3c7;
            color: #d97706;
          }
        }

        .interview-mode {
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;

          &.mode-text {
            background: #f0f9ff;
            color: #0369a1;
          }


        }

        .ai-model {
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
          background: #f0f9ff;
          color: #0369a1;
          border: 1px solid #bae6fd;
        }
      }
    }

    .interview-actions {
      display: flex;
      gap: 12px;
    }
  }

  .chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    .chat-messages {
      max-width: 800px;
      margin: 0 auto;

      .message {
        display: flex;
        margin-bottom: 24px;
        align-items: flex-start;

        &.ai-message {
          justify-content: flex-start;

          .message-content {
            margin-left: 12px;
          }

          .message-bubble {
            background: white;
            border: 1px solid #e4e7ed;
            border-radius: 0 12px 12px 12px;
          }
        }

        &.user-message {
          justify-content: flex-end;

          .message-content {
            margin-right: 12px;
            text-align: right;
          }

          .message-bubble {
            background: #409eff;
            color: white;
            border-radius: 12px 0 12px 12px;
          }
        }

        &.typing {
          .message-bubble {
            padding: 16px 20px;
          }
        }

        .message-content {
          max-width: 70%;

          .message-bubble {
            padding: 12px 16px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

            p {
              margin: 0;
              line-height: 1.6;
              word-wrap: break-word;
            }

            .evaluation-summary {
              margin-top: 12px;
              padding-top: 12px;
              border-top: 1px solid #f0f0f0;
              display: flex;
              align-items: center;
              gap: 12px;

              .score-badge {
                display: flex;
                align-items: baseline;
                gap: 2px;

                .score {
                  font-size: 18px;
                  font-weight: 600;
                  color: #409eff;
                }

                .score-label {
                  font-size: 12px;
                  color: #909399;
                }
              }

              .evaluation-brief {
                display: flex;
                gap: 4px;
                flex-wrap: wrap;
              }
            }
          }

          .message-time {
            font-size: 12px;
            color: #909399;
            margin-top: 4px;
          }
        }

        .message-avatar {
          flex-shrink: 0;
        }
      }
    }
  }

  .chat-input {
    background: white;
    border-top: 1px solid #e4e7ed;
    padding: 16px 24px;

    .input-container {
      max-width: 800px;
      margin: 0 auto;

      .el-textarea {
        margin-bottom: 12px;

        :deep(.el-textarea__inner) {
          border-radius: 8px;
          border: 1px solid #dcdfe6;
          resize: none;

          &:focus {
            border-color: #409eff;
          }
        }
      }

      .input-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .input-tools {
          display: flex;
          align-items: center;
          gap: 12px;

          .word-count {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }

  .interview-paused,
  .interview-completed {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: white;
  }
}

// 打字动画
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #409eff;
    animation: typing 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .chat-interview {
    .interview-header {
      padding: 12px 16px;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;

      .session-info h2 {
        font-size: 18px;
      }

      .session-meta {
        flex-wrap: wrap;
        gap: 8px;
      }
    }

    .chat-container {
      padding: 12px;

      .chat-messages .message .message-content {
        max-width: 85%;
      }
    }

    .chat-input {
      padding: 12px 16px;

      .input-actions {
        flex-direction: column;
        gap: 8px;
        align-items: stretch;

        .input-tools {
          justify-content: space-between;
        }
      }
    }
  }
}
</style>
