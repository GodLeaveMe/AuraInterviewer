<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1>欢迎回来，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}！</h1>
        <p>准备好开始您的AI面试之旅了吗？</p>
      </div>
      <div class="welcome-stats">
        <div class="stat-item">
          <div class="stat-number">{{ stats.totalInterviews }}</div>
          <div class="stat-label">总面试次数</div>
        </div>
        <div class="stat-item">
          <div class="stat-number">{{ stats.completedInterviews }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat-item">
          <div class="stat-number">{{ stats.averageScore }}</div>
          <div class="stat-label">平均分数</div>
        </div>
      </div>
    </div>
      <!-- 当前面试状态 -->
      <div v-if="interviewStore.currentSession" class="current-interview">
        <el-card class="interview-card">
          <template #header>
            <div class="card-header">
              <span>当前面试</span>
              <el-tag :type="getStatusType(interviewStore.currentSession.status)">
                {{ getStatusText(interviewStore.currentSession.status) }}
              </el-tag>
            </div>
          </template>
          
          <div class="interview-info">
            <h3>{{ interviewStore.currentSession.title }}</h3>
            <div class="interview-meta">
              <span>进度: {{ interviewStore.sessionProgress.toFixed(1) }}%</span>
              <span>已回答: {{ interviewStore.currentSession.answeredCount }}/{{ interviewStore.currentSession.questionCount }}</span>
              <span v-if="interviewStore.currentSession.startTime">
                开始时间: {{ formatTime(interviewStore.currentSession.startTime) }}
              </span>
            </div>
            
            <div class="interview-actions">
              <el-button
                v-if="interviewStore.currentSession.status === 0"
                type="primary"
                @click="startInterview"
              >
                开始面试
              </el-button>
              <el-button
                v-if="interviewStore.currentSession.status === 1"
                type="success"
                @click="continueInterview(interviewStore.currentSession.id)"
              >
                继续面试
              </el-button>
              <el-button
                v-if="interviewStore.currentSession.status === 4"
                type="success"
                @click="continueInterview(interviewStore.currentSession.id)"
              >
                继续面试
              </el-button>
              <el-button
                v-if="interviewStore.currentSession.status === 1 || interviewStore.currentSession.status === 4"
                type="warning"
                @click="finishInterview"
              >
                结束面试
              </el-button>
              <el-button
                v-if="interviewStore.currentSession.status === 1 || interviewStore.currentSession.status === 4"
                type="danger"
                @click="cancelInterview"
              >
                取消面试
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 快速开始 -->
      <div class="quick-start">
        <h2>快速开始</h2>
        <div class="template-grid">
          <div 
            v-for="template in popularTemplates" 
            :key="template.id"
            class="template-card"
            @click="selectTemplate(template)"
          >
            <div class="template-icon">
              <el-icon size="32"><Document /></el-icon>
            </div>
            <h3>{{ template.name }}</h3>
            <p>{{ template.description }}</p>
            <div class="template-meta">
              <el-tag size="small" :type="getDifficultyType(template.difficulty)">
                {{ getDifficultyText(template.difficulty) }}
              </el-tag>
              <span class="usage-count">{{ template.usageCount }} 次使用</span>
            </div>
          </div>
        </div>
        
        <div class="more-templates">
          <el-button type="primary" @click="goToTemplates">
            查看更多模板
          </el-button>
        </div>
      </div>

      <!-- 最近面试 -->
      <div class="recent-interviews">
        <h2>最近面试</h2>
        <el-table :data="recentInterviews" style="width: 100%">
          <el-table-column prop="title" label="面试标题" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分">
            <template #default="{ row }">
              <span v-if="row.score">{{ row.score }}分</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button 
                v-if="row.status === 2" 
                type="primary" 
                size="small"
                @click="viewReport(row.id)"
              >
                查看报告
              </el-button>
              <el-button
                v-if="row.status === 1 || row.status === 4"
                type="success"
                size="small"
                @click="continueInterview(row.id)"
              >
                继续面试
              </el-button>
              <el-button
                v-if="row.status === 4"
                type="warning"
                size="small"
                @click="finishInterviewFromHistory(row.id)"
              >
                结束面试
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="more-history">
          <el-button @click="goToHistory">
            查看全部历史
          </el-button>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useInterviewStore } from '@/stores/interview'
import type { InterviewTemplate, InterviewSession } from '@/types/interview'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const interviewStore = useInterviewStore()

const popularTemplates = ref<InterviewTemplate[]>([])
const recentInterviews = ref<InterviewSession[]>([])
const stats = ref({
  totalInterviews: 0,
  completedInterviews: 0,
  averageScore: 0
})

onMounted(async () => {
  try {
    // 获取当前面试状态
    await interviewStore.getCurrentSession()

    // 获取热门模板
    const templates = await interviewStore.getTemplates()
    popularTemplates.value = templates.slice(0, 6)

    // 获取最近面试
    const history = await interviewStore.getHistory(1, 5)
    recentInterviews.value = history

    // 计算统计数据
    stats.value = {
      totalInterviews: history.length,
      completedInterviews: history.filter((item: any) => item.status === 2).length,
      averageScore: history.length > 0 ?
        Math.round(history.reduce((sum: number, item: any) => sum + (item.score || 0), 0) / history.length) : 0
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    // 设置默认值
    stats.value = {
      totalInterviews: 0,
      completedInterviews: 0,
      averageScore: 0
    }
  }
})



// 开始面试
const startInterview = async () => {
  if (!interviewStore.currentSession) return

  try {
    await interviewStore.startInterview(Number(interviewStore.currentSession.id))
    // 导航到正确的面试聊天页面
    router.push(`/interview/chat/${interviewStore.currentSession.id}`)
  } catch (error) {
    console.error('开始面试失败:', error)
  }
}

// 继续面试
const continueInterview = (sessionId?: number | string) => {
  // 如果传入了sessionId，使用该ID；否则使用当前会话ID
  const targetSessionId = sessionId || interviewStore.currentSession?.id
  if (targetSessionId) {
    router.push(`/interview/chat/${targetSessionId}`)
  } else {
    ElMessage.error('无法找到面试会话')
  }
}

// 结束面试
const finishInterview = async () => {
  if (!interviewStore.currentSession) return

  try {
    await ElMessageBox.confirm('确定要结束当前面试吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await interviewStore.finishInterview(Number(interviewStore.currentSession.id))
    ElMessage.success('面试已结束')
    // 刷新当前会话状态
    await interviewStore.getCurrentSession()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('结束面试失败:', error)
    }
  }
}

// 从历史记录结束面试
const finishInterviewFromHistory = async (sessionId: number | string) => {
  try {
    await ElMessageBox.confirm('确定要结束这个面试吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await interviewStore.finishInterview(Number(sessionId))
    ElMessage.success('面试已结束')
    // 刷新数据
    await interviewStore.getCurrentSession()
    const history = await interviewStore.getHistory(1, 5)
    recentInterviews.value = history
  } catch (error) {
    if (error !== 'cancel') {
      console.error('结束面试失败:', error)
    }
  }
}

// 取消面试
const cancelInterview = async () => {
  if (!interviewStore.currentSession) return
  
  try {
    await ElMessageBox.confirm('确定要取消当前面试吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await interviewStore.cancelInterview(Number(interviewStore.currentSession.id))
    ElMessage.success('面试已取消')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消面试失败:', error)
    }
  }
}

// 选择模板
const selectTemplate = (template: InterviewTemplate) => {
  router.push(`/interview?templateId=${template.id}`)
}

// 查看报告
const viewReport = (sessionId: number | string) => {
  router.push(`/interview/report/${sessionId}`)
}

// 跳转到模板页面
const goToTemplates = () => {
  router.push('/interview')
}

// 跳转到历史页面
const goToHistory = () => {
  router.push('/interview/history')
}

// 获取状态类型
const getStatusType = (status: number) => {
  const types = ['info', 'warning', 'success', 'danger', 'primary']
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const texts = ['未开始', '进行中', '已完成', '已取消', '已暂停']
  return texts[status] || `未知(${status})`
}

// 获取难度类型
const getDifficultyType = (difficulty: number) => {
  const types = ['success', 'warning', 'danger']
  return types[difficulty - 1] || 'info'
}

// 获取难度文本
const getDifficultyText = (difficulty: number) => {
  const texts = ['初级', '中级', '高级']
  return texts[difficulty - 1] || '未知'
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 0;
  margin-bottom: 32px;

  .welcome-content {
    text-align: center;
    margin-bottom: 32px;

    h1 {
      margin: 0 0 8px 0;
      font-size: 32px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .welcome-stats {
    display: flex;
    justify-content: center;
    gap: 48px;

    .stat-item {
      text-align: center;

      .stat-number {
        font-size: 36px;
        font-weight: 700;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        opacity: 0.8;
      }
    }
  }
}

.current-interview {
  margin-bottom: 32px;
  
  .interview-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
  
  .interview-info h3 {
    margin: 0 0 16px 0;
    font-size: 20px;
    color: #333;
  }
  
  .interview-meta {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    color: #666;
    font-size: 14px;
  }
  
  .interview-actions {
    display: flex;
    gap: 12px;
  }
}

.quick-start, .recent-interviews {
  margin-bottom: 32px;
  
  h2 {
    margin: 0 0 20px 0;
    font-size: 20px;
    color: #333;
  }
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.template-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }
  
  .template-icon {
    text-align: center;
    margin-bottom: 16px;
    color: #409eff;
  }
  
  h3 {
    margin: 0 0 8px 0;
    font-size: 16px;
    color: #333;
  }
  
  p {
    margin: 0 0 16px 0;
    color: #666;
    font-size: 14px;
    line-height: 1.5;
  }
  
  .template-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .usage-count {
      font-size: 12px;
      color: #999;
    }
  }
}

.more-templates, .more-history {
  text-align: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .dashboard-main {
    padding: 16px;
  }
  
  .template-grid {
    grid-template-columns: 1fr;
  }
  
  .interview-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .interview-actions {
    flex-wrap: wrap;
  }
}
</style>
