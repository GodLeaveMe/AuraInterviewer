<template>
  <div class="template-browse">
    <div class="page-header">
      <h1>面试模板</h1>
      <p>选择合适的面试模板开始练习</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-form :model="filterForm" inline>
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="选择分类" clearable>
            <el-option
              v-for="category in INTERVIEW_CATEGORIES"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="filterForm.difficulty" placeholder="选择难度" clearable>
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTemplates">筛选</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 热门模板 -->
    <div v-if="!filterForm.category && !filterForm.difficulty" class="popular-section">
      <h2>热门模板</h2>
      <div class="template-grid">
        <div
          v-for="template in popularTemplates"
          :key="template.id"
          class="template-card popular"
        >
          <div class="card-header">
            <h3>{{ template.name }}</h3>
            <el-tag type="warning" size="small">热门</el-tag>
          </div>
          <div class="card-content">
            <p class="description">{{ template.description }}</p>
            <div class="template-info">
              <span class="category">{{ template.category }}</span>
              <span class="difficulty">{{ getDifficultyText(template.difficulty) }}</span>
              <span class="duration">{{ template.duration }}分钟</span>
              <span class="questions">{{ template.questionCount }}题</span>
            </div>
            <div class="usage-count">已使用 {{ template.usageCount }} 次</div>
          </div>
          <div class="card-actions">
            <el-button @click="viewTemplate(template)">查看详情</el-button>
            <el-button type="primary" @click="startInterview(template)">开始面试</el-button>
            <el-button @click="copyTemplate(template)">复制模板</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 所有模板 -->
    <div class="all-templates-section">
      <h2>{{ filterForm.category || filterForm.difficulty ? '筛选结果' : '所有模板' }}</h2>
      <div v-loading="loading" class="template-grid">
        <div
          v-for="template in templates"
          :key="template.id"
          class="template-card"
        >
          <div class="card-header">
            <h3>{{ template.name }}</h3>
            <el-tag :type="getCategoryTagType(template.category)">
              {{ template.category }}
            </el-tag>
          </div>
          <div class="card-content">
            <p class="description">{{ template.description }}</p>
            <div class="template-info">
              <span class="difficulty">{{ getDifficultyText(template.difficulty) }}</span>
              <span class="duration">{{ template.duration }}分钟</span>
              <span class="questions">{{ template.questionCount }}题</span>
            </div>
            <div class="usage-count">已使用 {{ template.usageCount || 0 }} 次</div>
          </div>
          <div class="card-actions">
            <el-button @click="viewTemplate(template)">查看详情</el-button>
            <el-button type="primary" @click="startInterview(template)">开始面试</el-button>
            <el-button @click="copyTemplate(template)">复制模板</el-button>
          </div>
        </div>
      </div>
      
      <div v-if="!loading && templates.length === 0" class="empty-state">
        <el-empty description="暂无模板" />
      </div>
    </div>

    <!-- 模板详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="模板详情"
      width="800px"
    >
      <div v-if="selectedTemplate" class="template-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">
            {{ selectedTemplate.name }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag :type="getCategoryTagType(selectedTemplate.category)">
              {{ selectedTemplate.category }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyTagType(selectedTemplate.difficulty)">
              {{ getDifficultyText(selectedTemplate.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预计时长">
            {{ selectedTemplate.duration }}分钟
          </el-descriptions-item>
          <el-descriptions-item label="问题数量">
            {{ selectedTemplate.questionCount }}个
          </el-descriptions-item>
          <el-descriptions-item label="使用次数">
            {{ selectedTemplate.usageCount || 0 }}次
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="selectedTemplate.description" style="margin-top: 20px;">
          <h4>模板描述</h4>
          <p>{{ selectedTemplate.description }}</p>
        </div>
        
        <div v-if="selectedTemplate.tags" style="margin-top: 20px;">
          <h4>标签</h4>
          <el-tag
            v-for="tag in getTemplateTags(selectedTemplate.tags)"
            :key="tag"
            style="margin-right: 8px; margin-bottom: 8px;"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button @click="copyTemplate(selectedTemplate!)">复制模板</el-button>
          <el-button type="primary" @click="startInterview(selectedTemplate!)">
            开始面试
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { interviewApi } from '@/api/interview'
import type { InterviewTemplate } from '@/types/interview'
import { INTERVIEW_CATEGORIES, getCategoryTagType, getDifficultyTagType, getDifficultyText } from '@/constants/interview'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const templates = ref<InterviewTemplate[]>([])
const popularTemplates = ref<InterviewTemplate[]>([])
const detailDialogVisible = ref(false)
const selectedTemplate = ref<InterviewTemplate>()

// 筛选表单
const filterForm = reactive({
  category: '',
  difficulty: undefined as number | undefined
})

// 生命周期
onMounted(() => {
  loadPopularTemplates()
  loadTemplates()
})

// 方法
const loadPopularTemplates = async () => {
  try {
    const response = await interviewApi.getPopularInterviewTemplates(6)
    popularTemplates.value = response.data || []
  } catch (error) {
    console.error('加载热门模板失败:', error)
  }
}

const loadTemplates = async () => {
  try {
    loading.value = true
    
    let response
    if (filterForm.category) {
      response = await interviewApi.getInterviewTemplatesByCategory(filterForm.category)
    } else if (filterForm.difficulty) {
      response = await interviewApi.getInterviewTemplatesByDifficulty(filterForm.difficulty)
    } else {
      response = await interviewApi.getPublicInterviewTemplates()
    }
    
    templates.value = response.data || []
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
    templates.value = []
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.category = ''
  filterForm.difficulty = undefined
  loadTemplates()
}

const viewTemplate = (template: InterviewTemplate) => {
  selectedTemplate.value = template
  detailDialogVisible.value = true
}

const startInterview = (template: InterviewTemplate) => {
  // 跳转到面试页面
  router.push({
    name: 'InterviewSession',
    params: { templateId: template.id }
  })
}

const copyTemplate = async (template: InterviewTemplate) => {
  try {
    await interviewApi.copyInterviewTemplate(template.id!)
    ElMessage.success('模板复制成功，可在"我的模板"中查看')
  } catch (error) {
    console.error('复制模板失败:', error)
    ElMessage.error('复制模板失败')
  }
}

// 使用导入的工具方法

const getTemplateTags = (tags: string | undefined) => {
  if (!tags) return []
  try {
    return JSON.parse(tags)
  } catch {
    return []
  }
}
</script>

<style scoped>
.template-browse {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 16px;
}

.filter-bar {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.popular-section,
.all-templates-section {
  margin-bottom: 40px;
}

.popular-section h2,
.all-templates-section h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.template-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.template-card.popular {
  border: 2px solid #f56c6c;
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 10px;
}

.card-content {
  margin-bottom: 20px;
}

.description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.template-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.template-info span {
  background: #f5f7fa;
  color: #606266;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.usage-count {
  color: #909399;
  font-size: 12px;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-actions .el-button {
  flex: 1;
  min-width: 80px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.template-detail h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

.template-detail p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .template-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: column;
  }

  .card-actions .el-button {
    width: 100%;
  }
}
</style>
