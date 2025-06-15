<template>
  <div class="interview-report">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="8" animated />
    </div>
    
    <div v-else-if="session" class="report-content">
      <!-- 报告头部 -->
      <div class="report-header">
        <div class="header-info">
          <h1>{{ session.title }}</h1>
          <div class="session-meta">
            <el-tag :type="getStatusType(session.status)">
              {{ getStatusText(session.status) }}
            </el-tag>
            <span>{{ formatTime(session.createTime) }}</span>
          </div>
        </div>
        
        <div class="header-actions">
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" @click="exportReport">
            导出报告
          </el-button>
          <el-button type="success" @click="shareReport">
            分享报告
          </el-button>
        </div>
      </div>

      <!-- 总体评分 -->
      <div class="score-overview">
        <el-card>
          <div class="score-display">
            <div class="main-score">
              <div class="score-circle">
                <el-progress
                  type="circle"
                  :percentage="getFinalScore()"
                  :width="120"
                  :stroke-width="8"
                  :color="getScoreColor(getFinalScore())"
                >
                  <template #default>
                    <span class="score-text">{{ getFinalScore().toFixed(1) }}</span>
                  </template>
                </el-progress>
              </div>
              <div class="score-label">综合评分</div>
            </div>

            <div class="score-breakdown">
              <div class="breakdown-item">
                <div class="breakdown-label">技术能力 (50%)</div>
                <div class="breakdown-score">{{ getTechnicalScore().toFixed(1) }}分</div>
                <el-progress
                  :percentage="getTechnicalScore()"
                  :stroke-width="6"
                  :show-text="false"
                  :color="getScoreColor(getTechnicalScore())"
                />
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">情感表达 (30%)</div>
                <div class="breakdown-score">{{ getEmotionScore().toFixed(1) }}分</div>
                <el-progress
                  :percentage="getEmotionScore()"
                  :stroke-width="6"
                  :show-text="false"
                  :color="getScoreColor(getEmotionScore())"
                />
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">自信度 (20%)</div>
                <div class="breakdown-score">{{ getConfidenceScore().toFixed(1) }}分</div>
                <el-progress
                  :percentage="getConfidenceScore()"
                  :stroke-width="6"
                  :show-text="false"
                  :color="getScoreColor(getConfidenceScore())"
                />
              </div>
            </div>

            <div class="score-details">
              <div class="detail-item">
                <span class="label">面试时长</span>
                <span class="value">{{ formatDuration(session.duration || 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">回答问题</span>
                <span class="value">{{ session.answeredCount }}/{{ session.questionCount }}</span>
              </div>
              <div class="detail-item">
                <span class="label">完成度</span>
                <span class="value">{{ ((session.answeredCount / session.questionCount) * 100).toFixed(1) }}%</span>
              </div>
              <div class="detail-item">
                <span class="label">平均用时</span>
                <span class="value">{{ getAverageTime() }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 能力雷达图 -->
      <div class="ability-radar">
        <el-card>
          <template #header>
            <h3>能力分析</h3>
          </template>
          <div class="radar-chart">
            <div ref="radarChartRef" style="width: 100%; height: 400px;"></div>
          </div>
        </el-card>
      </div>

      <!-- 问答详情 -->
      <div class="qa-details">
        <el-card>
          <template #header>
            <h3>问答详情</h3>
          </template>
          
          <!-- 调试信息 -->
          <div v-if="qaList.length === 0" class="debug-info">
            <p>问答列表为空，请检查数据加载</p>
            <p>会话ID: {{ session?.id }}</p>
            <p>会话状态: {{ session?.status }}</p>
          </div>

          <div class="qa-list">
            <div
              v-for="(qa, index) in qaList"
              :key="qa.id"
              class="qa-item"
            >
              <div class="qa-header">
                <div class="question-info">
                  <span class="question-number">第{{ qa.questionOrder }}题</span>
                  <div class="score-tags">
                    <el-tag v-if="qa.score" :type="getScoreTagType(qa.score)" size="small">
                      技术: {{ qa.score }}分
                    </el-tag>
                    <el-tag v-if="qa.emotionScore" type="success" size="small">
                      情感: {{ qa.emotionScore }}分
                    </el-tag>
                    <el-tag v-if="qa.confidenceScore" type="warning" size="small">
                      自信: {{ qa.confidenceScore }}分
                    </el-tag>
                  </div>
                </div>
                <div class="answer-meta">
                  <span v-if="qa.thinkingTime">思考时间: {{ qa.thinkingTime }}秒</span>

                </div>
              </div>
              
              <div class="question-content">
                <h4>问题：</h4>
                <p>{{ qa.question }}</p>
              </div>
              
              <div v-if="qa.answer" class="answer-content">
                <h4>回答：</h4>
                <div class="text-answer">
                  <p>{{ qa.answer }}</p>
                </div>
              </div>
              
              <!-- 详细评分 -->
              <div class="detailed-scores">
                <h4>详细评分：</h4>
                <div class="score-grid">
                  <div class="score-item">
                    <span class="score-name">技术准确性</span>
                    <div class="score-bar">
                      <el-progress
                        :percentage="qa.score || 0"
                        :stroke-width="8"
                        :color="getScoreColor(qa.score || 0)"
                      />
                    </div>
                    <span class="score-value">{{ (qa.score || 0).toFixed(1) }}分</span>
                  </div>
                  <div class="score-item">
                    <span class="score-name">情感表达</span>
                    <div class="score-bar">
                      <el-progress
                        :percentage="qa.emotionScore || 0"
                        :stroke-width="8"
                        color="#67c23a"
                      />
                    </div>
                    <span class="score-value">{{ (qa.emotionScore || 0).toFixed(1) }}分</span>
                  </div>
                  <div class="score-item">
                    <span class="score-name">自信度</span>
                    <div class="score-bar">
                      <el-progress
                        :percentage="qa.confidenceScore || 0"
                        :stroke-width="8"
                        color="#e6a23c"
                      />
                    </div>
                    <span class="score-value">{{ (qa.confidenceScore || 0).toFixed(1) }}分</span>
                  </div>
                </div>


              </div>

              <!-- AI反馈内容 -->
              <div class="ai-feedback-section">
                <div v-if="hasAiFeedback(qa)" class="ai-feedback">
                  <div class="feedback-header">
                    <h4>AI评价：</h4>
                    <el-button
                      v-if="qa.answer"
                      size="small"
                      type="primary"
                      :loading="refreshingEvaluation[qa.id]"
                      @click="refreshEvaluation(qa)"
                    >
                      {{ refreshingEvaluation[qa.id] ? '重新评估中...' : '重新评估' }}
                    </el-button>
                  </div>
                  <div class="feedback-content" v-html="formatFeedback(qa.aiFeedback || '')"></div>
                </div>

                <!-- 无AI反馈时的提示 -->
                <div v-else-if="qa.answer" class="no-feedback">
                  <div class="no-feedback-content">
                    <el-icon><Warning /></el-icon>
                    <span>暂无AI评价</span>
                    <el-button
                      size="small"
                      type="primary"
                      :loading="refreshingEvaluation[qa.id]"
                      @click="refreshEvaluation(qa)"
                    >
                      {{ refreshingEvaluation[qa.id] ? '获取评价中...' : '获取AI评价' }}
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- AI思考过程 -->
              <div v-if="hasAiThinking(qa)" class="ai-thinking">
                <h4>AI思考过程：</h4>
                <div class="thinking-content">
                  <el-collapse>
                    <el-collapse-item title="查看AI的详细思考过程" name="thinking">
                      <div class="thinking-text">
                        {{ getThinkingContent(qa) }}
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </div>

              <!-- 调试信息（开发环境） -->
              <div v-if="isDevelopment && qa.answer" class="debug-info" style="background: #f0f0f0; padding: 8px; margin: 8px 0; font-size: 12px; border-radius: 4px;">
                <details>
                  <summary>调试信息 (点击展开)</summary>
                  <div style="margin-top: 8px;">
                    <p><strong>问题ID:</strong> {{ qa.id }}</p>
                    <p><strong>aiFeedback:</strong> {{ qa.aiFeedback ? '有内容' : '无内容' }}</p>
                    <p><strong>aiThinking:</strong> {{ qa.aiThinking ? '有内容' : '无内容' }}</p>
                    <p><strong>answerData:</strong> {{ qa.answerData ? '有内容' : '无内容' }}</p>
                    <p><strong>评估状态:</strong> {{ getEvaluationStatus(qa) }}</p>
                  </div>
                </details>
              </div>

              <!-- 详细评估数据 -->
              <div v-if="getAnswerData(qa.answerData)" class="detailed-evaluation">
                <h4>详细分析：</h4>
                <div class="evaluation-content">
                  <!-- AI总体反馈 -->
                  <div v-if="getAnswerData(qa.answerData)?.feedback" class="ai-detailed-feedback">
                    <h5>AI详细反馈：</h5>
                    <div class="feedback-text">
                      {{ getAnswerData(qa.answerData).feedback }}
                    </div>
                  </div>

                  <!-- 优势分析 -->
                  <div v-if="getAnswerData(qa.answerData)?.strengths" class="strengths">
                    <h5>优势：</h5>
                    <ul>
                      <li v-for="strength in getAnswerData(qa.answerData).strengths" :key="strength">
                        {{ strength }}
                      </li>
                    </ul>
                  </div>

                  <!-- 不足分析 -->
                  <div v-if="getAnswerData(qa.answerData)?.weaknesses" class="weaknesses">
                    <h5>不足：</h5>
                    <ul>
                      <li v-for="weakness in getAnswerData(qa.answerData).weaknesses" :key="weakness">
                        {{ weakness }}
                      </li>
                    </ul>
                  </div>

                  <!-- 改进建议 -->
                  <div v-if="getAnswerData(qa.answerData)?.suggestions" class="suggestions">
                    <h5>改进建议：</h5>
                    <ul>
                      <li v-for="suggestion in getAnswerData(qa.answerData).suggestions" :key="suggestion">
                        {{ suggestion }}
                      </li>
                    </ul>
                  </div>

                  <!-- 技术细节评分 -->
                  <div v-if="hasDetailedScores(qa)" class="detailed-scores-breakdown">
                    <h5>技术细节评分：</h5>
                    <div class="score-breakdown">
                      <div v-if="getAnswerData(qa.answerData)?.technical_accuracy" class="breakdown-item">
                        <span class="breakdown-label">技术准确性：</span>
                        <span class="breakdown-value">{{ getAnswerData(qa.answerData).technical_accuracy.toFixed(1) }}分</span>
                      </div>
                      <div v-if="getAnswerData(qa.answerData)?.completeness" class="breakdown-item">
                        <span class="breakdown-label">完整性：</span>
                        <span class="breakdown-value">{{ getAnswerData(qa.answerData).completeness.toFixed(1) }}分</span>
                      </div>
                      <div v-if="getAnswerData(qa.answerData)?.clarity" class="breakdown-item">
                        <span class="breakdown-label">清晰度：</span>
                        <span class="breakdown-value">{{ getAnswerData(qa.answerData).clarity.toFixed(1) }}分</span>
                      </div>
                      <div v-if="getAnswerData(qa.answerData)?.logic" class="breakdown-item">
                        <span class="breakdown-label">逻辑性：</span>
                        <span class="breakdown-value">{{ getAnswerData(qa.answerData).logic.toFixed(1) }}分</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="qa.keywords && qa.keywords.length > 0" class="keywords">
                <h4>关键词：</h4>
                <div class="keyword-tags">
                  <el-tag
                    v-for="keyword in qa.keywords"
                    :key="keyword"
                    size="small"
                    effect="plain"
                  >
                    {{ keyword }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- AI总结 -->
      <div v-if="session.summary || session.feedback" class="ai-summary">
        <el-card>
          <template #header>
            <h3>AI总结与建议</h3>
          </template>
          
          <div v-if="session.summary" class="summary-section">
            <h4>面试总结</h4>
            <div class="summary-content" v-html="formatSummary(session.summary)"></div>
          </div>
          
          <div v-if="session.feedback" class="feedback-section">
            <h4>改进建议</h4>
            <div class="feedback-content" v-html="formatSummary(session.feedback)"></div>
          </div>
        </el-card>
      </div>


    </div>
    
    <div v-else class="error-state">
      <el-empty description="报告不存在或已被删除" />
      <el-button type="primary" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<style scoped>
.score-breakdown {
  margin-left: 40px;
  flex: 1;
}

.breakdown-item {
  margin-bottom: 16px;
}

.breakdown-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.breakdown-score {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.score-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detailed-scores {
  margin: 16px 0;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-name {
  min-width: 80px;
  font-size: 14px;
  color: #666;
}

.score-bar {
  flex: 1;
}

.score-value {
  min-width: 50px;
  font-weight: bold;
  color: #333;
}

.detailed-evaluation {
  margin: 16px 0;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.detailed-evaluation h5 {
  margin: 12px 0 8px 0;
  color: #333;
  font-size: 14px;
}

.detailed-evaluation ul {
  margin: 0;
  padding-left: 20px;
}

.detailed-evaluation li {
  margin-bottom: 4px;
  color: #666;
  line-height: 1.5;
}

.strengths {
  border-left: 3px solid #67c23a;
  padding-left: 12px;
  margin-bottom: 16px;
}

.weaknesses {
  border-left: 3px solid #f56c6c;
  padding-left: 12px;
  margin-bottom: 16px;
}

.suggestions {
  border-left: 3px solid #e6a23c;
  padding-left: 12px;
}
</style>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

import { useInterviewStore } from '@/stores/interview'
import { interviewApi } from '@/api/interview'
import type { InterviewSession, InterviewQuestion } from '@/types/interview'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()

const session = ref<InterviewSession | null>(null)
const qaList = ref<InterviewQuestion[]>([])
const detailedReport = ref<any>(null)
const loading = ref(true)
const radarChartRef = ref<HTMLDivElement>()
const refreshingEvaluation = ref<Record<string | number, boolean>>({})

onMounted(async () => {
  const param = route.params.sessionId as string

  if (param === 'latest') {
    // 使用0作为特殊标识，让后端自动查找用户的最新完成面试
    await loadReport(0)
  } else {
    // 直接使用字符串形式的sessionId，避免Number()导致的精度丢失
    await loadReport(param)
  }
})

// 加载报告数据
const loadReport = async (sessionId: number | string) => {
  try {
    loading.value = true

    // 如果sessionId为0，获取最新完成的面试会话
    if (sessionId === 0) {
      try {
        const latestResponse = await interviewApi.getLatestCompletedSession()
        session.value = latestResponse.data
        sessionId = session.value.id
        console.log('获取到最新完成的会话:', session.value)
      } catch (error) {
        console.error('获取最新完成会话失败:', error)
        ElMessage.error('没有找到已完成的面试会话')
        goBack()
        return
      }
    } else {
      // 获取指定会话详情
      try {
        const sessionResponse = await interviewApi.getInterviewSession(sessionId)
        session.value = sessionResponse.data
        console.log('获取到指定会话:', session.value)
      } catch (error) {
        console.error('获取指定会话失败:', error)
        // 如果获取失败，尝试从面试历史中获取
        try {
          const historyResponse = await interviewApi.getInterviewHistory(1, 50)
          const targetSession = historyResponse.data.find((s: any) => String(s.id) === String(sessionId))
          if (targetSession) {
            session.value = targetSession
            console.log('从历史中获取到会话:', session.value)
          } else {
            throw new Error('会话不存在')
          }
        } catch (historyError) {
          console.error('从历史获取会话失败:', historyError)
          ElMessage.error('无法找到指定的面试会话')
          goBack()
          return
        }
      }
    }

    // 获取详细报告数据
    try {
      const detailedResponse = await interviewApi.getDetailedReport(sessionId)
      detailedReport.value = detailedResponse.data

      // 从详细报告中提取问答列表
      qaList.value = (detailedReport.value.questionDetails || []).map((qa: any) => ({
        ...qa,
        // 处理关键词字段，如果是字符串则解析为数组
        keywords: typeof qa.keywords === 'string' ?
          (qa.keywords ? qa.keywords.split(',') : []) :
          (qa.keywords || [])
      }))

      console.log('获取到详细报告:', detailedReport.value)
      console.log('获取到问答列表:', qaList.value)
    } catch (error) {
      console.error('获取详细报告失败，尝试获取基础问答列表:', error)
      // 如果详细报告失败，回退到基础问答列表
      try {
        const qaResponse = await interviewApi.getQaList(sessionId)
        qaList.value = (qaResponse.data || []).map((qa: any) => ({
          ...qa,
          keywords: typeof qa.keywords === 'string' ?
            (qa.keywords ? qa.keywords.split(',') : []) :
            (qa.keywords || [])
        }))
        console.log('获取到基础问答列表:', qaList.value)
      } catch (qaError) {
        console.error('获取问答列表失败:', qaError)
        qaList.value = []
      }
    }

    // 如果没有总结，生成总结
    if (session.value && session.value.status === 2 && !session.value.summary) {
      await generateSummary(sessionId)
    }
  } catch (error) {
    console.error('加载报告失败:', error)
    ElMessage.error('加载报告失败')
    goBack()
  } finally {
    loading.value = false
    // 初始化雷达图
    await nextTick()
    initRadarChart()
  }
}

// 生成AI总结
const generateSummary = async (sessionId: number | string) => {
  try {
    const summary = await interviewStore.generateReport(sessionId)
    if (session.value) {
      session.value.summary = summary
    }
  } catch (error) {
    console.error('生成总结失败:', error)
  }
}

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 导出报告
const exportReport = () => {
  if (!session.value) return

  try {
    // 生成报告内容
    const reportContent = generateReportContent()

    // 创建Blob对象
    const blob = new Blob([reportContent], { type: 'text/plain;charset=utf-8' })

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `面试报告_${session.value.title}_${new Date().toLocaleDateString()}.txt`

    // 触发下载
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('报告导出成功')
  } catch (error) {
    console.error('导出报告失败:', error)
    ElMessage.error('导出报告失败')
  }
}

// 分享报告
const shareReport = async () => {
  if (!session.value) return

  try {
    const shareData = {
      title: `面试报告 - ${session.value.title}`,
      text: `我完成了"${session.value.title}"面试，总分${getFinalScore().toFixed(1)}分`,
      url: window.location.href
    }

    if (navigator.share) {
      // 使用原生分享API
      await navigator.share(shareData)
      ElMessage.success('分享成功')
    } else {
      // 复制链接到剪贴板
      await navigator.clipboard.writeText(window.location.href)
      ElMessage.success('链接已复制到剪贴板')
    }
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败')
  }
}

// 生成报告内容
const generateReportContent = () => {
  if (!session.value) return ''

  let content = `面试报告\n`
  content += `==========================================\n\n`
  content += `面试标题: ${session.value.title}\n`
  content += `面试时间: ${formatTime(session.value.createTime)}\n`
  content += `面试时长: ${formatDuration(session.value.duration || 0)}\n`
  content += `回答问题: ${session.value.answeredCount}/${session.value.questionCount}\n\n`

  content += `评分详情:\n`
  content += `------------------------------------------\n`
  content += `综合评分: ${getFinalScore().toFixed(1)}分\n`
  content += `技术能力: ${getTechnicalScore().toFixed(1)}分 (权重50%)\n`
  content += `情感表达: ${getEmotionScore().toFixed(1)}分 (权重30%)\n`
  content += `自信度: ${getConfidenceScore().toFixed(1)}分 (权重20%)\n\n`

  if (qaList.value.length > 0) {
    content += `问答详情:\n`
    content += `------------------------------------------\n`
    qaList.value.forEach((qa) => {
      content += `第${qa.questionOrder}题:\n`
      content += `问题: ${qa.question}\n`
      if (qa.answer) {
        content += `回答: ${qa.answer}\n`
      }
      content += `技术分数: ${(qa.score || 0).toFixed(1)}分\n`
      content += `情感分数: ${(qa.emotionScore || 0).toFixed(1)}分\n`
      content += `自信分数: ${(qa.confidenceScore || 0).toFixed(1)}分\n`
      if (qa.aiFeedback) {
        content += `AI评价: ${qa.aiFeedback}\n`
      }

      // 添加AI思考过程
      const reasoningContent = getReasoningContent(qa)
      if (reasoningContent) {
        content += `AI思考过程: ${reasoningContent}\n`
      }

      // 添加详细评估数据
      const answerData = getAnswerData(qa.answerData)
      if (answerData) {
        if (answerData.feedback) {
          content += `详细反馈: ${answerData.feedback}\n`
        }
        if (answerData.strengths && answerData.strengths.length > 0) {
          content += `优势: ${answerData.strengths.join(', ')}\n`
        }
        if (answerData.weaknesses && answerData.weaknesses.length > 0) {
          content += `不足: ${answerData.weaknesses.join(', ')}\n`
        }
        if (answerData.suggestions && answerData.suggestions.length > 0) {
          content += `建议: ${answerData.suggestions.join(', ')}\n`
        }
      }

      content += `\n`
    })
  }

  if (detailedReport.value?.aiSummary) {
    content += `AI总结:\n`
    content += `------------------------------------------\n`
    content += `${detailedReport.value.aiSummary}\n\n`
  }

  content += `报告生成时间: ${new Date().toLocaleString()}\n`

  return content
}

// 获取状态类型
const getStatusType = (status: number) => {
  const types = ['info', 'warning', 'success', 'danger', 'primary']
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const texts = ['未开始', '进行中', '已完成', '已取消', '已暂停']
  return texts[status] || '未知'
}

// 获取分数颜色
const getScoreColor = (score: number) => {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#409eff'
  if (score >= 70) return '#e6a23c'
  return '#f56c6c'
}

// 获取分数标签类型
const getScoreTagType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  return 'danger'
}

// 获取平均用时
const getAverageTime = () => {
  if (!session.value || !session.value.duration || session.value.answeredCount === 0) {
    return '-'
  }
  const avgSeconds = session.value.duration / session.value.answeredCount
  return `${Math.round(avgSeconds)}秒`
}

// 获取技术能力分数
const getTechnicalScore = () => {
  if (detailedReport.value?.overallScores?.totalScore) {
    return detailedReport.value.overallScores.totalScore
  }
  return session.value?.score || 0
}

// 获取情感表达分数
const getEmotionScore = () => {
  if (detailedReport.value?.overallScores?.emotionScore) {
    return detailedReport.value.overallScores.emotionScore
  }
  return 0
}

// 获取自信度分数
const getConfidenceScore = () => {
  if (detailedReport.value?.overallScores?.confidenceScore) {
    return detailedReport.value.overallScores.confidenceScore
  }
  return 0
}

// 解析answerData
const getAnswerData = (answerData: any) => {
  if (!answerData) return null
  if (typeof answerData === 'string') {
    try {
      return JSON.parse(answerData)
    } catch {
      return null
    }
  }
  return answerData
}

// 获取AI思考过程内容
const getReasoningContent = (qa: any) => {
  // 首先检查直接的aiThinking字段
  if (qa.aiThinking) {
    return qa.aiThinking
  }

  // 然后检查answerData中的reasoning_content
  const answerData = getAnswerData(qa.answerData)
  if (answerData && answerData.reasoning_content) {
    return answerData.reasoning_content
  }

  return null
}

// 检查是否有详细评分数据
const hasDetailedScores = (qa: any) => {
  const answerData = getAnswerData(qa.answerData)
  if (!answerData) return false

  return answerData.technical_accuracy !== undefined ||
         answerData.completeness !== undefined ||
         answerData.clarity !== undefined ||
         answerData.logic !== undefined
}

// 检查是否有AI反馈
const hasAiFeedback = (qa: any) => {
  return qa.aiFeedback && qa.aiFeedback.trim().length > 0
}

// 检查是否有AI思考过程
const hasAiThinking = (qa: any) => {
  const directThinking = qa.aiThinking && qa.aiThinking.trim().length > 0
  const reasoningContent = getReasoningContent(qa)
  return directThinking || (reasoningContent && reasoningContent.trim().length > 0)
}

// 获取思考过程内容
const getThinkingContent = (qa: any) => {
  if (qa.aiThinking && qa.aiThinking.trim().length > 0) {
    return qa.aiThinking
  }
  return getReasoningContent(qa) || ''
}

// 开发环境标识
const isDevelopment = import.meta.env.DEV

// 获取评估状态
const getEvaluationStatus = (qa: any) => {
  const hasBasicFeedback = hasAiFeedback(qa)
  const hasThinking = hasAiThinking(qa)
  const hasDetailedData = getAnswerData(qa.answerData) !== null

  if (hasBasicFeedback && hasThinking && hasDetailedData) {
    return '完整评估'
  } else if (hasBasicFeedback || hasDetailedData) {
    return '部分评估'
  } else {
    return '无评估数据'
  }
}

// 刷新评估
const refreshEvaluation = async (qa: any) => {
  if (!qa.answer || !session.value) {
    ElMessage.warning('无法重新评估：缺少回答内容或会话信息')
    return
  }

  try {
    refreshingEvaluation.value[qa.id] = true

    // 准备请求数据
    const requestData = {
      qaId: qa.id,
      question: qa.question,
      answer: qa.answer,
      jobPosition: session.value.jobPosition || '通用岗位'
    }

    // 调试信息
    console.log('刷新评估请求数据:', requestData)
    console.log('qa对象:', qa)

    // 调用后端重新评估接口
    const response = await interviewApi.refreshEvaluation(requestData)

    // 更新当前问答的评估数据
    const updatedQa = response.data
    const index = qaList.value.findIndex(item => item.id === qa.id)
    if (index !== -1) {
      qaList.value[index] = { ...qaList.value[index], ...updatedQa }
    }

    ElMessage.success('AI评估已更新')

  } catch (error) {
    console.error('刷新评估失败:', error)
    ElMessage.error('刷新评估失败，请稍后重试')
  } finally {
    refreshingEvaluation.value[qa.id] = false
  }
}

// 获取最终综合分数
const getFinalScore = () => {
  if (detailedReport.value?.overallScores?.finalScore) {
    return detailedReport.value.overallScores.finalScore
  }
  // 如果没有详细报告，使用原始分数
  return session.value?.score || 0
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 格式化时长
const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  }
  return `${secs}秒`
}

// 格式化反馈内容
const formatFeedback = (feedback: string) => {
  // 简单的换行处理
  return feedback.replace(/\n/g, '<br>')
}

// 格式化总结内容
const formatSummary = (summary: string) => {
  // 简单的换行处理
  return summary.replace(/\n/g, '<br>')
}

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChartRef.value) return

  const chart = echarts.init(radarChartRef.value)

  // 使用详细报告的雷达图数据，如果没有则计算
  const abilities = detailedReport.value?.radarData ?
    getRadarDataFromReport() :
    calculateAbilities()

  const option = {
    title: {
      text: '能力分析雷达图',
      left: 'center',
      textStyle: {
        fontSize: 16,
        color: '#333'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        let result = `${data.name}<br/>`
        data.value.forEach((val: number, index: number) => {
          const indicator = option.radar.indicator[index]
          result += `${indicator.name}: ${(val * 100).toFixed(1)}%<br/>`
        })
        return result
      }
    },
    radar: {
      indicator: [
        { name: '技术准确性', max: 100 },
        { name: '完整性', max: 100 },
        { name: '表达清晰度', max: 100 },
        { name: '逻辑性', max: 100 },
        { name: '实践经验', max: 100 },
        { name: '自信度', max: 100 }
      ],
      radius: '70%',
      axisLabel: {
        formatter: (value: number) => `${value.toFixed(0)}`
      }
    },
    series: [{
      name: '能力评估',
      type: 'radar',
      data: [{
        value: [
          abilities.technical_accuracy,
          abilities.completeness,
          abilities.clarity,
          abilities.logic,
          abilities.practical_experience,
          abilities.confidence_score
        ],
        name: '综合能力',
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.2)'
        },
        lineStyle: {
          color: '#409eff',
          width: 2
        },
        itemStyle: {
          color: '#409eff'
        }
      }]
    }]
  }

  chart.setOption(option)

  // 响应式调整
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

// 从详细报告获取雷达图数据
const getRadarDataFromReport = (): Record<string, number> => {
  if (!detailedReport.value?.radarData?.dimensions) {
    return calculateAbilities()
  }

  const dimensions = detailedReport.value.radarData.dimensions
  const result: Record<string, number> = {
    technical_accuracy: 60,
    completeness: 60,
    clarity: 60,
    logic: 60,
    practical_experience: 60,
    confidence_score: 60
  }

  // 映射维度名称到结果对象
  const nameMapping: { [key: string]: string } = {
    '技术准确性': 'technical_accuracy',
    '回答完整性': 'completeness',
    '表达清晰度': 'clarity',
    '逻辑性': 'logic',
    '实践经验': 'practical_experience',
    '自信度': 'confidence_score'
  }

  dimensions.forEach((dim: any) => {
    const key = nameMapping[dim.name]
    if (key) {
      result[key] = dim.value || 60
    }
  })

  return result
}

// 计算各维度能力分数
const calculateAbilities = (): Record<string, number> => {
  if (!qaList.value.length) {
    return {
      technical_accuracy: 60,
      completeness: 60,
      clarity: 60,
      logic: 60,
      practical_experience: 60,
      confidence_score: 60
    }
  }

  const validQas = qaList.value.filter(qa => qa.answer)
  if (!validQas.length) {
    return {
      technical_accuracy: 60,
      completeness: 60,
      clarity: 60,
      logic: 60,
      practical_experience: 60,
      confidence_score: 60
    }
  }

  let totalTechnical = 0, totalCompleteness = 0, totalClarity = 0
  let totalLogic = 0, totalPractical = 0, totalConfidence = 0
  let count = 0

  // 如果有详细报告数据，直接使用
  if (detailedReport.value?.radarData) {
    return getRadarDataFromReport()
  }

  validQas.forEach(qa => {
    try {
      const answerData = typeof qa.answerData === 'string' ? JSON.parse(qa.answerData) : qa.answerData
      if (answerData) {
        totalTechnical += answerData.technical_accuracy || 0.6
        totalCompleteness += answerData.completeness || 0.6
        totalClarity += answerData.clarity || 0.6
        totalLogic += answerData.logic || 0.6
        totalPractical += answerData.practical_experience || 0.6
        totalConfidence += answerData.confidence_score || 0.6
        count++
      }
    } catch (e) {
      console.warn('解析答案数据失败:', e)
    }
  })

  if (count === 0) {
    return {
      technical_accuracy: 0.6,
      completeness: 0.6,
      clarity: 0.6,
      logic: 0.6,
      practical_experience: 0.6,
      confidence_score: 0.6
    }
  }

  return {
    technical_accuracy: Math.min(totalTechnical / count, 1),
    completeness: Math.min(totalCompleteness / count, 1),
    clarity: Math.min(totalClarity / count, 1),
    logic: Math.min(totalLogic / count, 1),
    practical_experience: Math.min(totalPractical / count, 1),
    confidence_score: Math.min(totalConfidence / count, 1)
  }
}
</script>

<style lang="scss" scoped>
.interview-report {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.loading, .error-state {
  text-align: center;
  padding: 60px 0;
}

.debug-info {
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #0ea5e9;
  border-radius: 8px;
  margin-bottom: 16px;

  p {
    margin: 4px 0;
    color: #0369a1;
    font-size: 14px;
  }
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  
  .header-info {
    h1 {
      margin: 0 0 8px 0;
      color: #333;
      font-size: 28px;
    }
    
    .session-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #666;
    }
  }
  
  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.score-overview {
  margin-bottom: 32px;
  
  .score-display {
    display: flex;
    align-items: center;
    gap: 48px;
    
    .main-score {
      text-align: center;
      
      .score-text {
        font-size: 24px;
        font-weight: 600;
        color: #333;
      }
      
      .score-label {
        margin-top: 12px;
        font-size: 16px;
        color: #666;
      }
    }
    
    .score-details {
      flex: 1;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 24px;
      
      .detail-item {
        text-align: center;
        
        .label {
          display: block;
          font-size: 14px;
          color: #666;
          margin-bottom: 4px;
        }
        
        .value {
          font-size: 18px;
          font-weight: 600;
          color: #333;
        }
      }
    }
  }
}

.ability-radar, .qa-details, .ai-summary {
  margin-bottom: 32px;
  
  h3 {
    margin: 0;
    color: #333;
  }
}

.chart-placeholder {
  text-align: center;
  padding: 60px 0;
  color: #666;
  
  p:first-child {
    font-size: 18px;
    margin-bottom: 8px;
  }
}

.qa-list {
  .qa-item {
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 16px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .qa-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      .question-info {
        display: flex;
        align-items: center;
        gap: 12px;
        
        .question-number {
          background: #409eff;
          color: white;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
        }
      }
      
      .answer-meta {
        display: flex;
        gap: 12px;
        font-size: 12px;
        color: #666;
      }
    }
    
    .question-content, .answer-content, .ai-feedback, .ai-thinking, .detailed-evaluation, .keywords {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }

      h4 {
        margin: 0 0 8px 0;
        font-size: 14px;
        color: #333;
        font-weight: 600;
      }

      h5 {
        margin: 0 0 6px 0;
        font-size: 13px;
        color: #409eff;
        font-weight: 500;
      }

      p {
        margin: 0;
        line-height: 1.6;
        color: #666;
      }
    }
    

    
    .feedback-content {
      line-height: 1.6;
      color: #666;
    }
    
    .keyword-tags {
      .el-tag {
        margin-right: 8px;
        margin-bottom: 4px;
      }
    }

    // AI思考过程样式
    .ai-thinking {
      .thinking-content {
        .thinking-text {
          background: #f0f9ff;
          padding: 16px;
          border-radius: 8px;
          border: 1px solid #e1f5fe;
          line-height: 1.8;
          color: #37474f;
          font-size: 13px;
          white-space: pre-wrap;
          word-wrap: break-word;
          max-height: 300px;
          overflow-y: auto;
        }
      }
    }

    // 详细评估样式
    .detailed-evaluation {
      .evaluation-content {
        background: #fafafa;
        padding: 16px;
        border-radius: 8px;
        border: 1px solid #e0e0e0;

        .ai-detailed-feedback {
          margin-bottom: 16px;

          .feedback-text {
            background: #fff3e0;
            padding: 12px;
            border-radius: 6px;
            border-left: 3px solid #ff9800;
            line-height: 1.6;
            color: #5d4037;
            font-size: 13px;
          }
        }

        .strengths, .weaknesses, .suggestions {
          margin-bottom: 12px;

          ul {
            margin: 4px 0 0 0;
            padding-left: 20px;

            li {
              margin-bottom: 4px;
              line-height: 1.5;
              color: #666;
              font-size: 13px;
            }
          }
        }

        .strengths {
          h5 { color: #4caf50; }
        }

        .weaknesses {
          h5 { color: #f44336; }
        }

        .suggestions {
          h5 { color: #2196f3; }
        }

        .detailed-scores-breakdown {
          .score-breakdown {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 8px;
            margin-top: 8px;

            .breakdown-item {
              display: flex;
              justify-content: space-between;
              align-items: center;
              padding: 8px 12px;
              background: white;
              border-radius: 4px;
              border: 1px solid #e0e0e0;

              .breakdown-label {
                font-size: 12px;
                color: #666;
              }

              .breakdown-value {
                font-size: 12px;
                font-weight: 600;
                color: #409eff;
              }
            }
          }
        }
      }
    }
  }
}

.summary-section, .feedback-section {
  margin-bottom: 24px;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  h4 {
    margin: 0 0 12px 0;
    color: #333;
  }
  
  .summary-content, .feedback-content {
    line-height: 1.8;
    color: #666;
  }
}



@media (max-width: 768px) {
  .interview-report {
    padding: 16px;
  }

  .report-header {
    flex-direction: column;
    gap: 16px;
  }

  .score-display {
    flex-direction: column;
    gap: 24px;
    text-align: center;
  }

  .score-details {
    grid-template-columns: repeat(2, 1fr);
  }

  .qa-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

.ai-feedback-section {
  .feedback-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    h4 {
      margin: 0;
    }
  }

  .no-feedback {
    .no-feedback-content {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      background: #fff7e6;
      border: 1px solid #ffd591;
      border-radius: 6px;
      color: #d46b08;

      .el-icon {
        font-size: 16px;
      }

      span {
        flex: 1;
        font-size: 13px;
      }
    }
  }
}
</style>
