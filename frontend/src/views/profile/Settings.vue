<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>系统设置</h1>
      <p>个性化您的使用体验</p>
    </div>

    <div class="settings-content">
      <!-- 通用设置 -->
      <el-card class="settings-card">
        <template #header>
          <span>通用设置</span>
        </template>

        <div class="setting-items">
          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">语言设置</div>
              <div class="item-desc">选择您的首选语言</div>
            </div>
            <el-select v-model="settings.language" style="width: 150px">
              <el-option label="简体中文" value="zh-CN" />
              <el-option label="English" value="en-US" />
            </el-select>
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">主题模式</div>
              <div class="item-desc">选择浅色或深色主题</div>
            </div>
            <el-radio-group v-model="settings.theme">
              <el-radio label="light">浅色</el-radio>
              <el-radio label="dark">深色</el-radio>
              <el-radio label="auto">跟随系统</el-radio>
            </el-radio-group>
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">自动保存</div>
              <div class="item-desc">自动保存面试回答草稿</div>
            </div>
            <el-switch v-model="settings.autoSave" />
          </div>
        </div>
      </el-card>

      <!-- 面试设置 -->
      <el-card class="settings-card">
        <template #header>
          <span>面试设置</span>
        </template>

        <div class="setting-items">
          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">默认AI模型</div>
              <div class="item-desc">选择默认使用的AI模型</div>
            </div>
            <el-select
              v-model="settings.defaultAiModel"
              style="width: 200px"
              :loading="loadingModels"
              placeholder="选择AI模型"
            >
              <el-option
                v-for="model in availableModels"
                :key="model.id"
                :label="model.name"
                :value="model.id"
              />
            </el-select>
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">语音面试</div>
              <div class="item-desc">默认启用语音面试功能</div>
            </div>
            <el-switch v-model="settings.voiceEnabled" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">面试提醒</div>
              <div class="item-desc">在面试开始前发送提醒</div>
            </div>
            <el-switch v-model="settings.interviewReminder" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">自动评分</div>
              <div class="item-desc">回答后自动进行AI评分</div>
            </div>
            <el-switch v-model="settings.autoScoring" />
          </div>
        </div>
      </el-card>

      <!-- 通知设置 -->
      <el-card class="settings-card">
        <template #header>
          <span>通知设置</span>
        </template>

        <div class="setting-items">
          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">邮件通知</div>
              <div class="item-desc">接收重要更新的邮件通知</div>
            </div>
            <el-switch v-model="settings.emailNotification" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">浏览器通知</div>
              <div class="item-desc">在浏览器中显示通知</div>
            </div>
            <el-switch v-model="settings.browserNotification" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">面试完成通知</div>
              <div class="item-desc">面试完成后发送通知</div>
            </div>
            <el-switch v-model="settings.completionNotification" />
          </div>
        </div>
      </el-card>

      <!-- 隐私设置 -->
      <el-card class="settings-card">
        <template #header>
          <span>隐私设置</span>
        </template>

        <div class="setting-items">
          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">数据分析</div>
              <div class="item-desc">允许收集匿名使用数据以改进服务</div>
            </div>
            <el-switch v-model="settings.analytics" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">面试记录保存</div>
              <div class="item-desc">保存面试录音和文本记录</div>
            </div>
            <el-switch v-model="settings.saveRecords" />
          </div>

          <div class="setting-item">
            <div class="item-info">
              <div class="item-title">自动清理</div>
              <div class="item-desc">自动清理30天前的面试记录</div>
            </div>
            <el-switch v-model="settings.autoCleanup" />
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="settings-actions">
        <el-button @click="resetSettings">恢复默认</el-button>
        <el-button type="primary" :loading="saving" @click="saveSettings">
          保存设置
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi } from '@/api/ai'

const saving = ref(false)
const loadingModels = ref(false)
const availableModels = ref<any[]>([])

// 设置数据
const settings = reactive({
  // 通用设置
  language: 'zh-CN',
  theme: 'light',
  autoSave: true,
  
  // 面试设置
  defaultAiModel: 'gpt-3.5-turbo',
  voiceEnabled: false,
  interviewReminder: true,
  autoScoring: true,
  
  // 通知设置
  emailNotification: true,
  browserNotification: true,
  completionNotification: true,
  
  // 隐私设置
  analytics: true,
  saveRecords: true,
  autoCleanup: true
})

// 默认设置
const defaultSettings = { ...settings }

onMounted(() => {
  loadSettings()
  loadAiModels()
  applyThemeSettings()
})

// 加载AI模型列表
const loadAiModels = async () => {
  try {
    loadingModels.value = true
    const response = await aiApi.getAvailableModels()
    availableModels.value = response.data || []
  } catch (error) {
    console.error('加载AI模型失败:', error)
    availableModels.value = []
  } finally {
    loadingModels.value = false
  }
}

// 加载设置
const loadSettings = () => {
  const savedSettings = localStorage.getItem('userSettings')
  if (savedSettings) {
    try {
      const parsed = JSON.parse(savedSettings)
      Object.assign(settings, parsed)
    } catch (error) {
      console.error('加载设置失败:', error)
    }
  }
}

// 保存设置
const saveSettings = async () => {
  try {
    saving.value = true

    // 保存到本地存储
    localStorage.setItem('userSettings', JSON.stringify(settings))

    // 应用主题设置
    applyThemeSettings()

    ElMessage.success('设置保存成功')
  } catch (error) {
    ElMessage.error('保存设置失败')
    console.error('保存设置失败:', error)
  } finally {
    saving.value = false
  }
}

// 应用主题设置
const applyThemeSettings = () => {
  const html = document.documentElement

  if (settings.theme === 'dark') {
    html.classList.add('dark')
  } else if (settings.theme === 'light') {
    html.classList.remove('dark')
  } else {
    // 跟随系统
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (prefersDark) {
      html.classList.add('dark')
    } else {
      html.classList.remove('dark')
    }
  }
}

// 恢复默认设置
const resetSettings = async () => {
  try {
    await ElMessageBox.confirm('确定要恢复默认设置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    Object.assign(settings, defaultSettings)
    ElMessage.success('已恢复默认设置')
  } catch (error) {
    // 用户取消
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  max-width: 800px;
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

.settings-content {
  .settings-card {
    margin-bottom: 24px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.setting-items {
  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid #ebeef5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .item-info {
      flex: 1;
      
      .item-title {
        font-size: 16px;
        color: #333;
        margin-bottom: 4px;
      }
      
      .item-desc {
        font-size: 14px;
        color: #666;
      }
    }
  }
}

.settings-actions {
  text-align: center;
  margin-top: 32px;
  
  .el-button {
    margin: 0 8px;
  }
}

@media (max-width: 768px) {
  .settings-page {
    padding: 16px;
  }
  
  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
