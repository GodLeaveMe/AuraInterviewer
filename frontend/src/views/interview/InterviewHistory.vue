<template>
  <div class="interview-history">
    <div class="page-header">
      <h1>面试历史</h1>
      <p>查看您的所有面试记录和成绩</p>
    </div>

    <!-- 筛选和统计 -->
    <div class="filters-stats">
      <el-card>
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-number">{{ totalCount }}</div>
            <div class="stat-label">总面试次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ completedCount }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ averageScore.toFixed(1) }}</div>
            <div class="stat-label">平均分数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ formatDuration(totalDuration) }}</div>
            <div class="stat-label">总练习时长</div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="filter-row">
          <div class="filter-item">
            <label>状态：</label>
            <el-select v-model="filters.status" placeholder="全部状态" clearable @change="loadHistory">
              <el-option label="未开始" :value="0" />
              <el-option label="进行中" :value="1" />
              <el-option label="已完成" :value="2" />
              <el-option label="已取消" :value="3" />
              <el-option label="已暂停" :value="4" />
            </el-select>
          </div>
          
          <div class="filter-item">
            <label>时间范围：</label>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="loadHistory"
            />
          </div>
          
          <div class="filter-item">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索面试标题或职位"
              clearable
              @input="debounceSearch"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="filter-item">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetFilters">
              重置
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 面试列表 -->
    <div class="interview-list">
      <el-table 
        v-loading="loading"
        :data="interviews" 
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="title" label="面试标题" min-width="200">
          <template #default="{ row }">
            <div class="interview-title">
              <h4>{{ row.title }}</h4>
              <div class="interview-meta">
                <el-tag size="small" type="info">{{ getTemplateCategory(row.templateId) }}</el-tag>
                <span class="create-time">{{ formatTime(row.createTime) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="{ row }">
            <div class="progress-cell">
              <span>{{ row.answeredCount }}/{{ row.questionCount }}</span>
              <el-progress 
                :percentage="(row.answeredCount / row.questionCount) * 100" 
                :stroke-width="4"
                :show-text="false"
              />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="score" label="得分" width="100" sortable="custom">
          <template #default="{ row }">
            <div v-if="row.score" class="score-cell">
              <span :class="getScoreClass(row.score)">{{ row.score }}分</span>
            </div>
            <span v-else class="no-score">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="duration" label="时长" width="100">
          <template #default="{ row }">
            <span v-if="row.duration">{{ formatDuration(row.duration) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="startTime" label="开始时间" width="160" sortable="custom">
          <template #default="{ row }">
            <span v-if="row.startTime">{{ formatTime(row.startTime) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <!-- 进行中或暂停状态可以继续面试 -->
              <el-button
                v-if="row.status === 1 || row.status === 4"
                type="success"
                size="small"
                @click="continueInterview(row.id)"
              >
                {{ row.status === 4 ? '继续面试' : '进入面试' }}
              </el-button>

              <!-- 已完成可以查看报告 -->
              <el-button
                v-if="row.status === 2"
                type="primary"
                size="small"
                @click="viewReport(row.id)"
              >
                查看报告
              </el-button>

              <!-- 编辑面试信息（仅未开始状态） -->
              <el-button
                v-if="row.status === 0"
                type="warning"
                size="small"
                @click="editInterview(row)"
              >
                编辑
              </el-button>

              <!-- 查看详情 -->
              <el-button
                size="small"
                @click="viewDetails(row)"
              >
                详情
              </el-button>

              <!-- 删除（进行中状态不能删除） -->
              <el-popconfirm
                title="确定删除这条面试记录吗？"
                @confirm="deleteInterview(row.id)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    size="small"
                    :disabled="row.status === 1"
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadHistory"
          @current-change="loadHistory"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="面试详情"
      width="800px"
    >
      <div v-if="selectedInterview" class="interview-detail">
        <div class="detail-header">
          <h2>{{ selectedInterview.title }}</h2>
          <el-tag :type="getStatusType(selectedInterview.status)">
            {{ getStatusText(selectedInterview.status) }}
          </el-tag>
        </div>
        
        <div class="detail-content">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="面试状态">
              <el-tag :type="getStatusType(selectedInterview.status)">
                {{ getStatusText(selectedInterview.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatTime(selectedInterview.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ selectedInterview.startTime ? formatTime(selectedInterview.startTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ selectedInterview.endTime ? formatTime(selectedInterview.endTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="面试时长">
              {{ selectedInterview.duration ? formatDuration(selectedInterview.duration) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="总分">
              {{ selectedInterview.score ? selectedInterview.score + '分' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="问题进度">
              {{ selectedInterview.answeredCount }}/{{ selectedInterview.questionCount }}
            </el-descriptions-item>
            <el-descriptions-item label="AI模型">
              {{ getAiModelName(selectedInterview.aiModel || '') }}
            </el-descriptions-item>
          </el-descriptions>
          
          <div v-if="selectedInterview.summary" class="summary-section">
            <h3>面试总结</h3>
            <p>{{ selectedInterview.summary }}</p>
          </div>
          
          <div v-if="selectedInterview.feedback" class="feedback-section">
            <h3>改进建议</h3>
            <p>{{ selectedInterview.feedback }}</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="selectedInterview?.status === 2" 
          type="primary" 
          @click="viewReportFromDetail"
        >
          查看详细报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

import { interviewApi } from '@/api/interview'
import { aiApi } from '@/api/ai'
import type { InterviewSession } from '@/types/interview'
import type { AiModel } from '@/api/ai'
import { debounce } from 'lodash-es'
import dayjs from 'dayjs'

const router = useRouter()

const interviews = ref<InterviewSession[]>([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedInterview = ref<InterviewSession | null>(null)
const aiModels = ref<AiModel[]>([])

// 筛选条件
const filters = reactive({
  status: undefined as number | undefined,
  dateRange: [] as string[],
  keyword: ''
})

// 分页
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 排序
const sortConfig = reactive({
  prop: '',
  order: ''
})

// 统计数据
const totalCount = computed(() => interviews.value.length)
const completedCount = computed(() => interviews.value.filter(item => item.status === 2).length)
const averageScore = computed(() => {
  const completedInterviews = interviews.value.filter(item => item.status === 2 && item.score)
  if (completedInterviews.length === 0) return 0
  const totalScore = completedInterviews.reduce((sum, item) => sum + (item.score || 0), 0)
  return totalScore / completedInterviews.length
})
const totalDuration = computed(() => {
  return interviews.value.reduce((sum, item) => sum + (item.duration || 0), 0)
})

onMounted(() => {
  loadHistory()
  loadAiModels()
})

// 加载面试历史
const loadHistory = async () => {
  loading.value = true
  try {
    const result = await interviewApi.getInterviewHistory(pagination.current, pagination.size)
    let data = result.data || []

    // 应用搜索过滤
    if (filters.keyword.trim()) {
      data = data.filter((item: any) =>
        item.title?.toLowerCase().includes(filters.keyword.toLowerCase()) ||
        item.jobPosition?.toLowerCase().includes(filters.keyword.toLowerCase())
      )
    }

    // 应用状态过滤
    if (filters.status !== undefined) {
      data = data.filter((item: any) => item.status === filters.status)
    }

    interviews.value = data
    pagination.total = data.length
  } catch (error) {
    console.error('加载面试历史失败:', error)
    ElMessage.error('加载面试历史失败')
    interviews.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 加载AI模型列表
const loadAiModels = async () => {
  try {
    const response = await aiApi.getAvailableModels()
    aiModels.value = response.data || []
  } catch (error) {
    console.warn('加载AI模型列表失败:', error)
    aiModels.value = []
  }
}

// 防抖搜索
const debounceSearch = debounce(() => {
  pagination.current = 1
  loadHistory()
}, 500)

// 搜索处理
const handleSearch = () => {
  pagination.current = 1
  loadHistory()
}

// 重置筛选
const resetFilters = () => {
  filters.status = undefined
  filters.dateRange = []
  filters.keyword = ''
  pagination.current = 1
  loadHistory()
}



// 处理排序
const handleSortChange = ({ prop, order }: any) => {
  sortConfig.prop = prop
  sortConfig.order = order
  loadHistory()
}

// 继续面试
const continueInterview = (sessionId: number | string) => {
  router.push(`/interview/chat/${sessionId}`)
}

// 查看报告
const viewReport = (sessionId: number) => {
  router.push(`/interview/report/${sessionId}`)
}

// 查看详情
const viewDetails = (interview: InterviewSession) => {
  selectedInterview.value = interview
  detailDialogVisible.value = true
}

// 从详情查看报告
const viewReportFromDetail = () => {
  if (selectedInterview.value) {
    detailDialogVisible.value = false
    viewReport(Number(selectedInterview.value.id))
  }
}

// 编辑面试
const editInterview = (interview: InterviewSession) => {
  // 跳转到开始面试页面，并传递模板ID
  router.push({
    path: '/interview',
    query: {
      templateId: interview.templateId.toString(),
      edit: 'true',
      sessionId: interview.id.toString()
    }
  })
}

// 删除面试记录
const deleteInterview = async (sessionId: number | string) => {
  try {
    await interviewApi.deleteInterviewSession(sessionId)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error) {
    ElMessage.error('删除失败')
  }
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

// 获取分数样式类
const getScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 70) return 'score-fair'
  return 'score-poor'
}

// 获取模板分类
const getTemplateCategory = (_templateId: number) => {
  // TODO: 根据模板ID获取分类
  return '技术面试'
}

// 获取AI模型名称
const getAiModelName = (aiModelId: string | number) => {
  if (!aiModelId) return '-'

  // 查找系统模型
  const model = aiModels.value.find(m => m.id === Number(aiModelId))
  if (model) {
    return model.name
  }

  // 如果是用户自定义模型（以'user-'开头）
  if (String(aiModelId).startsWith('user-')) {
    const userModelId = String(aiModelId).replace('user-', '')
    const userModel = aiModels.value.find(m => m.id === Number(userModelId))
    if (userModel) {
      return `${userModel.name} (自定义)`
    }
  }

  // 如果找不到，返回ID
  return `模型 ${aiModelId}`
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 格式化时长
const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}
</script>

<style lang="scss" scoped>
.interview-history {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
  
  h1 {
    margin: 0 0 8px 0;
    font-size: 32px;
    color: #333;
  }
  
  p {
    margin: 0;
    color: #666;
    font-size: 16px;
  }
}

.filters-stats {
  margin-bottom: 24px;
  
  .stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 24px;
    margin-bottom: 16px;
    
    .stat-item {
      text-align: center;
      
      .stat-number {
        font-size: 24px;
        font-weight: 600;
        color: #409eff;
        margin-bottom: 4px;
      }
      
      .stat-label {
        font-size: 14px;
        color: #666;
      }
    }
  }
  
  .filter-row {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
    
    .filter-item {
      display: flex;
      align-items: center;
      gap: 8px;
      
      label {
        font-weight: 500;
        color: #333;
        white-space: nowrap;
      }
    }
  }
}

.interview-list {
  .interview-title {
    h4 {
      margin: 0 0 4px 0;
      font-size: 14px;
      color: #333;
    }
    
    .interview-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .create-time {
        font-size: 12px;
        color: #999;
      }
    }
  }
  
  .progress-cell {
    span {
      display: block;
      font-size: 12px;
      margin-bottom: 4px;
    }
  }
  
  .score-cell {
    .score-excellent { color: #67c23a; }
    .score-good { color: #409eff; }
    .score-fair { color: #e6a23c; }
    .score-poor { color: #f56c6c; }
  }
  
  .no-score {
    color: #999;
  }
  
  .action-buttons {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }
}

.pagination {
  margin-top: 24px;
  text-align: center;
}

.interview-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 {
      margin: 0;
      color: #333;
    }
  }
  
  .summary-section, .feedback-section {
    margin-top: 24px;
    
    h3 {
      margin: 0 0 12px 0;
      color: #333;
      font-size: 16px;
    }
    
    p {
      margin: 0;
      line-height: 1.6;
      color: #666;
    }
  }
}

@media (max-width: 768px) {
  .interview-history {
    padding: 16px;
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .filter-item {
    justify-content: space-between;
  }
  
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
