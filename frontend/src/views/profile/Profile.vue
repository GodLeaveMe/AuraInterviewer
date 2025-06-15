<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>个人资料</h1>
      <p>管理您的个人信息和账户设置</p>
    </div>

    <div class="profile-content">
      <!-- 基本信息 -->
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-button 
              v-if="!editing" 
              type="primary" 
              size="small"
              @click="startEdit"
            >
              编辑
            </el-button>
            <div v-else class="edit-actions">
              <el-button size="small" @click="cancelEdit">取消</el-button>
              <el-button 
                type="primary" 
                size="small" 
                :loading="saving"
                @click="saveProfile"
              >
                保存
              </el-button>
            </div>
          </div>
        </template>

        <div class="profile-form">
          <div class="avatar-section">
            <div class="avatar-display">
              <el-avatar :size="80" :src="profileForm.avatar">
                {{ profileForm.nickname?.[0] || profileForm.username?.[0] }}
              </el-avatar>
              <div v-if="editing" class="avatar-upload">
                <el-upload
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="uploadAvatar"
                  accept="image/*"
                >
                  <el-button size="small">更换头像</el-button>
                </el-upload>
              </div>
            </div>
          </div>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            :disabled="!editing"
          >
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="profileForm.username" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" disabled />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="profileForm.gender">
                    <el-radio :label="0">保密</el-radio>
                    <el-radio :label="1">男</el-radio>
                    <el-radio :label="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生日" prop="birthday">
                  <el-date-picker
                    v-model="profileForm.birthday"
                    type="date"
                    placeholder="选择生日"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="职业" prop="profession">
                  <el-input v-model="profileForm.profession" placeholder="请输入职业" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="工作经验" prop="experienceYears">
                  <el-input-number
                    v-model="profileForm.experienceYears"
                    :min="0"
                    :max="50"
                    controls-position="right"
                    style="width: 100%"
                  />
                  <span style="margin-left: 8px;">年</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>
      </el-card>

      <!-- 账户安全 -->
      <el-card class="security-card">
        <template #header>
          <span>账户安全</span>
        </template>

        <div class="security-items">
          <div class="security-item">
            <div class="item-info">
              <div class="item-title">登录密码</div>
              <div class="item-desc">定期更换密码可以提高账户安全性</div>
            </div>
            <el-button @click="showChangePassword">修改密码</el-button>
          </div>

          <div class="security-item">
            <div class="item-info">
              <div class="item-title">邮箱验证</div>
              <div class="item-desc">
                {{ userStore.userInfo?.email }}
                <el-tag v-if="emailVerified" type="success" size="small">已验证</el-tag>
                <el-tag v-else type="warning" size="small">未验证</el-tag>
              </div>
            </div>
            <el-button v-if="!emailVerified" type="primary">验证邮箱</el-button>
          </div>

          <div class="security-item">
            <div class="item-info">
              <div class="item-title">最后登录</div>
              <div class="item-desc">
                {{ userStore.userInfo?.lastLoginTime ? formatTime(userStore.userInfo.lastLoginTime) : '暂无记录' }}
                <span v-if="userStore.userInfo?.lastLoginIp">
                  ({{ userStore.userInfo.lastLoginIp }})
                </span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 数据统计 -->
      <el-card class="stats-card">
        <template #header>
          <div class="stats-header">
            <span>数据统计</span>
            <el-button size="small" @click="loadStats">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <div v-loading="statsLoading" class="stats-content">
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-number">{{ stats.totalInterviews }}</div>
              <div class="stat-label">总面试次数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ stats.completedInterviews }}</div>
              <div class="stat-label">已完成面试</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ stats.averageScore.toFixed(1) }}</div>
              <div class="stat-label">平均分数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ formatDuration(stats.totalDuration) }}</div>
              <div class="stat-label">总练习时长</div>
            </div>
          </div>

          <!-- 完成率进度条 -->
          <div v-if="stats.totalInterviews > 0" class="completion-rate">
            <div class="rate-header">
              <span>完成率</span>
              <span class="rate-value">{{ stats.completionRate?.toFixed(1) || 0 }}%</span>
            </div>
            <el-progress
              :percentage="stats.completionRate || 0"
              :stroke-width="8"
            />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="500px"
      @close="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          :loading="changingPassword"
          @click="changePassword"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElForm, ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import type { User } from '@/types/user'
import { fileApi } from '@/api/file'
import { interviewApi } from '@/api/interview'
import dayjs from 'dayjs'

const userStore = useUserStore()

const editing = ref(false)
const saving = ref(false)
const passwordDialogVisible = ref(false)
const changingPassword = ref(false)
const emailVerified = ref(false) // 默认未验证
const statsLoading = ref(false)

const profileFormRef = ref<InstanceType<typeof ElForm>>()
const passwordFormRef = ref<InstanceType<typeof ElForm>>()

// 个人资料表单
const profileForm = reactive<Partial<User>>({
  username: '',
  email: '',
  nickname: '',
  avatar: '',
  phone: '',
  gender: 0,
  birthday: '',
  profession: '',
  experienceYears: 0
})

// 原始数据备份
const originalProfile = reactive<Partial<User>>({})

// 密码修改表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 统计数据
const stats = reactive({
  totalInterviews: 0,
  completedInterviews: 0,
  averageScore: 0,
  totalDuration: 0,
  completionRate: 0
})

// 表单验证规则
const profileRules = {
  nickname: [
    { max: 50, message: '昵称长度不能超过50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  profession: [
    { max: 100, message: '职业长度不能超过100个字符', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadProfile()
  loadStats()
})

// 加载个人资料
const loadProfile = () => {
  if (userStore.userInfo) {
    Object.assign(profileForm, userStore.userInfo)
    Object.assign(originalProfile, userStore.userInfo)

    // 检查邮箱验证状态（如果用户信息中有这个字段）
    emailVerified.value = (userStore.userInfo as any).emailVerified || false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    statsLoading.value = true
    // 从面试API获取统计数据
    const response = await interviewApi.getInterviewStatistics()
    if (response.data) {
      const data = response.data as any
      stats.totalInterviews = data.totalInterviews || 0
      stats.completedInterviews = data.completedInterviews || 0
      stats.averageScore = data.averageScore || 0
      stats.totalDuration = data.totalDuration || 0
      stats.completionRate = data.completionRate || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
    // 设置默认值
    stats.totalInterviews = 0
    stats.completedInterviews = 0
    stats.averageScore = 0
    stats.totalDuration = 0
    stats.completionRate = 0
  } finally {
    statsLoading.value = false
  }
}

// 开始编辑
const startEdit = () => {
  editing.value = true
}

// 取消编辑
const cancelEdit = () => {
  editing.value = false
  Object.assign(profileForm, originalProfile)
}

// 保存个人资料
const saveProfile = async () => {
  if (!profileFormRef.value) return
  
  try {
    const valid = await profileFormRef.value.validate()
    if (!valid) return
    
    saving.value = true
    const success = await userStore.updateUserInfo(profileForm)
    
    if (success) {
      editing.value = false
      Object.assign(originalProfile, profileForm)
      ElMessage.success('个人资料更新成功')
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
  } finally {
    saving.value = false
  }
}

// 显示修改密码对话框
const showChangePassword = () => {
  passwordDialogVisible.value = true
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return
    
    changingPassword.value = true
    const success = await userStore.changePassword(
      passwordForm.oldPassword,
      passwordForm.newPassword
    )
    
    if (success) {
      passwordDialogVisible.value = false
      resetPasswordForm()
    }
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    changingPassword.value = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  if (passwordFormRef.value) {
    passwordFormRef.value.clearValidate()
  }
}

// 头像上传前验证
const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!')
    return false
  }
  return true
}

// 自定义头像上传
const uploadAvatar = async (options: any) => {
  try {
    const response = await fileApi.uploadAvatar(options.file)
    if (response.data.code === 200) {
      profileForm.avatar = response.data.data.url
      ElMessage.success('头像上传成功')
      options.onSuccess(response.data)
    } else {
      ElMessage.error(response.data.message || '头像上传失败')
      options.onError(new Error(response.data.message))
    }
  } catch (error) {
    ElMessage.error('头像上传失败')
    options.onError(error)
  }
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
.profile-page {
  max-width: 1000px;
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

.profile-content {
  .el-card {
    margin-bottom: 24px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .edit-actions {
    display: flex;
    gap: 8px;
  }
}

.profile-form {
  .avatar-section {
    text-align: center;
    margin-bottom: 32px;
    
    .avatar-display {
      display: inline-block;
      
      .avatar-upload {
        margin-top: 12px;
      }
    }
  }
}

.security-items {
  .security-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid #ebeef5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .item-info {
      .item-title {
        font-size: 16px;
        color: #333;
        margin-bottom: 4px;
      }
      
      .item-desc {
        font-size: 14px;
        color: #666;
        
        .el-tag {
          margin-left: 8px;
        }
      }
    }
  }
}

.stats-card {
  .stats-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 24px;
    margin-bottom: 24px;

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

  .completion-rate {
    .rate-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .rate-value {
        font-weight: 600;
        color: #409eff;
      }
    }
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }
  
  .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
