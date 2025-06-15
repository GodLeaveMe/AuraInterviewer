<template>
  <div class="template-manage">
    <div class="page-header">
      <h1>面试模板管理</h1>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加模板
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="search-bar">
      <el-form :model="searchForm" inline>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="选择分类" clearable>
            <el-option label="技术面试" value="技术面试" />
            <el-option label="行为面试" value="行为面试" />
            <el-option label="算法面试" value="算法面试" />
            <el-option label="系统设计" value="系统设计" />
            <el-option label="项目经验" value="项目经验" />
            <el-option label="综合面试" value="综合面试" />
            <el-option label="英语面试" value="英语面试" />
            <el-option label="管理面试" value="管理面试" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="searchForm.difficulty" placeholder="选择难度" clearable>
            <el-option label="初级" value="1" />
            <el-option label="中级" value="2" />
            <el-option label="高级" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTemplates">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 模板列表 -->
    <el-table :data="templates" v-loading="loading">
      <el-table-column prop="name" label="模板名称" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="getDifficultyType(row.difficulty)">
            {{ getDifficultyText(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="100" />
      <el-table-column prop="duration" label="时长(分钟)" width="120" />
      <el-table-column prop="usageCount" label="使用次数" width="120" />
      <el-table-column prop="isPublic" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewTemplate(row)">查看</el-button>
          <el-button size="small" type="primary" @click="editTemplate(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteTemplate(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTemplates"
        @current-change="loadTemplates"
      />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="resetForm"
    >
      <el-form :model="templateForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="templateForm.category" placeholder="选择分类">
            <el-option label="技术面试" value="技术面试" />
            <el-option label="行为面试" value="行为面试" />
            <el-option label="算法面试" value="算法面试" />
            <el-option label="系统设计" value="系统设计" />
            <el-option label="项目经验" value="项目经验" />
            <el-option label="综合面试" value="综合面试" />
            <el-option label="英语面试" value="英语面试" />
            <el-option label="管理面试" value="管理面试" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="templateForm.difficulty" placeholder="选择难度">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(分钟)" prop="duration">
          <el-input-number v-model="templateForm.duration" :min="10" :max="180" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="templateForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
          />
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="templateForm.isPublic" />
        </el-form-item>
        <el-form-item label="问题列表" prop="config">
          <div class="questions-editor">
            <div
              v-for="(question, index) in questions"
              :key="index"
              class="question-item"
            >
              <div class="question-header">
                <span>问题 {{ index + 1 }}</span>
                <el-button
                  size="small"
                  type="danger"
                  text
                  @click="removeQuestion(index)"
                >
                  删除
                </el-button>
              </div>
              <el-input
                v-model="question.question"
                placeholder="请输入问题内容"
                class="question-input"
              />
              <el-input
                v-model="question.type"
                placeholder="问题类型（如：技术基础、项目经验等）"
                class="question-type"
              />
            </div>
            <el-button @click="addQuestion" class="add-question-btn">
              <el-icon><Plus /></el-icon>
              添加问题
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate" :loading="saving">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import type { InterviewTemplate } from '@/types/interview'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const templates = ref<InterviewTemplate[]>([])
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  category: '',
  difficulty: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 模板表单
const templateForm = reactive({
  id: null,
  name: '',
  category: '',
  difficulty: 1,
  duration: 60,
  description: '',
  isPublic: false,
  config: ''
})

// 问题列表
const questions = ref([
  { question: '', type: '' }
])

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑模板' : '添加模板')

// 方法
const loadTemplates = async () => {
  try {
    loading.value = true

    // 获取我的模板（用户自己创建的）
    const response = await interviewApi.getMyInterviewTemplates()
    templates.value = response.data || []
    pagination.total = templates.value.length
  } catch (error) {
    console.error('加载模板失败:', error)
    ElMessage.error('加载模板失败')
    templates.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const editTemplate = (template: any) => {
  isEdit.value = true
  dialogVisible.value = true
  Object.assign(templateForm, template)
  
  // 解析问题配置
  try {
    const config = JSON.parse(template.config || '{}')
    questions.value = config.questions || [{ question: '', type: '' }]
  } catch (error) {
    questions.value = [{ question: '', type: '' }]
  }
}

const viewTemplate = (_template: any) => {
  // 实现查看模板详情
  ElMessage.info('查看功能开发中')
}

const deleteTemplate = async (template: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这个模板吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await interviewApi.deleteInterviewTemplate(template.id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const saveTemplate = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    // 构建配置
    const config = {
      questions: questions.value.filter(q => q.question.trim())
    }

    // 构建提交数据
    const submitData = {
      name: templateForm.name,
      description: templateForm.description,
      category: templateForm.category,
      difficulty: templateForm.difficulty,
      duration: templateForm.duration,
      questionCount: config.questions.length,
      tags: '[]', // 可以后续扩展
      config: config,
      isPublic: templateForm.isPublic ? 1 : 0
    }

    if (isEdit.value && templateForm.id) {
      await interviewApi.updateInterviewTemplate(templateForm.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await interviewApi.createInterviewTemplate(submitData)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadTemplates()
  } catch (error) {
    console.error('保存模板失败:', error)
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  Object.assign(templateForm, {
    id: null,
    name: '',
    category: '',
    difficulty: 1,
    duration: 60,
    description: '',
    isPublic: false,
    config: ''
  })
  questions.value = [{ question: '', type: '' }]
  formRef.value?.resetFields()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    category: '',
    difficulty: ''
  })
  loadTemplates()
}

const addQuestion = () => {
  questions.value.push({ question: '', type: '' })
}

const removeQuestion = (index: number) => {
  if (questions.value.length > 1) {
    questions.value.splice(index, 1)
  }
}

const getDifficultyType = (difficulty: number) => {
  const types = ['success', 'warning', 'danger']
  return types[difficulty - 1] || 'info'
}

const getDifficultyText = (difficulty: number) => {
  const texts = ['初级', '中级', '高级']
  return texts[difficulty - 1] || '未知'
}

// 生命周期
onMounted(() => {
  loadTemplates()
})
</script>

<style lang="scss" scoped>
.template-manage {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  h1 {
    margin: 0;
    font-size: 24px;
    color: #333;
  }
}

.search-bar {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.questions-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  
  .question-item {
    margin-bottom: 16px;
    
    .question-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-weight: 500;
    }
    
    .question-input {
      margin-bottom: 8px;
    }
    
    .question-type {
      margin-bottom: 8px;
    }
  }
  
  .add-question-btn {
    width: 100%;
    border-style: dashed;
  }
}
</style>
