<template>
  <div class="template-manage">
    <div class="page-header">
      <h1>面试模板管理</h1>
      <p>管理系统面试模板，创建不同类型的面试场景</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="search-box">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索模板名称或描述"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      
      <div class="filter-box">
        <el-select
          v-model="searchForm.category"
          placeholder="选择分类"
          clearable
          style="width: 150px"
        >
          <el-option
            v-for="category in categories"
            :key="category"
            :label="category"
            :value="category"
          />
        </el-select>
        
        <el-select
          v-model="searchForm.difficulty"
          placeholder="选择难度"
          clearable
          style="width: 120px"
        >
          <el-option label="初级" :value="1" />
          <el-option label="中级" :value="2" />
          <el-option label="高级" :value="3" />
        </el-select>
        
        <el-select
          v-model="searchForm.isPublic"
          placeholder="选择状态"
          clearable
          style="width: 120px"
        >
          <el-option label="公开" :value="1" />
          <el-option label="私有" :value="0" />
        </el-select>
        
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
      
      <div class="action-buttons">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
      </div>
    </div>

    <!-- 模板列表 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="templateList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="template-name">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.usageCount > 100" type="success" size="small">热门</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTagType(row.difficulty)">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="duration" label="时长" width="80">
          <template #default="{ row }">
            {{ row.duration }}分钟
          </template>
        </el-table-column>
        
        <el-table-column prop="questionCount" label="问题数" width="80" />
        
        <el-table-column prop="usageCount" label="使用次数" width="100" />
        
        <el-table-column prop="isPublic" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isPublic"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">查看</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 模板表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类">
                <el-option
                  v-for="category in categories"
                  :key="category"
                  :label="category"
                  :value="category"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="模板描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="选择难度">
                <el-option label="初级" :value="1" />
                <el-option label="中级" :value="2" />
                <el-option label="高级" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计时长" prop="duration">
              <el-input-number
                v-model="form.duration"
                :min="5"
                :max="180"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #909399;">分钟</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="问题数量" prop="questionCount">
              <el-input-number
                v-model="form.questionCount"
                :min="1"
                :max="50"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            placeholder="请选择或输入标签"
            style="width: 100%"
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
            v-model="form.isPublic"
            :active-value="1"
            :inactive-value="0"
          />
          <span style="margin-left: 8px; color: #909399;">
            公开后所有用户都可以使用此模板
          </span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="submitting"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 模板详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="模板详情"
      width="800px"
    >
      <div v-if="viewTemplate" class="template-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">
            {{ viewTemplate.name }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag :type="getCategoryTagType(viewTemplate.category)">
              {{ viewTemplate.category }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyTagType(viewTemplate.difficulty)">
              {{ getDifficultyText(viewTemplate.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预计时长">
            {{ viewTemplate.duration }}分钟
          </el-descriptions-item>
          <el-descriptions-item label="问题数量">
            {{ viewTemplate.questionCount }}个
          </el-descriptions-item>
          <el-descriptions-item label="使用次数">
            {{ viewTemplate.usageCount }}次
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="viewTemplate.isPublic ? 'success' : 'info'">
              {{ viewTemplate.isPublic ? '公开' : '私有' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(viewTemplate.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="viewTemplate.description" style="margin-top: 20px;">
          <h4>模板描述</h4>
          <p>{{ viewTemplate.description }}</p>
        </div>
        
        <div v-if="viewTemplate.tags" style="margin-top: 20px;">
          <h4>标签</h4>
          <el-tag
            v-for="tag in parseTemplateTags(viewTemplate.tags)"
            :key="tag"
            style="margin-right: 8px; margin-bottom: 8px;"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { interviewApi, type InterviewTemplateRequest } from '@/api/interview'
import type { InterviewTemplate } from '@/types/interview'
import { formatTime } from '@/utils/date'

// 响应式数据
const loading = ref(false)
const templateList = ref<InterviewTemplate[]>([])
const submitting = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  category: '',
  difficulty: undefined as number | undefined,
  isPublic: undefined as number | undefined
})

// 分页
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const editId = ref<number>()

const viewDialogVisible = ref(false)
const viewTemplate = ref<InterviewTemplate>()

// 表单
const formRef = ref()
const form = reactive<InterviewTemplateRequest>({
  name: '',
  description: '',
  category: '',
  difficulty: 1,
  duration: 30,
  questionCount: 5,
  tags: '[]',
  isPublic: 1
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  difficulty: [
    { required: true, message: '请选择难度', trigger: 'change' }
  ],
  duration: [
    { required: true, message: '请输入预计时长', trigger: 'blur' }
  ],
  questionCount: [
    { required: true, message: '请输入问题数量', trigger: 'blur' }
  ]
}

// 分类和标签数据
const categories = ref([
  '技术面试',
  '行为面试',
  '算法面试',
  '系统设计',
  '项目经验',
  '综合面试',
  '英语面试',
  '管理面试'
])

const commonTags = ref([
  'Java', 'Python', 'JavaScript', 'React', 'Vue',
  'Spring', 'MySQL', 'Redis', 'Docker', 'Kubernetes',
  '算法', '数据结构', '系统设计', '项目管理', '团队协作'
])

// 生命周期
onMounted(() => {
  loadTemplateList()
})

// 方法
const loadTemplateList = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      category: searchForm.category || undefined,
      difficulty: searchForm.difficulty,
      isPublic: searchForm.isPublic
    }

    const response = await interviewApi.getInterviewTemplateList(params)
    templateList.value = response.data.records
    pagination.total = response.data.total
  } catch (error) {
    ElMessage.error('加载模板列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadTemplateList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.difficulty = undefined
  searchForm.isPublic = undefined
  pagination.current = 1
  loadTemplateList()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  loadTemplateList()
}

const handleCurrentChange = (current: number) => {
  pagination.current = current
  loadTemplateList()
}

const handleCreate = () => {
  dialogTitle.value = '新增面试模板'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: InterviewTemplate) => {
  dialogTitle.value = '编辑面试模板'
  isEdit.value = true
  editId.value = row.id

  // 填充表单
  let parsedTags = []
  if (row.tags) {
    try {
      const rawTags = JSON.parse(row.tags)
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
    } catch (error) {
      console.warn('解析tags失败，使用空数组:', error)
      parsedTags = []
    }
  }

  Object.assign(form, {
    name: row.name,
    description: row.description,
    category: row.category,
    difficulty: row.difficulty,
    duration: row.duration,
    questionCount: row.questionCount,
    tags: parsedTags,
    isPublic: row.isPublic
  })

  dialogVisible.value = true
}

const handleView = (row: InterviewTemplate) => {
  viewTemplate.value = row
  viewDialogVisible.value = true
}

const handleDelete = async (row: InterviewTemplate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${row.name}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await interviewApi.deleteInterviewTemplate(row.id!)
    ElMessage.success('删除成功')
    loadTemplateList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

const handleStatusChange = async (row: InterviewTemplate) => {
  try {
    await interviewApi.updateInterviewTemplateStatus(row.id!, row.isPublic!)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    console.error(error)
    // 恢复原状态
    row.isPublic = row.isPublic === 1 ? 0 : 1
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 处理标签 - 确保是有效的JSON格式
    let tagsValue = '[]'
    if (form.tags && Array.isArray(form.tags) && form.tags.length > 0) {
      tagsValue = JSON.stringify(form.tags)
    }

    const submitData = {
      ...form,
      tags: tagsValue
    }

    if (isEdit.value) {
      await interviewApi.updateInterviewTemplate(editId.value!, submitData)
      ElMessage.success('更新成功')
    } else {
      await interviewApi.createInterviewTemplate(submitData)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadTemplateList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  resetForm()
}

const resetForm = () => {
  Object.assign(form, {
    name: '',
    description: '',
    category: '',
    difficulty: 1,
    duration: 30,
    questionCount: 5,
    tags: [],
    isPublic: 1
  })
}

const getCategoryTagType = (category: string) => {
  const types: Record<string, string> = {
    '技术面试': 'primary',
    '行为面试': 'success',
    '算法面试': 'warning',
    '系统设计': 'danger',
    '项目经验': 'info',
    '综合面试': 'info',
    '英语面试': 'primary',
    '管理面试': 'success'
  }
  return types[category] || 'info'
}

const getDifficultyTagType = (difficulty: number) => {
  const types = ['info', 'success', 'warning', 'danger']
  return types[difficulty] || 'info'
}

const getDifficultyText = (difficulty: number) => {
  const texts = ['', '初级', '中级', '高级']
  return texts[difficulty] || '未知'
}

const parseTemplateTags = (tags: string | undefined) => {
  if (!tags) return []
  try {
    return JSON.parse(tags)
  } catch (error) {
    console.warn('解析模板tags失败:', error)
    return []
  }
}
</script>

<style scoped>
.template-manage {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 20px;
}

.search-box {
  flex: 1;
  max-width: 300px;
}

.filter-box {
  display: flex;
  gap: 10px;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.template-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.template-detail h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.template-detail p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-box {
    justify-content: space-between;
  }

  .action-buttons {
    justify-content: center;
  }
}
</style>
