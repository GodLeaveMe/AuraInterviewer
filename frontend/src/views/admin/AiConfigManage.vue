<template>
  <div class="ai-config-manage">
    <div class="page-header">
      <h1>AI配置管理</h1>
      <p>管理系统内置的AI模型配置</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="search-box">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索配置名称、模型或提供商"
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
          v-model="searchForm.provider"
          placeholder="选择提供商"
          clearable
          style="width: 150px"
        >
          <el-option label="OpenAI" value="openai" />
          <el-option label="Claude" value="claude" />
          <el-option label="通义千问" value="qwen" />
          <el-option label="文心一言" value="ernie" />
        </el-select>
        
        <el-select
          v-model="searchForm.isActive"
          placeholder="选择状态"
          clearable
          style="width: 120px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
          新增配置
        </el-button>
      </div>
    </div>

    <!-- 配置列表 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="configList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="配置名称" min-width="150">
          <template #default="{ row }">
            <div class="config-name">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.priority === 100" type="warning" size="small">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="provider" label="提供商" width="120">
          <template #default="{ row }">
            <el-tag :type="getProviderTagType(row.provider)">
              {{ getProviderName(row.provider) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="model" label="模型" width="150" />
        
        <el-table-column prop="maxTokens" label="最大Token" width="100" />
        
        <el-table-column prop="temperature" label="温度" width="80" />
        
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="priority" label="优先级" width="80" />
        
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleTest(row)"
              :loading="testingIds.includes(row.id)"
            >
              测试
            </el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.priority !== 100"
              type="success"
              size="small"
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
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

    <!-- 配置表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="配置名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入配置名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务提供商" prop="provider">
              <el-select v-model="form.provider" placeholder="请选择提供商">
                <el-option label="OpenAI" value="openai" />
                <el-option label="Claude" value="claude" />
                <el-option label="通义千问" value="qwen" />
                <el-option label="文心一言" value="ernie" />
                <el-option label="硅基流动" value="siliconflow" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="Azure" value="azure" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType" placeholder="请选择模型类型">
                <el-option label="文本对话(Chat)" value="chat" />

                <el-option label="深度思考(Reasoning)" value="reasoning" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="model">
              <div class="model-selector">
                <el-select
                  v-model="form.model"
                  placeholder="选择模型或手动输入"
                  filterable
                  allow-create
                  :loading="loadingModels"
                  style="width: 100%"
                >
                  <el-option
                    v-for="model in availableModels"
                    :key="model.id"
                    :label="model.name"
                    :value="model.id"
                  >
                    <div class="model-option">
                      <span class="model-name">{{ model.name }}</span>
                      <span class="model-desc">{{ model.description }}</span>
                      <el-tag
                        v-if="model.supportsThinking"
                        size="small"
                        type="success"
                        style="margin-left: 8px"
                      >
                        思维链
                      </el-tag>
                      <el-tag
                        v-if="model.available"
                        size="small"
                        type="primary"
                        style="margin-left: 4px"
                      >
                        可用
                      </el-tag>
                    </div>
                  </el-option>
                </el-select>
                <el-button
                  type="primary"
                  :icon="Refresh"
                  @click="handleRefreshModels"
                  :loading="loadingModels"
                  :disabled="!form.provider || !form.apiKey"
                  style="margin-left: 8px"
                >
                  获取模型
                </el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API地址" prop="apiUrl">
              <el-input v-model="form.apiUrl" placeholder="API接口地址" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="API密钥" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            placeholder="请输入API密钥"
            show-password
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number
                v-model="form.maxTokens"
                :min="1"
                :max="32000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="温度参数" prop="temperature">
              <el-input-number
                v-model="form.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-input-number
                v-model="form.priority"
                :min="1"
                :max="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Top P" prop="topP">
              <el-input-number
                v-model="form.topP"
                :min="0"
                :max="1"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="频率惩罚" prop="frequencyPenalty">
              <el-input-number
                v-model="form.frequencyPenalty"
                :min="-2"
                :max="2"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="存在惩罚" prop="presencePenalty">
              <el-input-number
                v-model="form.presencePenalty"
                :min="-2"
                :max="2"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="系统提示词">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="3"
            placeholder="可选的系统提示词"
          />
        </el-form-item>
        
        <el-form-item label="配置描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="配置说明"
          />
        </el-form-item>
        
        <el-form-item label="启用状态">
          <el-switch
            v-model="form.isActive"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="handleTestConfig"
            :loading="testingConfig"
          >
            测试连接
          </el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { aiApi, type AiConfig, type AiConfigRequest } from '@/api/ai'
import { formatTime } from '@/utils/date'

// 响应式数据
const loading = ref(false)
const configList = ref<AiConfig[]>([])
const testingIds = ref<number[]>([])
const testingConfig = ref(false)
const submitting = ref(false)
const loadingModels = ref(false)
const availableModels = ref<any[]>([])

// 搜索表单
const searchForm = reactive({
  keyword: '',
  provider: '',
  isActive: undefined as number | undefined
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

// 表单
const formRef = ref()
const form = reactive<AiConfigRequest>({
  name: '',
  provider: '',
  model: '',
  modelType: 'chat',
  apiKey: '',
  apiUrl: '',
  maxTokens: 4096,
  temperature: 0.7,
  topP: 1.0,
  frequencyPenalty: 0.0,
  presencePenalty: 0.0,
  systemPrompt: '',
  isActive: 1,
  priority: 1,
  description: ''
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '请选择服务提供商', trigger: 'change' }
  ],
  modelType: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ],
  model: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: '请输入API密钥', trigger: 'blur' }
  ],
  maxTokens: [
    { required: true, message: '请输入最大Token数', trigger: 'blur' }
  ],
  temperature: [
    { required: true, message: '请输入温度参数', trigger: 'blur' }
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'blur' }
  ]
}

// 生命周期
onMounted(() => {
  loadConfigList()
})

// 方法
const loadConfigList = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      provider: searchForm.provider || undefined,
      isActive: searchForm.isActive
    }

    const response = await aiApi.getAiConfigList(params)
    configList.value = response.data.records
    pagination.total = response.data.total
  } catch (error) {
    ElMessage.error('加载配置列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadConfigList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.provider = ''
  searchForm.isActive = undefined
  pagination.current = 1
  loadConfigList()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  loadConfigList()
}

const handleCurrentChange = (current: number) => {
  pagination.current = current
  loadConfigList()
}

const handleCreate = () => {
  dialogTitle.value = '新增AI配置'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: AiConfig) => {
  dialogTitle.value = '编辑AI配置'
  isEdit.value = true
  editId.value = row.id

  // 填充表单
  Object.assign(form, {
    name: row.name,
    provider: row.provider,
    model: row.model,
    modelType: row.modelType || 'chat',
    apiKey: row.apiKey,
    apiUrl: row.apiUrl,
    maxTokens: row.maxTokens,
    temperature: row.temperature,
    topP: row.topP,
    frequencyPenalty: row.frequencyPenalty,
    presencePenalty: row.presencePenalty,
    systemPrompt: row.systemPrompt,
    isActive: row.isActive,
    priority: row.priority,
    description: row.description
  })

  dialogVisible.value = true
}

const handleDelete = async (row: AiConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置"${row.name}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await aiApi.deleteAiConfig(row.id!)
    ElMessage.success('删除成功')
    loadConfigList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

const handleStatusChange = async (row: AiConfig) => {
  try {
    await aiApi.updateAiConfigStatus(row.id!, row.isActive!)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    console.error(error)
    // 恢复原状态
    row.isActive = row.isActive === 1 ? 0 : 1
  }
}

const handleTest = async (row: AiConfig) => {
  try {
    testingIds.value.push(row.id!)
    const result = await aiApi.testAiConfig(row.id!)

    if (result.data) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接测试失败')
    }
  } catch (error: any) {
    console.error('AI配置测试失败:', error)

    // 提取详细错误信息
    let errorMessage = '连接测试失败'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  } finally {
    testingIds.value = testingIds.value.filter(id => id !== row.id)
  }
}

const handleSetDefault = async (row: AiConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要将"${row.name}"设为默认配置吗？`,
      '确认设置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await aiApi.setDefaultAiConfig(row.id!)
    ElMessage.success('设置成功')
    loadConfigList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('设置失败')
      console.error(error)
    }
  }
}

const handleTestConfig = async () => {
  try {
    await formRef.value.validate()
    testingConfig.value = true

    // 确保model字段是字符串而不是数组
    const testData = { ...form }
    if (Array.isArray(testData.model)) {
      testData.model = testData.model[0] || ''
    }

    const result = await aiApi.testAiConfigWithData(testData)

    if (result.data) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接测试失败')
    }
  } catch (error: any) {
    console.error('AI配置测试失败:', error)

    // 提取详细错误信息
    let errorMessage = '连接测试失败'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  } finally {
    testingConfig.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 确保model字段是字符串而不是数组
    const submitData = { ...form }
    if (Array.isArray(submitData.model)) {
      submitData.model = submitData.model[0] || ''
    }

    if (isEdit.value) {
      await aiApi.updateAiConfig(editId.value!, submitData)
      ElMessage.success('更新成功')
    } else {
      await aiApi.createAiConfig(submitData)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadConfigList()
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
    provider: '',
    model: '',
    modelType: 'chat',
    apiKey: '',
    apiUrl: '',
    maxTokens: 4096,
    temperature: 0.7,
    topP: 1.0,
    frequencyPenalty: 0.0,
    presencePenalty: 0.0,
    systemPrompt: '',
    isActive: 1,
    priority: 1,
    description: ''
  })
  availableModels.value = []
}

const handleRefreshModels = async () => {
  if (!form.provider || !form.apiKey) {
    ElMessage.warning('请先填写服务提供商和API密钥')
    return
  }

  try {
    loadingModels.value = true

    // 所有模型类型都从API动态获取
    const response = await aiApi.getAvailableModels({
      provider: form.provider,
      apiKey: form.apiKey,
      apiUrl: form.apiUrl || ''
    })

    if (response.data && Array.isArray(response.data)) {
      // 将字符串数组转换为对象数组
      availableModels.value = response.data.map((model: any) => {
        if (typeof model === 'string') {
          return {
            id: model,
            name: model,
            description: `${form.modelType.toUpperCase()}模型`,
            available: true,
            supportsThinking: form.modelType === 'reasoning'
          }
        }
        return model
      })
      ElMessage.success(`获取到 ${availableModels.value.length} 个模型`)
    } else {
      availableModels.value = []
      ElMessage.warning('未获取到可用模型')
    }

    // 如果当前选择的模型不在列表中，清空选择
    if (form.model && !availableModels.value.some(m => m.id === form.model || m.name === form.model)) {
      form.model = ''
    }

  } catch (error: any) {
    console.error('获取模型列表失败:', error)

    // 提取详细错误信息
    let errorMessage = '获取模型列表失败，请检查API配置'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
    availableModels.value = []
  } finally {
    loadingModels.value = false
  }
}

const getModelsForProvider = (provider: string, modelType: string): string[] => {
  const modelMap: Record<string, Record<string, string[]>> = {
    siliconflow: {

      chat: ['deepseek-ai/DeepSeek-V2.5', 'Qwen/Qwen2.5-7B-Instruct'],
      reasoning: ['deepseek-ai/DeepSeek-R1']
    },
    openai: {
      tts: ['tts-1', 'tts-1-hd'],
      stt: ['whisper-1'],
      chat: ['gpt-3.5-turbo', 'gpt-4', 'gpt-4-turbo', 'gpt-4o', 'gpt-4o-mini'],
      reasoning: ['gpt-4', 'gpt-4-turbo', 'gpt-4o']
    },
    deepseek: {
      tts: ['tts-1'],
      stt: ['whisper-1'],
      chat: ['deepseek-chat'],
      reasoning: ['deepseek-reasoner']
    },
    azure: {
      tts: ['speech-synthesis'],
      stt: ['speech-to-text'],
      chat: ['gpt-35-turbo', 'gpt-4'],
      reasoning: ['gpt-4']
    }
  }

  return modelMap[provider]?.[modelType] || []
}

const getProviderName = (provider: string) => {
  const names: Record<string, string> = {
    openai: 'OpenAI',
    claude: 'Claude',
    qwen: '通义千问',
    ernie: '文心一言',
    deepseek: 'DeepSeek',
    siliconflow: '硅基流动',
    azure: 'Azure'
  }
  return names[provider] || provider
}

const getProviderTagType = (provider: string) => {
  const types: Record<string, string> = {
    openai: 'primary',
    claude: 'success',
    qwen: 'warning',
    ernie: 'info',
    deepseek: 'danger',
    siliconflow: 'primary',
    azure: 'info'
  }
  return types[provider] || 'info'
}
</script>

<style scoped>
.ai-config-manage {
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

.config-name {
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

.model-selector {
  display: flex;
  align-items: center;
}

.model-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}

.model-name {
  font-weight: 600;
  color: #303133;
}

.model-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
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
