<template>
  <div class="api-config">
    <div class="page-header">
      <h1>API配置管理</h1>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加API配置
      </el-button>
    </div>

    <!-- API配置列表 -->
    <div class="config-list">
      <el-card
        v-for="config in configs"
        :key="config.id"
        class="config-card"
        :class="{ 'is-default': config.isDefault, 'is-disabled': !config.enabled }"
      >
        <template #header>
          <div class="card-header">
            <div class="config-info">
              <h3>{{ config.name }}</h3>
              <el-tag :type="getApiTypeColor(config.apiType)">{{ config.apiType }}</el-tag>
              <el-tag v-if="config.isDefault" type="success" size="small">默认</el-tag>
              <el-tag v-if="!config.enabled" type="info" size="small">已禁用</el-tag>
            </div>
            <div class="card-actions">
              <el-button size="small" @click="testConfig(config)" :loading="config.testing">
                <el-icon><Connection /></el-icon>
                测试
              </el-button>
              <el-dropdown @command="(cmd: string) => handleCommand(cmd, config)">
                <el-button size="small">
                  <el-icon><More /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="toggle">
                      {{ config.enabled ? '禁用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="setDefault" v-if="!config.isDefault">
                      设为默认
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>
        
        <div class="config-details">
          <div class="detail-item">
            <label>模型:</label>
            <span>{{ config.model || '默认' }}</span>
          </div>
          <div class="detail-item">
            <label>最大Token:</label>
            <span>{{ config.maxTokens || '默认' }}</span>
          </div>
          <div class="detail-item">
            <label>温度:</label>
            <span>{{ config.temperature || '默认' }}</span>
          </div>
          <div class="detail-item" v-if="config.description">
            <label>描述:</label>
            <span>{{ config.description }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="configForm" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="configForm.name" placeholder="请输入配置名称" />
        </el-form-item>
        
        <el-form-item label="API类型" prop="apiType">
          <el-select v-model="configForm.apiType" placeholder="选择API类型" @change="onApiTypeChange">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Claude" value="claude" />
            <el-option label="Gemini" value="gemini" />
            <el-option label="通义千问" value="qwen" />
            <el-option label="文心一言" value="ernie" />
            <el-option label="智谱AI" value="zhipu" />
            <el-option label="硅基流动" value="siliconflow" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Azure" value="azure" />
          </el-select>
        </el-form-item>

        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="configForm.modelType" placeholder="选择模型类型" @change="onModelTypeChange">
            <el-option label="文本对话(Chat)" value="chat" />
            <el-option label="语音合成(TTS)" value="tts" />
            <el-option label="语音识别(STT)" value="stt" />
            <el-option label="深度思考(Reasoning)" value="reasoning" />
          </el-select>
        </el-form-item>

        <el-form-item label="API密钥" prop="apiKey">
          <el-input
            v-model="configForm.apiKey"
            type="password"
            placeholder="请输入API密钥"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="基础URL" prop="baseUrl">
          <el-input v-model="configForm.baseUrl" placeholder="API基础URL（可选）" />
        </el-form-item>
        
        <el-form-item label="模型名称" prop="model">
          <div style="display: flex; gap: 8px;">
            <el-select
              v-model="configForm.model"
              placeholder="选择模型（可选）"
              filterable
              allow-create
              style="flex: 1"
              :loading="loadingModels"
            >
              <el-option
                v-for="model in availableModels"
                :key="model"
                :label="model"
                :value="model"
              />
            </el-select>
            <el-button
              @click="loadModels"
              :loading="loadingModels"
              :disabled="!configForm.apiType || !configForm.apiKey"
              size="default"
            >
              <el-icon><Refresh /></el-icon>
              获取模型
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="最大Token数" prop="maxTokens">
          <el-input-number
            v-model="configForm.maxTokens"
            :min="1"
            :max="32000"
            placeholder="最大Token数"
          />
        </el-form-item>
        
        <el-form-item label="温度参数" prop="temperature">
          <el-input-number
            v-model="configForm.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            placeholder="温度参数"
          />
        </el-form-item>
        
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="configForm.systemPrompt"
            type="textarea"
            :rows="3"
            placeholder="定义AI的基本行为和角色（可选）"
          />
        </el-form-item>

        <el-form-item label="面试官角色" prop="interviewerRole">
          <el-input
            v-model="configForm.interviewerRole"
            type="textarea"
            :rows="2"
            placeholder="面试官的专业背景和经验描述（可选）"
          />
        </el-form-item>

        <el-form-item label="面试风格" prop="interviewStyle">
          <el-select v-model="configForm.interviewStyle" placeholder="选择面试风格（可选）">
            <el-option label="专业友好" value="专业友好" />
            <el-option label="深入分析" value="深入分析" />
            <el-option label="本土化专业" value="本土化专业" />
            <el-option label="严格型" value="严格型" />
            <el-option label="鼓励式" value="鼓励式" />
            <el-option label="技术导向" value="技术导向" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="configForm.description"
            type="textarea"
            :rows="2"
            placeholder="配置描述（可选）"
          />
        </el-form-item>

        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="configForm.enabled" />
        </el-form-item>

        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="configForm.isDefault" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          {{ isEdit ? '更新' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection, More, Refresh } from '@element-plus/icons-vue'
import { apiConfigApi } from '@/api/apiConfig'

// 响应式数据
const configs = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<any>()
const loadingModels = ref(false)
const availableModels = ref<string[]>([])

// 配置表单
const configForm = reactive({
  id: null,
  name: '',
  apiType: '',
  modelType: 'chat',
  apiKey: '',
  baseUrl: '',
  model: '',
  maxTokens: null,
  temperature: null,
  systemPrompt: '',
  interviewerRole: '',
  interviewStyle: '',
  description: '',
  enabled: true,
  isDefault: false
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  apiType: [{ required: true, message: '请选择API类型', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入API密钥', trigger: 'blur' }]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑API配置' : '添加API配置')

// 方法
const loadConfigs = async () => {
  try {
    const response = await apiConfigApi.getList(1, 100) // 获取所有配置
    configs.value = response.data?.list || []

    // 过滤掉可能存在的无效ID记录（不存在的记录）
    configs.value = configs.value.filter(config => {
      return config.id && typeof config.id === 'number' && config.id > 0
    })
  } catch (error) {
    ElMessage.error('加载API列表失败，无法添加API配置')
    console.error('API配置加载失败:', error)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const editConfig = (config: any) => {
  isEdit.value = true
  dialogVisible.value = true
  Object.assign(configForm, config)
}

const saveConfig = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    
    if (isEdit.value && configForm.id) {
      const updateData = { ...configForm }
      await apiConfigApi.update(configForm.id, updateData as any)
      ElMessage.success('更新成功')
    } else {
      const { id, ...addData } = configForm
      await apiConfigApi.add(addData as any)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    loadConfigs()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    saving.value = false
  }
}

const deleteConfig = async (config: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这个配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await apiConfigApi.delete(config.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除配置失败:', error)

      // 提取详细错误信息
      let errorMessage = '删除失败'
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message
      } else if (error.message) {
        errorMessage = error.message
      }

      ElMessage.error(errorMessage)

      // 如果是404错误，说明记录已不存在，重新加载列表
      if (error.response?.status === 404 || error.response?.status === 403) {
        loadConfigs()
      }
    }
  }
}

const testConfig = async (config: any) => {
  try {
    config.testing = true
    const response = await apiConfigApi.test(config)
    ElMessage.success((response as any).message || 'API测试成功')
  } catch (error: any) {
    console.error('API测试失败:', error)

    // 提取详细错误信息
    let errorMessage = 'API测试失败'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  } finally {
    config.testing = false
  }
}

const toggleConfig = async (config: any) => {
  try {
    await apiConfigApi.toggleStatus(config.id, !config.enabled)
    config.enabled = !config.enabled
    ElMessage.success(config.enabled ? '已启用' : '已禁用')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const setDefaultConfig = async (config: any) => {
  try {
    await apiConfigApi.setDefault(config.id)
    ElMessage.success('设置默认配置成功')
    loadConfigs() // 重新加载配置列表
  } catch (error) {
    ElMessage.error('设置默认配置失败')
    console.error('设置默认配置失败:', error)
  }
}

const handleCommand = (command: string, config: any) => {
  switch (command) {
    case 'edit':
      editConfig(config)
      break
    case 'toggle':
      toggleConfig(config)
      break
    case 'setDefault':
      setDefaultConfig(config)
      break
    case 'delete':
      deleteConfig(config)
      break
  }
}

const resetForm = () => {
  Object.assign(configForm, {
    id: null,
    name: '',
    apiType: '',
    modelType: 'chat',
    apiKey: '',
    baseUrl: '',
    model: '',
    maxTokens: null,
    temperature: null,
    systemPrompt: '',
    interviewerRole: '',
    interviewStyle: '',
    description: '',
    enabled: true,
    isDefault: false
  })
  availableModels.value = []
  formRef.value?.resetFields()
}

const loadModels = async () => {
  if (!configForm.apiType || !configForm.apiKey) {
    ElMessage.warning('请先选择API类型并输入API密钥')
    return
  }

  try {
    loadingModels.value = true
    const response = await apiConfigApi.getModels({
      apiType: configForm.apiType,
      apiKey: configForm.apiKey,
      baseUrl: configForm.baseUrl
    })

    availableModels.value = response.data || []
    ElMessage.success(`成功获取 ${availableModels.value.length} 个模型`)
  } catch (error) {
    console.error('获取模型列表失败:', error)
    ElMessage.error('获取模型列表失败，请检查API配置')
  } finally {
    loadingModels.value = false
  }
}

const onApiTypeChange = (apiType: string) => {
  // 清空模型列表
  availableModels.value = []

  // 根据API类型设置默认值
  const defaults: Record<string, any> = {
    openai: {
      baseUrl: 'https://api.openai.com/v1',
      model: 'gpt-3.5-turbo',
      maxTokens: 2048,
      temperature: 0.7
    },
    claude: {
      baseUrl: 'https://api.anthropic.com',
      model: 'claude-3-sonnet-20240229',
      maxTokens: 4096,
      temperature: 0.7
    },
    gemini: {
      baseUrl: 'https://generativelanguage.googleapis.com/v1',
      model: 'gemini-pro',
      maxTokens: 2048,
      temperature: 0.7
    },
    qwen: {
      baseUrl: 'https://dashscope.aliyuncs.com/api/v1',
      model: 'qwen-turbo',
      maxTokens: 2048,
      temperature: 0.7
    },
    siliconflow: {
      baseUrl: 'https://api.siliconflow.cn/v1',
      model: 'FunAudioLLM/CosyVoice2-0.5B',
      maxTokens: 2048,
      temperature: 0.7
    },
    deepseek: {
      baseUrl: 'https://api.deepseek.com/v1',
      model: 'deepseek-chat',
      maxTokens: 2048,
      temperature: 0.7
    },
    azure: {
      baseUrl: 'https://speech.platform.bing.com',
      model: 'speech-synthesis',
      maxTokens: 2048,
      temperature: 0.7
    }
  }

  const defaultConfig = defaults[apiType]
  if (defaultConfig) {
    Object.assign(configForm, defaultConfig)
  }
}

const onModelTypeChange = (modelType: string) => {
  // 根据模型类型调整默认模型
  const modelDefaults: Record<string, Record<string, string>> = {
    chat: {
      openai: 'gpt-3.5-turbo',
      claude: 'claude-3-sonnet-20240229',
      gemini: 'gemini-pro',
      qwen: 'qwen-turbo',
      siliconflow: 'deepseek-ai/DeepSeek-V2.5',
      deepseek: 'deepseek-chat'
    },
    tts: {
      openai: 'tts-1',
      siliconflow: 'FunAudioLLM/CosyVoice2-0.5B',
      deepseek: 'tts-1',
      azure: 'speech-synthesis'
    },
    stt: {
      openai: 'whisper-1',
      azure: 'speech-to-text'
    },
    reasoning: {
      deepseek: 'deepseek-reasoner',
      openai: 'gpt-4'
    }
  }

  const typeDefaults = modelDefaults[modelType]
  if (typeDefaults && configForm.apiType && typeDefaults[configForm.apiType]) {
    configForm.model = typeDefaults[configForm.apiType]
  }

  // 清空模型列表，需要重新获取
  availableModels.value = []
}

const getApiTypeColor = (apiType: string) => {
  const colors: Record<string, string> = {
    openai: 'success',
    claude: 'warning',
    gemini: 'primary',
    qwen: 'info',
    ernie: 'danger',
    zhipu: 'success'
  }
  return colors[apiType] || 'info'
}

// 生命周期
onMounted(() => {
  loadConfigs()
})
</script>

<style lang="scss" scoped>
.api-config {
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

.config-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.config-card {
  &.is-default {
    border-color: #67c23a;
  }
  
  &.is-disabled {
    opacity: 0.6;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    
    .config-info {
      flex: 1;
      
      h3 {
        margin: 0 0 8px 0;
        font-size: 16px;
        color: #333;
      }
      
      .el-tag {
        margin-right: 8px;
      }
    }
    
    .card-actions {
      display: flex;
      gap: 8px;
    }
  }
}

.config-details {
  .detail-item {
    display: flex;
    margin-bottom: 8px;
    
    label {
      width: 80px;
      color: #666;
      font-weight: 500;
    }
    
    span {
      flex: 1;
      color: #333;
    }
  }
}
</style>
