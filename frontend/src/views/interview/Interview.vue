<template>
  <div class="interview-page">
    <div class="page-header">
      <h1>开始面试</h1>
      <p>选择面试模板，开始您的面试练习</p>
    </div>

    <!-- 筛选条件 -->
    <div class="filters">
      <el-card>
        <div class="filter-row">
          <div class="filter-item">
            <label>分类：</label>
            <el-select v-model="filters.category" placeholder="全部分类" clearable @change="loadTemplates">
              <el-option
                v-for="category in INTERVIEW_CATEGORIES"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
          </div>
          
          <div class="filter-item">
            <label>难度：</label>
            <el-select v-model="filters.difficulty" placeholder="全部难度" clearable @change="loadTemplates">
              <el-option label="初级" :value="1" />
              <el-option label="中级" :value="2" />
              <el-option label="高级" :value="3" />
            </el-select>
          </div>
          

        </div>
      </el-card>
    </div>

    <!-- 模板列表 -->
    <div class="templates-section">
      <el-row :gutter="20">
        <el-col 
          v-for="template in templates" 
          :key="template.id"
          :xs="24" 
          :sm="12" 
          :md="8" 
          :lg="6"
        >
          <el-card class="template-card" @click="selectTemplate(template)">
            <div class="template-header">
              <h3>{{ template.name }}</h3>
              <el-tag :type="getDifficultyType(template.difficulty)">
                {{ getDifficultyText(template.difficulty) }}
              </el-tag>
            </div>
            
            <p class="template-description">{{ template.description }}</p>
            
            <div class="template-info">
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>{{ template.duration }} 分钟</span>
              </div>
              <div class="info-item">
                <el-icon><Document /></el-icon>
                <span>{{ template.questionCount }} 题</span>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>{{ template.usageCount }} 次使用</span>
              </div>
            </div>
            
            <div class="template-tags">
              <el-tag 
                v-for="tag in getTemplateTags(template.tags)" 
                :key="tag"
                size="small"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>
            
            <div class="template-actions">
              <el-button type="primary" size="small" @click.stop="startInterview(template)">
                开始面试
              </el-button>
              <el-button size="small" @click.stop="previewTemplate(template)">
                预览
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <div v-if="loading" class="loading">
        <el-skeleton :rows="4" animated />
      </div>
      
      <div v-if="!loading && templates.length === 0" class="empty">
        <el-empty description="暂无面试模板" />
      </div>
    </div>

    <!-- 创建面试对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建面试会话"
      width="600px"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item label="面试标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入面试标题" />
        </el-form-item>
        
        <el-form-item label="职位信息" prop="jobPosition">
          <el-input v-model="createForm.jobPosition" placeholder="请输入目标职位" />
        </el-form-item>
        
        <el-form-item label="面试时长">
          <el-input
            v-model="createForm.duration"
            readonly
            suffix-icon="Clock"
          >
            <template #suffix>
              <span>分钟</span>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="问题数量">
          <el-input
            v-model="createForm.questionCount"
            readonly
            suffix-icon="Document"
          >
            <template #suffix>
              <span>题</span>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="面试模式" prop="interviewMode">
          <el-radio-group v-model="createForm.interviewMode" @change="onInterviewModeChange">
            <el-radio value="text">文本面试</el-radio>

          </el-radio-group>
          <div style="margin-top: 8px; color: #666; font-size: 12px;">
            <span v-if="createForm.interviewMode === 'text'">文本问答模式，使用聊天和推理模型，适合快速练习和学习</span>

          </div>
        </el-form-item>

        <el-form-item label="AI模型" prop="aiModel">
          <el-select
            v-model="createForm.aiModel"
            placeholder="选择AI模型"
            :loading="loadingModels"
            @focus="loadAvailableModels"
          >
            <el-option-group label="系统内置模型">
              <el-option
                v-for="model in filteredSystemModels"
                :key="model.id"
                :label="model.model || model.name"
                :value="model.model || model.name"
              >
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>{{ model.model || model.name }}</span>
                  <div>
                    <el-tag v-if="model.isDefault" type="success" size="small">推荐</el-tag>
                    <el-tag v-if="(model as any).modelType" :type="getModelTypeColor((model as any).modelType)" size="small" style="margin-left: 4px;">
                      {{ getModelTypeText((model as any).modelType) }}
                    </el-tag>
                  </div>
                </div>
                <div style="color: #999; font-size: 12px;">{{ model.description }}</div>
              </el-option>
            </el-option-group>
            <el-option-group label="用户自定义模型" v-if="filteredUserModels.length > 0">
              <el-option
                v-for="model in filteredUserModels"
                :key="'user-' + model.id"
                :label="model.model || model.name"
                :value="model.model || model.name"
              >
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>{{ model.model || model.name }}</span>
                  <div>
                    <el-tag type="info" size="small">自定义</el-tag>
                    <el-tag v-if="(model as any).modelType" :type="getModelTypeColor((model as any).modelType)" size="small" style="margin-left: 4px;">
                      {{ getModelTypeText((model as any).modelType) }}
                    </el-tag>
                  </div>
                </div>
                <div style="color: #999; font-size: 12px;">{{ model.description }}</div>
              </el-option>
            </el-option-group>
          </el-select>
          <div style="margin-top: 8px; color: #666; font-size: 12px;">
            <span v-if="createForm.interviewMode === 'text'">文本面试需要选择聊天或推理模型</span>

            <span v-else>选择合适的AI模型进行面试评估</span>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">
          创建并开始
        </el-button>
      </template>
    </el-dialog>

    <!-- 模板预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="模板预览"
      width="800px"
    >
      <div v-if="selectedTemplate" class="template-preview">
        <div class="preview-header">
          <h2>{{ selectedTemplate.name }}</h2>
          <el-tag :type="getDifficultyType(selectedTemplate.difficulty)">
            {{ getDifficultyText(selectedTemplate.difficulty) }}
          </el-tag>
        </div>
        
        <div class="preview-content">
          <p><strong>描述：</strong>{{ selectedTemplate.description }}</p>
          <p><strong>分类：</strong>{{ selectedTemplate.category }}</p>
          <p><strong>预计时长：</strong>{{ selectedTemplate.duration }} 分钟</p>
          <p><strong>问题数量：</strong>{{ selectedTemplate.questionCount }} 题</p>
          <p><strong>使用次数：</strong>{{ selectedTemplate.usageCount }} 次</p>
          
          <div class="preview-tags">
            <strong>标签：</strong>
            <el-tag 
              v-for="tag in getTemplateTags(selectedTemplate.tags)" 
              :key="tag"
              size="small"
              style="margin-left: 8px;"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="startInterviewFromPreview">
          开始面试
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { Clock, Document, User } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import { aiApi } from '@/api/ai'
import { apiConfigApi, type ApiConfig } from '@/api/apiConfig'
import type { InterviewTemplate } from '@/types/interview'
import type { AiModel } from '@/api/ai'
import { INTERVIEW_CATEGORIES } from '@/constants/interview'

// 扩展AiModel类型以包含modelType
interface ExtendedAiModel extends AiModel {
  modelType?: string
}

const router = useRouter()

const templates = ref<InterviewTemplate[]>([])
const loading = ref(false)
const createDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const selectedTemplate = ref<InterviewTemplate | null>(null)
const creating = ref(false)
const loadingModels = ref(false)
const systemModels = ref<AiModel[]>([])
const userModels = ref<AiModel[]>([])
const availableModels = ref<AiModel[]>([])

const createFormRef = ref<InstanceType<typeof ElForm>>()

// 筛选条件
const filters = reactive({
  category: '',
  difficulty: undefined as number | undefined
})

// 创建表单
const createForm = reactive({
  title: '',
  templateId: 0,
  jobPosition: '',
  duration: 30,
  questionCount: 10,
  interviewMode: 'text' as 'text',
  aiModel: ''
})

// 表单验证规则
const createRules = {
  title: [
    { required: true, message: '请输入面试标题', trigger: 'blur' }
  ],
  jobPosition: [
    { required: true, message: '请输入目标职位', trigger: 'blur' }
  ],
  interviewMode: [
    { required: true, message: '请选择面试模式', trigger: 'change' }
  ],
  aiModel: [
    { required: true, message: '请选择AI模型', trigger: 'change' }
  ]
}

onMounted(() => {
  loadTemplates()
  loadAvailableModels()

  // 检查URL参数
  const templateId = router.currentRoute.value.query.templateId
  if (templateId) {
    const template = templates.value.find(t => t.id === Number(templateId))
    if (template) {
      selectTemplate(template)
    }
  }
})

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    // 获取所有公开模板
    const response = await interviewApi.getPublicInterviewTemplates()
    let allTemplates = response.data || []

    // 客户端筛选
    let filteredTemplates = allTemplates

    // 按分类筛选
    if (filters.category) {
      filteredTemplates = filteredTemplates.filter(template =>
        template.category === filters.category
      )
    }

    // 按难度筛选
    if (filters.difficulty) {
      filteredTemplates = filteredTemplates.filter(template =>
        template.difficulty === filters.difficulty
      )
    }

    templates.value = filteredTemplates
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
    templates.value = []
  } finally {
    loading.value = false
  }
}

// 加载可用的AI模型
const loadAvailableModels = async () => {
  if (loadingModels.value) return

  loadingModels.value = true
  try {
    // 加载系统内置模型
    const systemResponse = await aiApi.getAvailableModels()
    systemModels.value = systemResponse.data || []

    // 加载用户自定义模型 - 从API配置获取
    try {
      const userResponse = await apiConfigApi.getEnabledConfigs()
      const userConfigs = userResponse.data || []

      // 转换用户配置为模型格式
      userModels.value = userConfigs.map((config: ApiConfig) => ({
        id: config.id || 0,
        name: config.name || '',
        provider: config.apiType || '', // API配置中使用apiType字段
        model: config.model || '',
        modelType: config.modelType, // 添加模型类型
        description: config.description,
        isActive: Boolean(config.enabled), // API配置中使用enabled字段
        isDefault: Boolean(config.isDefault)
      }))
    } catch (error) {
      // 用户可能没有自定义模型，这是正常的
      userModels.value = []
    }

    // 合并所有模型
    availableModels.value = [...systemModels.value, ...userModels.value]

    // 设置默认模型
    if (availableModels.value.length > 0 && !createForm.aiModel) {
      const defaultModel = systemModels.value.find(m => m.isDefault) || systemModels.value[0]
      if (defaultModel) {
        createForm.aiModel = defaultModel.model || defaultModel.name
      }
    }
  } catch (error) {
    console.error('加载AI模型失败:', error)
    ElMessage.error('加载AI模型失败')
    systemModels.value = []
    userModels.value = []
    availableModels.value = []
  } finally {
    loadingModels.value = false
  }
}

// 选择模板
const selectTemplate = (template: InterviewTemplate) => {
  selectedTemplate.value = template

  // 设置表单数据
  createForm.templateId = template.id || 0
  createForm.title = `${template.name} - 面试练习`
  createForm.duration = template.duration
  createForm.questionCount = template.questionCount

  createDialogVisible.value = true
}

// 开始面试
const startInterview = (template: InterviewTemplate) => {
  selectTemplate(template)
}

// 预览模板
const previewTemplate = (template: InterviewTemplate) => {
  selectedTemplate.value = template
  previewDialogVisible.value = true
}

// 从预览开始面试
const startInterviewFromPreview = () => {
  previewDialogVisible.value = false
  if (selectedTemplate.value) {
    selectTemplate(selectedTemplate.value)
  }
}

// 确认创建
const confirmCreate = async () => {
  if (!createFormRef.value) return

  try {
    const valid = await createFormRef.value.validate()
    if (!valid) return

    creating.value = true

    // 创建面试会话
    const sessionData = {
      title: createForm.title,
      templateId: createForm.templateId,
      jobPosition: createForm.jobPosition,
      duration: createForm.duration,
      questionCount: createForm.questionCount,
      interviewMode: createForm.interviewMode,
      aiModel: createForm.aiModel,
      settings: {
        enableThinking: false,
        enableRealTimeScore: true,
        enableAutoNext: false,
        interviewMode: createForm.interviewMode,
        aiModel: createForm.aiModel
      }
    }

    const response = await interviewApi.createInterviewSession(sessionData)

    if (response.code === 200) {
      createDialogVisible.value = false
      resetCreateForm()
      ElMessage.success('面试会话创建成功')

      // 跳转到聊天面试页面
      router.push(`/interview/chat/${response.data.id}`)
    } else {
      ElMessage.error(response.message || '创建失败')
    }
  } catch (error) {
    console.error('创建面试会话失败:', error)
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

// 处理取消
const handleCancel = () => {
  createDialogVisible.value = false
  resetCreateForm()
}

// 重置创建表单
const resetCreateForm = () => {
  createForm.title = ''
  createForm.templateId = 0
  createForm.jobPosition = ''
  createForm.duration = 30
  createForm.questionCount = 10
  createForm.interviewMode = 'text'
  createForm.aiModel = ''
  selectedTemplate.value = null

  // 重新设置默认AI模型
  if (availableModels.value.length > 0) {
    const defaultModel = systemModels.value.find(m => m.isDefault) || systemModels.value[0]
    if (defaultModel) {
      createForm.aiModel = defaultModel.model || defaultModel.name
    }
  }
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

// 获取模板标签
const getTemplateTags = (tags: string | string[] | undefined) => {
  if (!tags) return []
  if (typeof tags === 'string') {
    try {
      return JSON.parse(tags)
    } catch {
      return []
    }
  }
  return tags || []
}

// 面试模式改变时的处理
const onInterviewModeChange = () => {
  // 清空当前选择的AI模型，让用户重新选择合适的模型
  createForm.aiModel = ''
}

// 根据面试模式过滤系统模型
const filteredSystemModels = computed(() => {
  if (!createForm.interviewMode) return systemModels.value

  return systemModels.value.filter(model => {
    const modelType = (model as any).modelType
    if (!modelType) return true // 如果没有模型类型，显示所有

    if (createForm.interviewMode === 'text') {
      // 文本面试：显示聊天和推理模型
      return modelType === 'chat' || modelType === 'reasoning'
    }

    return true
  })
})

// 根据面试模式过滤用户模型
const filteredUserModels = computed(() => {
  if (!createForm.interviewMode) return userModels.value

  return userModels.value.filter(model => {
    const modelType = (model as any).modelType
    if (!modelType) return true // 如果没有模型类型，显示所有

    if (createForm.interviewMode === 'text') {
      // 文本面试：显示聊天和推理模型
      return modelType === 'chat' || modelType === 'reasoning'
    }

    return true
  })
})

// 获取模型类型颜色
const getModelTypeColor = (modelType: string) => {
  const colorMap: Record<string, string> = {
    'chat': 'primary',
    'reasoning': 'warning',

  }
  return colorMap[modelType] || 'default'
}

// 获取模型类型文本
const getModelTypeText = (modelType: string) => {
  const textMap: Record<string, string> = {
    'chat': '聊天',
    'reasoning': '推理',

  }
  return textMap[modelType] || modelType
}
</script>

<style lang="scss" scoped>
.interview-page {
  max-width: 1200px;
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

.filters {
  margin-bottom: 24px;
  
  .filter-row {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
  }
  
  .filter-item {
    display: flex;
    align-items: center;
    gap: 8px;
    
    label {
      font-weight: 500;
      color: #333;
    }
  }
}

.templates-section {
  .el-col {
    margin-bottom: 20px;
  }
}

.template-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }
  
  .template-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
    
    h3 {
      margin: 0;
      font-size: 16px;
      color: #333;
      flex: 1;
    }
  }
  
  .template-description {
    color: #666;
    font-size: 14px;
    line-height: 1.5;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  
  .template-info {
    margin-bottom: 12px;
    
    .info-item {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #666;
      font-size: 12px;
      margin-bottom: 4px;
      
      .el-icon {
        font-size: 14px;
      }
    }
  }
  
  .template-tags {
    margin-bottom: 16px;
    
    .el-tag {
      margin-right: 8px;
      margin-bottom: 4px;
    }
  }
  
  .template-actions {
    display: flex;
    gap: 8px;
  }
}

.loading, .empty {
  text-align: center;
  padding: 40px 0;
}

.template-preview {
  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #333;
    }
  }
  
  .preview-content {
    p {
      margin-bottom: 12px;
      line-height: 1.6;
    }
    
    .preview-tags {
      margin-top: 16px;
    }
  }
}

@media (max-width: 768px) {
  .interview-page {
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
}

.interview-settings {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .el-checkbox {
    margin: 0;

    .el-checkbox__label {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

:deep(.el-select-dropdown__item) {
  height: auto;
  padding: 8px 20px;

  .el-tag {
    margin-left: 8px;
  }
}
</style>
