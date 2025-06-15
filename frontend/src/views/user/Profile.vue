<template>
  <div class="user-profile">
    <div class="page-header">
      <h1>个人资料</h1>
      <p>管理您的个人信息和账户设置</p>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：头像和基本信息 -->
      <el-col :span="8">
        <!-- 头像卡片 -->
        <el-card class="avatar-card">
          <div class="avatar-section">
            <div class="avatar-container">
              <el-avatar
                :size="120"
                :src="userInfo?.avatar"
                class="user-avatar"
              >
                <span class="avatar-text">{{ getAvatarText() }}</span>
              </el-avatar>
              <el-button
                type="primary"
                size="small"
                class="change-avatar-btn"
                @click="showAvatarDialog"
              >
                <el-icon><Camera /></el-icon>
                更换头像
              </el-button>
            </div>
            <div class="user-basic-info">
              <h3 class="username">{{ userInfo?.nickname || userInfo?.username || '未设置昵称' }}</h3>
              <p class="user-email">{{ userInfo?.email || '未设置邮箱' }}</p>
              <el-tag
                :type="userInfo?.status === 1 ? 'success' : 'info'"
                size="small"
              >
                {{ userInfo?.status === 1 ? '正常' : '未激活' }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <!-- 统计信息 -->
        <el-card title="我的统计" style="margin-top: 16px;" v-loading="statsLoading">
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ stats.totalInterviews || 0 }}</div>
                <div class="stat-label">面试次数</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Trophy /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ stats.averageScore?.toFixed(1) || '0.0' }}</div>
                <div class="stat-label">平均分数</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ formatDuration(stats.totalDuration || 0) }}</div>
                <div class="stat-label">总时长</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ stats.completedInterviews || 0 }}</div>
                <div class="stat-label">已完成</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：详细信息 -->
      <el-col :span="16">
        <el-card title="个人信息">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
              <el-button type="primary" @click="showEditDialog">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
            </div>
          </template>

          <div class="profile-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户名">
                {{ userInfo?.username || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                {{ userInfo?.email || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="昵称">
                {{ userInfo?.nickname || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="真实姓名">
                {{ userInfo?.realName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                {{ userInfo?.phone || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="性别">
                {{ getGenderText(userInfo?.gender) }}
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">
                {{ formatTime(userInfo?.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="最后登录">
                {{ userInfo?.lastLoginTime ? formatTime(userInfo.lastLoginTime) : '从未登录' }}
              </el-descriptions-item>
              <el-descriptions-item label="登录IP" v-if="userInfo?.lastLoginIp">
                {{ userInfo.lastLoginIp }}
              </el-descriptions-item>
              <el-descriptions-item label="账户状态">
                <el-tag :type="userInfo?.status === 1 ? 'success' : 'warning'">
                  {{ userInfo?.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 安全设置 -->
        <el-card title="安全设置" style="margin-top: 16px;">
          <div class="security-items">
            <div class="security-item">
              <div class="security-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="security-content">
                <div class="security-title">登录密码</div>
                <div class="security-desc">定期更换密码可以提高账户安全性</div>
              </div>
              <el-button type="primary" @click="showPasswordDialog">修改密码</el-button>
            </div>

          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="600px">
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="editForm.realName" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editForm.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="editForm.gender" style="width: 100%">
                <el-option label="未知" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职业" prop="profession">
              <el-input v-model="editForm.profession" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="工作经验" prop="experienceYears">
          <el-input-number
            v-model="editForm.experienceYears"
            :min="0"
            :max="50"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="4"
            placeholder="请输入个人简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>



    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 头像上传对话框 -->
    <el-dialog
      v-model="avatarDialogVisible"
      title="更换头像"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="avatar-upload-section">
        <div class="current-avatar">
          <h4>当前头像</h4>
          <div class="avatar-container">
            <el-avatar :size="80" :src="userInfo?.avatar">
              <span class="avatar-text">{{ getAvatarText() }}</span>
            </el-avatar>
            <el-button
              v-if="userInfo?.avatar"
              type="danger"
              size="small"
              circle
              class="delete-avatar-btn"
              @click="deleteAvatar"
              :loading="deleting"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="upload-area">
          <h4>上传新头像</h4>
          <div class="upload-container">
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleFileSelect"
            />
            <div
              class="upload-box"
              @click="selectFile"
              :class="{ uploading: uploading }"
            >
              <el-icon class="upload-icon" v-if="!uploading">
                <Plus />
              </el-icon>
              <el-icon class="upload-icon" v-else>
                <Loading />
              </el-icon>
              <div class="upload-text">
                {{ uploading ? '上传中...' : '点击选择图片' }}
              </div>
            </div>
          </div>
          <div class="upload-tips">
            <p>支持 JPG、PNG 格式</p>
            <p>文件大小不超过 5MB</p>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="avatarDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAvatarChange" :loading="uploading">
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
import { Lock, Plus, Edit, Camera, Document, Trophy, Clock, Calendar, Loading, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { interviewApi } from '@/api/interview'
import { fileApi } from '@/api/file'
import dayjs from 'dayjs'

const userStore = useUserStore()

// 响应式数据
const userInfo = ref<any>({})
const editDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const avatarDialogVisible = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const statsLoading = ref(false)
const formRef = ref()
const passwordFormRef = ref()

// 统计数据
const stats = ref({
  totalInterviews: 0,
  completedInterviews: 0,
  averageScore: 0,
  totalDuration: 0
})

// 编辑表单
const editForm = reactive({
  nickname: '',
  realName: '',
  email: '',
  phone: '',
  gender: 0,
  profession: '',
  experienceYears: 0,
  bio: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadUserInfo()
  loadUserStats()
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    userInfo.value = userStore.userInfo
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

// 加载用户统计
const loadUserStats = async () => {
  try {
    statsLoading.value = true
    const response = await interviewApi.getInterviewStatistics()
    if (response.data) {
      stats.value.totalInterviews = response.data.totalInterviews || 0
      stats.value.completedInterviews = response.data.completedInterviews || 0
      stats.value.averageScore = response.data.averageScore || 0
      stats.value.totalDuration = response.data.totalDuration || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 如果API调用失败，设置为0
    stats.value.totalInterviews = 0
    stats.value.completedInterviews = 0
    stats.value.averageScore = 0
    stats.value.totalDuration = 0
  } finally {
    statsLoading.value = false
  }
}

// 显示编辑对话框
const showEditDialog = () => {
  Object.assign(editForm, userInfo.value)
  editDialogVisible.value = true
}

// 保存个人资料
const saveProfile = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const response = await userApi.updateProfile(editForm)

    // 检查响应是否成功
    if (response.code === 200 || (response as any).success === true) {
      // 更新本地用户信息
      Object.assign(userInfo.value, editForm)
      // 重新获取用户信息以确保数据同步
      await userStore.getUserInfo()

      ElMessage.success('个人资料更新成功')
      editDialogVisible.value = false
    } else {
      ElMessage.error(response.message || '个人资料更新失败')
    }
  } catch (error: any) {
    console.error('个人资料更新错误:', error)
    ElMessage.error(error.message || '个人资料更新失败')
  } finally {
    saving.value = false
  }
}

// 显示密码对话框
const showPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

// 修改密码
const changePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true

    const response = await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    // 检查响应是否成功
    if (response.code === 200 || (response as any).success === true) {
      ElMessage.success('密码修改成功')
      passwordDialogVisible.value = false
    } else {
      ElMessage.error(response.message || '密码修改失败')
    }
  } catch (error: any) {
    console.error('密码修改错误:', error)
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

// 头像上传相关
const uploading = ref(false)
const deleting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const pendingAvatarUrl = ref<string | null>(null)

// 显示头像对话框
const showAvatarDialog = () => {
  avatarDialogVisible.value = true
}

// 选择文件
const selectFile = () => {
  if (uploading.value) return
  fileInput.value?.click()
}

// 处理文件选择
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 验证文件
  if (!validateFile(file)) return

  // 上传文件
  await uploadAvatar(file)

  // 清空input
  target.value = ''
}

// 验证文件
const validateFile = (file: File) => {
  console.log('验证文件:', {
    fileName: file.name,
    fileType: file.type,
    fileSize: file.size,
    token: userStore.token ? userStore.token.substring(0, 20) + '...' : '未设置',
    isLoggedIn: userStore.isLoggedIn,
    userInfo: userStore.userInfo
  })

  // 检查用户是否已登录
  if (!userStore.isLoggedIn || !userStore.token) {
    ElMessage.error('请先登录后再上传头像')
    return false
  }

  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }

  return true
}

// 上传头像
const uploadAvatar = async (file: File) => {
  try {
    uploading.value = true
    console.log('开始上传头像:', file.name)

    const response = await fileApi.uploadAvatar(file)
    console.log('上传响应:', response)

    if (response.data?.url) {
      // 暂存头像URL，等待用户确认
      pendingAvatarUrl.value = response.data.url
      ElMessage.success('头像上传成功，请点击确定按钮保存')
    } else {
      ElMessage.error('头像上传失败：响应数据异常')
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)

    let errorMessage = '头像上传失败'
    if (error?.response?.status === 403) {
      errorMessage = '权限不足，请重新登录后重试'
    } else if (error?.response?.status === 401) {
      errorMessage = '登录已过期，请重新登录'
    } else if (error?.response?.status === 413) {
      errorMessage = '文件过大，请选择小于5MB的图片'
    } else if (error?.response?.status === 415) {
      errorMessage = '文件格式不支持，请选择JPG、PNG格式的图片'
    } else if (error?.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  } finally {
    uploading.value = false
  }
}

// 确认头像更改
const confirmAvatarChange = async () => {
  if (!pendingAvatarUrl.value) {
    ElMessage.warning('请先上传头像')
    return
  }

  try {
    uploading.value = true

    // 更新后端用户信息
    await userApi.updateUserInfo({ avatar: pendingAvatarUrl.value })

    // 更新本地用户信息
    if (userInfo.value) {
      userInfo.value.avatar = pendingAvatarUrl.value
    }

    // 更新store中的用户信息
    await userStore.getUserInfo()

    ElMessage.success('头像更新成功')
    avatarDialogVisible.value = false
    pendingAvatarUrl.value = null
  } catch (error: any) {
    console.error('头像更新失败:', error)
    ElMessage.error('头像更新失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 删除头像
const deleteAvatar = async () => {
  try {
    await ElMessageBox.confirm('确定要删除当前头像吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    deleting.value = true

    // 更新后端用户信息，将头像设为空
    await userApi.updateUserInfo({ avatar: '' })

    // 更新本地用户信息
    if (userInfo.value) {
      userInfo.value.avatar = ''
    }

    // 更新store中的用户信息
    await userStore.getUserInfo()

    ElMessage.success('头像删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('头像删除失败:', error)
      ElMessage.error('头像删除失败，请重试')
    }
  } finally {
    deleting.value = false
  }
}



// 获取头像文本
const getAvatarText = () => {
  if (userInfo.value?.nickname) {
    return userInfo.value.nickname.charAt(0).toUpperCase()
  }
  if (userInfo.value?.username) {
    return userInfo.value.username.charAt(0).toUpperCase()
  }
  return 'U'
}

// 获取性别文本
const getGenderText = (gender?: number) => {
  const texts = ['未知', '男', '女']
  return texts[gender || 0] || '未知'
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 格式化时长
const formatDuration = (seconds: number) => {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${hours}小时${minutes}分钟`
}
</script>

<style lang="scss" scoped>
.user-profile {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
  
  h1 {
    margin: 0 0 8px 0;
    font-size: 24px;
    color: #333;
  }
  
  p {
    margin: 0;
    color: #666;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-info {
  .el-descriptions {
    margin-top: 16px;
  }
}

.account-actions {
  .el-button {
    margin-bottom: 12px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

// 头像卡片样式
.avatar-card {
  .avatar-section {
    text-align: center;

    .avatar-container {
      position: relative;
      display: inline-block;
      margin-bottom: 16px;

      .user-avatar {
        border: 3px solid #f0f0f0;
        transition: all 0.3s;

        &:hover {
          border-color: #409eff;
        }

        .avatar-text {
          font-size: 48px;
          font-weight: 600;
          color: #409eff;
        }
      }

      .change-avatar-btn {
        position: absolute;
        bottom: -8px;
        right: -8px;
        border-radius: 50%;
        width: 32px;
        height: 32px;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .user-basic-info {
      .username {
        margin: 0 0 8px 0;
        font-size: 20px;
        font-weight: 600;
        color: #333;
      }

      .user-email {
        margin: 0 0 12px 0;
        color: #666;
        font-size: 14px;
      }
    }
  }
}

// 统计网格样式
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  .stat-item {
    display: flex;
    align-items: center;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      background: #e9ecef;
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;

      .el-icon {
        color: white;
        font-size: 18px;
      }
    }

    .stat-content {
      flex: 1;

      .stat-number {
        font-size: 20px;
        font-weight: 600;
        color: #333;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 12px;
        color: #666;
      }
    }
  }
}

// 安全设置样式
.security-items {
  .security-item {
    display: flex;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .security-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;

      .el-icon {
        color: #666;
        font-size: 18px;
      }
    }

    .security-content {
      flex: 1;

      .security-title {
        font-size: 16px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
      }

      .security-desc {
        font-size: 14px;
        color: #666;
      }
    }
  }
}

// 头像上传对话框样式
.avatar-upload-section {
  .current-avatar {
    text-align: center;
    margin-bottom: 24px;

    h4 {
      margin: 0 0 16px 0;
      color: #333;
    }

    .avatar-container {
      position: relative;
      display: inline-block;

      .delete-avatar-btn {
        position: absolute;
        top: -8px;
        right: -8px;
        width: 24px;
        height: 24px;
        min-height: 24px;
        padding: 0;
        background: #f56c6c;
        border-color: #f56c6c;

        &:hover {
          background: #f78989;
          border-color: #f78989;
        }

        .el-icon {
          font-size: 12px;
        }
      }
    }
  }

  .upload-area {
    text-align: center;

    h4 {
      margin: 0 0 16px 0;
      color: #333;
    }

    .upload-container {
      margin-bottom: 16px;

      .upload-box {
        border: 2px dashed #d9d9d9;
        border-radius: 8px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: all 0.3s;
        width: 120px;
        height: 120px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        margin: 0 auto;
        background: #fafafa;

        &:hover {
          border-color: #409eff;
          background: #f0f9ff;
        }

        &.uploading {
          border-color: #409eff;
          background: #f0f9ff;
          cursor: not-allowed;
        }

        .upload-icon {
          font-size: 32px;
          color: #8c939d;
          margin-bottom: 8px;
        }

        .upload-text {
          font-size: 12px;
          color: #666;
        }
      }
    }

    .upload-tips {
      p {
        margin: 4px 0;
        font-size: 12px;
        color: #999;
      }
    }
  }
}

// 对话框footer样式
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
