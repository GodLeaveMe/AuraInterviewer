<template>
  <div class="my-templates">
    <div class="page-header">
      <h1>我的模板</h1>
      <p>管理您创建的面试模板</p>
      <el-button type="primary" @click="createTemplate">
        <el-icon><Plus /></el-icon>
        创建模板
      </el-button>
    </div>

    <!-- 筛选和搜索 -->
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
          
          <div class="filter-item">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索模板名称或描述"
              clearable
              @input="debounceSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 模板列表 -->
    <div class="template-list">
      <el-table 
        v-loading="loading"
        :data="templates" 
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="template-name">
              <h4>{{ row.name }}</h4>
              <p class="description">{{ row.description || '暂无描述' }}</p>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="questionCount" label="题目数量" width="100">
          <template #default="{ row }">
            <span>{{ row.questionCount }}题</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="duration" label="预计时长" width="100">
          <template #default="{ row }">
            <span>{{ row.duration }}分钟</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" sortable="custom">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="warning"
                size="small"
                @click="editTemplate(row)"
              >
                编辑
              </el-button>

              <el-button
                size="small"
                @click="copyTemplate(row)"
              >
                复制
              </el-button>

              <el-popconfirm
                title="确定删除这个模板吗？"
                @confirm="deleteTemplate(row.id)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    size="small"
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
          @size-change="loadTemplates"
          @current-change="loadTemplates"
        />
      </div>
    </div>

    <!-- 创建/编辑模板对话框 -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="isEditing ? '编辑模板' : '创建模板'"
      width="800px"
      @close="resetForm"
    >
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="templateRules"
        label-width="100px"
      >
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        
        <el-form-item label="模板描述" prop="description">
          <el-input 
            v-model="templateForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入模板描述"
          />
        </el-form-item>
        
        <el-form-item label="分类" prop="category">
          <el-select v-model="templateForm.category" placeholder="请选择分类">
            <el-option
              v-for="category in INTERVIEW_CATEGORIES"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="templateForm.difficulty" placeholder="请选择难度">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="题目数量" prop="questionCount">
          <el-input-number 
            v-model="templateForm.questionCount" 
            :min="1" 
            :max="50"
            placeholder="题目数量"
          />
        </el-form-item>
        
        <el-form-item label="预计时长" prop="duration">
          <el-input-number
            v-model="templateForm.duration"
            :min="5"
            :max="300"
            placeholder="分钟"
          />
          <span style="margin-left: 8px;">分钟</span>
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="templateForm.tags"
            multiple
            filterable
            allow-create
            placeholder="请选择标签"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="tag in commonTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="是否公开">
          <el-switch
            v-model="templateForm.isPublic"
            :active-value="1"
            :inactive-value="0"
            active-text="公开后所有用户都可以使用此模板"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate" :loading="saving">
          {{ isEditing ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import type { InterviewTemplate, InterviewTemplateRequest } from '@/types/interview'
import { INTERVIEW_CATEGORIES, getDifficultyTagType, getDifficultyText } from '@/constants/interview'
import { debounce } from 'lodash-es'
import dayjs from 'dayjs'



const templates = ref<InterviewTemplate[]>([])
const loading = ref(false)
const templateDialogVisible = ref(false)
const isEditing = ref(false)
const saving = ref(false)
const templateFormRef = ref()

// 筛选条件
const filters = reactive({
  category: '',
  difficulty: undefined as number | undefined,
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

// 模板表单
const templateForm = reactive<InterviewTemplateRequest>({
  name: '',
  description: '',
  category: '',
  difficulty: 1,
  questionCount: 10,
  duration: 60,
  tags: [],
  isPublic: 0  // 默认私有
})

// 常用标签
const commonTags = ref([
  'Java', 'Python', 'JavaScript', 'Vue', 'React', 'Spring Boot',
  '数据结构', '算法', '数据库', 'MySQL', 'Redis', 'Docker',
  '微服务', '分布式', '高并发', '性能优化', '系统设计',
  '团队协作', '项目管理', '沟通能力', '学习能力', '解决问题'
])

// 表单验证规则
const templateRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 50, message: '模板名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  difficulty: [
    { required: true, message: '请选择难度', trigger: 'change' }
  ],
  questionCount: [
    { required: true, message: '请输入题目数量', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入预计时长', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadTemplates()
})

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      category: filters.category || undefined,
      difficulty: filters.difficulty || undefined,
      keyword: filters.keyword || undefined,
      orderBy: sortConfig.prop || undefined,
      orderDirection: sortConfig.order === 'ascending' ? 'asc' : 'desc'
    }

    const result = await interviewApi.getMyTemplateList(params)
    templates.value = result.data.records || []
    pagination.total = result.data.total || 0
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
    // 使用默认数据
    templates.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 防抖搜索
const debounceSearch = debounce(() => {
  pagination.current = 1
  loadTemplates()
}, 500)

// 处理排序
const handleSortChange = ({ prop, order }: any) => {
  sortConfig.prop = prop
  sortConfig.order = order
  loadTemplates()
}

// 创建模板
const createTemplate = () => {
  isEditing.value = false
  resetForm()
  templateDialogVisible.value = true
}

// 编辑模板
const editTemplate = async (template: InterviewTemplate) => {
  isEditing.value = true
  try {
    const response = await interviewApi.getMyTemplateDetail(template.id!)
    const templateData = response.data

    // 处理tags数据，确保正确解析
    let parsedTags = []
    if (templateData.tags) {
      try {
        if (typeof templateData.tags === 'string') {
          const rawTags = JSON.parse(templateData.tags)
          // 清理可能损坏的tags数据
          parsedTags = rawTags.map((tag: any) => {
            if (typeof tag === 'string') {
              // 如果tag是字符串，检查是否是错误的JSON格式
              if (tag.startsWith('[') || tag.startsWith('"')) {
                try {
                  // 尝试解析嵌套的JSON
                  return JSON.parse(tag)
                } catch {
                  // 如果解析失败，清理引号和转义字符
                  return tag.replace(/^["\\]+|["\\]+$/g, '').trim()
                }
              }
              return tag.trim()
            }
            return String(tag).trim()
          }).flat().filter((tag: string) => tag && tag.length > 0)
        } else if (Array.isArray(templateData.tags)) {
          parsedTags = templateData.tags
        }
      } catch (error) {
        console.warn('解析tags失败，使用空数组:', error)
        parsedTags = []
      }
    }

    Object.assign(templateForm, {
      id: templateData.id,
      name: templateData.name,
      description: templateData.description,
      category: templateData.category,
      difficulty: templateData.difficulty,
      questionCount: templateData.questionCount,
      duration: templateData.duration,
      tags: parsedTags,
      isPublic: templateData.isPublic || 0
    })

    templateDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取模板详情失败')
  }
}



// 复制模板
const copyTemplate = async (template: InterviewTemplate) => {
  try {
    const response = await interviewApi.getMyTemplateDetail(template.id!)
    const templateData = response.data

    // 处理tags数据，确保正确解析
    let parsedTags = []
    if (templateData.tags) {
      try {
        if (typeof templateData.tags === 'string') {
          const rawTags = JSON.parse(templateData.tags)
          // 清理可能损坏的tags数据
          parsedTags = rawTags.map((tag: any) => {
            if (typeof tag === 'string') {
              // 如果tag是字符串，检查是否是错误的JSON格式
              if (tag.startsWith('[') || tag.startsWith('"')) {
                try {
                  // 尝试解析嵌套的JSON
                  return JSON.parse(tag)
                } catch {
                  // 如果解析失败，清理引号和转义字符
                  return tag.replace(/^["\\]+|["\\]+$/g, '').trim()
                }
              }
              return tag.trim()
            }
            return String(tag).trim()
          }).flat().filter((tag: string) => tag && tag.length > 0)
        } else if (Array.isArray(templateData.tags)) {
          parsedTags = templateData.tags
        }
      } catch (error) {
        console.warn('解析tags失败，使用空数组:', error)
        parsedTags = []
      }
    }

    Object.assign(templateForm, {
      name: templateData.name + ' (副本)',
      description: templateData.description,
      category: templateData.category,
      difficulty: templateData.difficulty,
      questionCount: templateData.questionCount,
      duration: templateData.duration,
      tags: parsedTags,
      isPublic: 0  // 复制的模板默认为私有
    })

    isEditing.value = false
    templateDialogVisible.value = true
  } catch (error) {
    ElMessage.error('复制模板失败')
  }
}

// 删除模板
const deleteTemplate = async (templateId: number) => {
  try {
    await interviewApi.deleteMyTemplate(templateId)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 保存模板
const saveTemplate = async () => {
  if (!templateFormRef.value) return

  try {
    await templateFormRef.value.validate()
    saving.value = true

    // 处理tags数据，确保是有效的JSON格式
    let tagsValue = '[]'
    if (templateForm.tags && Array.isArray(templateForm.tags) && templateForm.tags.length > 0) {
      tagsValue = JSON.stringify(templateForm.tags)
    }

    const submitData = {
      ...templateForm,
      difficulty: Number(templateForm.difficulty),
      tags: tagsValue
    }

    if (isEditing.value && templateForm.id) {
      await interviewApi.updateMyTemplate(templateForm.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await interviewApi.createMyTemplate(submitData)
      ElMessage.success('创建成功')
    }

    templateDialogVisible.value = false
    loadTemplates()
  } catch (error) {
    if (error !== false) { // 不是表单验证错误
      ElMessage.error(isEditing.value ? '更新失败' : '创建失败')
    }
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (templateFormRef.value) {
    templateFormRef.value.resetFields()
  }
  Object.assign(templateForm, {
    id: undefined,
    name: '',
    description: '',
    category: '',
    difficulty: 1,
    questionCount: 10,
    duration: 60,
    tags: [],
    isPublic: 0  // 默认私有
  })
}

// 获取难度类型 - 使用导入的方法
const getDifficultyType = getDifficultyTagType

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
.my-templates {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;

  div {
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
}

.filters {
  margin-bottom: 24px;

  .filter-row {
    display: flex;
    gap: 20px;
    align-items: center;
    flex-wrap: wrap;

    .filter-item {
      display: flex;
      align-items: center;
      gap: 12px;

      label {
        white-space: nowrap;
        color: #666;
        font-size: 14px;
      }

      .el-select, .el-input {
        width: 200px;
      }
    }
  }
}

.template-list {
  background: white;
  border-radius: 8px;
  overflow: hidden;

  .template-name {
    h4 {
      margin: 0 0 4px 0;
      color: #333;
      font-size: 16px;
    }

    .description {
      margin: 0;
      color: #666;
      font-size: 12px;
      line-height: 1.4;
    }
  }

  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .pagination {
    margin-top: 24px;
    text-align: center;
  }
}



@media (max-width: 768px) {
  .my-templates {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;

    div h1 {
      font-size: 24px;
    }
  }

  .filters {
    .filter-row {
      flex-direction: column;
      gap: 16px;
      align-items: stretch;

      .filter-item {
        .el-select, .el-input {
          width: 100%;
        }
      }
    }
  }
}
</style>
