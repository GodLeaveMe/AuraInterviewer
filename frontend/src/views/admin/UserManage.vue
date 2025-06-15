<template>
  <div class="user-manage">
    <div class="page-header">
      <h1>用户管理</h1>
      <p>管理系统中的所有用户</p>
    </div>

    <!-- 统计卡片 -->
    <div class="statistics">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ statistics.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
            <el-icon class="stat-icon"><User /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ statistics.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
            <el-icon class="stat-icon active"><UserFilled /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ statistics.inactiveUsers }}</div>
              <div class="stat-label">禁用用户</div>
            </div>
            <el-icon class="stat-icon inactive"><Remove /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ statistics.todayNewUsers }}</div>
              <div class="stat-label">今日新增</div>
            </div>
            <el-icon class="stat-icon new"><Plus /></el-icon>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 可视化图表 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="chart-header">
                <span>用户注册趋势</span>
                <el-button size="small" @click="refreshCharts">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </template>
            <div ref="userTrendChartRef" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>用户状态分布</span>
            </template>
            <div ref="userStatusChartRef" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>性别分布</span>
            </template>
            <div ref="genderChartRef" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>活跃度分析</span>
            </template>
            <div ref="activityChartRef" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 搜索和操作栏 -->
    <div class="toolbar">
      <div class="search-section">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索用户名、邮箱或昵称"
          style="width: 300px"
          clearable
          @input="debounceSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select v-model="searchForm.status" placeholder="用户状态" clearable @change="loadUsers">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        
        <el-select v-model="searchForm.gender" placeholder="性别" clearable @change="loadUsers">
          <el-option label="男" :value="1" />
          <el-option label="女" :value="2" />
          <el-option label="未知" :value="0" />
        </el-select>
        
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
      </div>
      
      <div class="action-section">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加用户
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedUsers.length === 0"
          @click="batchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button 
          type="warning"
          :disabled="selectedUsers.length === 0"
          @click="batchToggleStatus"
        >
          <el-icon><Switch /></el-icon>
          批量禁用
        </el-button>
        <el-button @click="exportUsers">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="user-table">
      <el-table
        v-loading="loading"
        :data="users"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="avatar" label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="40">
              {{ row.nickname?.[0] || row.username?.[0] }}
            </el-avatar>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="用户名" min-width="120" sortable="custom" />
        
        <el-table-column prop="email" label="邮箱" min-width="180" sortable="custom" />
        
        <el-table-column prop="nickname" label="昵称" min-width="120" />

        <el-table-column prop="realName" label="真实姓名" min-width="120" />

        <el-table-column prop="phone" label="手机号" min-width="130" />

        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'warning' : 'info'">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="birthday" label="生日" width="120">
          <template #default="{ row }">
            {{ row.birthday || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ getGenderText(row.gender) }}
          </template>
        </el-table-column>

        <el-table-column prop="profession" label="职业" min-width="120" />

        <el-table-column prop="experienceYears" label="工作经验" width="100">
          <template #default="{ row }">
            {{ row.experienceYears }}年
          </template>
        </el-table-column>
        
        <el-table-column prop="lastLoginTime" label="最后登录时间" width="160" sortable="custom">
          <template #default="{ row }">
            {{ row.lastLoginTime ? formatTime(row.lastLoginTime) : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="lastLoginIp" label="最后登录IP" width="140">
          <template #default="{ row }">
            {{ row.lastLoginIp || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="160" sortable="custom">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editUser(row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-dropdown @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button size="small">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="resetPassword">重置密码</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除用户</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="userForm"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="userForm.nickname" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职业" prop="profession">
              <el-input v-model="userForm.profession" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" v-if="!isEdit">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="userForm.password" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="userForm.confirmPassword" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="userForm.gender" style="width: 100%">
                <el-option label="未知" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="userForm.status" style="width: 100%">
                <el-option label="正常" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="userForm.role" style="width: 100%">
                <el-option label="普通用户" :value="0" />
                <el-option label="管理员" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作经验" prop="experienceYears">
              <el-input-number
                v-model="userForm.experienceYears"
                :min="0"
                :max="50"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="userForm.birthday"
            type="date"
            placeholder="选择生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="userForm.bio"
            type="textarea"
            :rows="3"
            placeholder="请输入个人简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, UserFilled, Remove, Plus, Search, Delete, Switch, Download, ArrowDown, Refresh
} from '@element-plus/icons-vue'
import { adminApi, type User as UserType, type UserQueryParams } from '@/api/admin'
import { debounce } from 'lodash-es'
import dayjs from 'dayjs'
import * as XLSX from 'xlsx'
import * as echarts from 'echarts'

// 响应式数据
const users = ref<UserType[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const selectedUsers = ref<UserType[]>([])
const dateRange = ref<string[]>([])

// 图表引用
const userTrendChartRef = ref<HTMLDivElement>()
const userStatusChartRef = ref<HTMLDivElement>()
const genderChartRef = ref<HTMLDivElement>()
const activityChartRef = ref<HTMLDivElement>()

// 统计数据
const statistics = ref({
  totalUsers: 0,
  activeUsers: 0,
  inactiveUsers: 0,
  todayNewUsers: 0
})

// 搜索表单
const searchForm = reactive<UserQueryParams>({
  keyword: '',
  status: undefined,
  gender: undefined,
  sortField: 'createTime',
  sortOrder: 'desc'
})

// 分页
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 用户表单
const userForm = reactive<UserType & { confirmPassword?: string }>({
  username: '',
  email: '',
  nickname: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: '',
  gender: 0,
  status: 1,
  role: 0,
  profession: '',
  experienceYears: 0,
  birthday: '',
  bio: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== userForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '添加用户')

onMounted(async () => {
  loadUsers()
  loadStatistics()
  await nextTick()
  initCharts()
})

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      page: pagination.current,
      size: pagination.size
    }
    const response = await adminApi.getUserList(params)
    users.value = response.data.records
    pagination.total = response.data.total
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await adminApi.getSystemStatistics()
    statistics.value = response.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
    // 设置默认值
    statistics.value = {
      totalUsers: 0,
      activeUsers: 0,
      inactiveUsers: 0,
      todayNewUsers: 0
    }
  }
}

// 防抖搜索
const debounceSearch = debounce(() => {
  pagination.current = 1
  loadUsers()
}, 500)

// 处理日期范围变化
const handleDateChange = (dates: string[]) => {
  if (dates && dates.length === 2) {
    searchForm.startDate = dates[0]
    searchForm.endDate = dates[1]
  } else {
    searchForm.startDate = undefined
    searchForm.endDate = undefined
  }
  loadUsers()
}

// 处理排序变化
const handleSortChange = ({ prop, order }: any) => {
  searchForm.sortField = prop
  searchForm.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  loadUsers()
}

// 处理选择变化
const handleSelectionChange = (selection: UserType[]) => {
  selectedUsers.value = selection
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

// 编辑用户
const editUser = (user: UserType) => {
  isEdit.value = true
  dialogVisible.value = true
  Object.assign(userForm, user)
}

// 重置表单
const resetForm = () => {
  Object.assign(userForm, {
    username: '',
    email: '',
    nickname: '',
    realName: '',
    phone: '',
    password: '',
    confirmPassword: '',
    gender: 0,
    status: 1,
    role: 0,
    profession: '',
    experienceYears: 0,
    birthday: '',
    bio: ''
  })
  formRef.value?.resetFields()
}

// 保存用户
const saveUser = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    
    if (isEdit.value) {
      await adminApi.updateUser(userForm.id!, userForm)
      ElMessage.success('用户更新成功')
    } else {
      await adminApi.createUser(userForm)
      ElMessage.success('用户创建成功')
    }
    
    dialogVisible.value = false
    loadUsers()
    loadStatistics()
    // 刷新图表数据
    await nextTick()
    refreshCharts()
  } catch (error) {
    ElMessage.error(isEdit.value ? '用户更新失败' : '用户创建失败')
  } finally {
    saving.value = false
  }
}

// 切换用户状态
const toggleUserStatus = async (user: UserType) => {
  try {
    const newStatus = user.status === 1 ? 0 : 1
    await adminApi.toggleUserStatus(user.id!, newStatus)
    user.status = newStatus
    ElMessage.success(newStatus === 1 ? '用户已启用' : '用户已禁用')
    loadStatistics()
    // 刷新图表数据
    await nextTick()
    refreshCharts()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 处理下拉菜单命令
const handleCommand = (command: string, user: UserType) => {
  switch (command) {
    case 'resetPassword':
      resetPassword(user)
      break
    case 'delete':
      deleteUser(user)
      break
  }
}

// 重置密码
const resetPassword = async (user: UserType) => {
  try {
    const { value: newPassword } = await ElMessageBox.prompt(
      '请输入新密码',
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'password'
      }
    )
    
    await adminApi.resetUserPassword(user.id!, newPassword)
    ElMessage.success('密码重置成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('密码重置失败')
    }
  }
}

// 删除用户
const deleteUser = async (user: UserType) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await adminApi.deleteUser(user.id!)
    ElMessage.success('用户删除成功')
    loadUsers()
    loadStatistics()
    // 刷新图表数据
    await nextTick()
    refreshCharts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('用户删除失败')
    }
  }
}

// 批量删除
const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedUsers.value.length} 个用户吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const userIds = selectedUsers.value.map(user => user.id!)
    await adminApi.batchDeleteUsers(userIds)
    ElMessage.success('批量删除成功')
    loadUsers()
    loadStatistics()
    // 刷新图表数据
    await nextTick()
    refreshCharts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 批量切换状态
const batchToggleStatus = async () => {
  try {
    const status = await ElMessageBox.confirm(
      '确定要批量修改选中用户的状态吗？',
      '批量状态设置',
      {
        confirmButtonText: '启用',
        cancelButtonText: '禁用',
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    ).then(() => '1').catch((action) => {
      if (action === 'cancel') return '0'
      throw action
    })
    
    const userIds = selectedUsers.value.map(user => user.id!)
    await adminApi.batchUpdateUserStatus(userIds, parseInt(status))
    ElMessage.success('批量状态更新成功')
    loadUsers()
    loadStatistics()
    // 刷新图表数据
    await nextTick()
    refreshCharts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量状态更新失败')
    }
  }
}

// 导出用户数据
const exportUsers = async () => {
  try {
    ElMessage.info('正在导出用户数据...')

    // 获取所有用户数据
    const allUsers = await adminApi.getUserList({
      ...searchForm,
      page: 1,
      size: 10000 // 获取所有数据
    })

    // 准备导出数据
    const exportData = allUsers.data.records.map(user => ({
      '用户ID': user.id,
      '用户名': user.username,
      '邮箱': user.email,
      '手机号': user.phone || '',
      '真实姓名': user.realName || '',
      '性别': getGenderText(user.gender || 0),
      '状态': getStatusText(user.status || 0),
      '注册时间': formatTime(user.createTime || ''),
      '最后登录': user.lastLoginTime ? formatTime(user.lastLoginTime) : '未登录',
      '登录IP': user.lastLoginIp || ''
    }))

    // 创建工作簿
    const ws = XLSX.utils.json_to_sheet(exportData)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '用户数据')

    // 设置列宽
    const colWidths = [
      { wch: 15 }, // 用户ID
      { wch: 20 }, // 用户名
      { wch: 25 }, // 邮箱
      { wch: 15 }, // 手机号
      { wch: 15 }, // 真实姓名
      { wch: 8 },  // 性别
      { wch: 10 }, // 状态
      { wch: 20 }, // 注册时间
      { wch: 20 }, // 最后登录
      { wch: 15 }  // 登录IP
    ]
    ws['!cols'] = colWidths

    // 生成文件名
    const now = new Date()
    const fileName = `用户数据_${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}_${now.getHours().toString().padStart(2, '0')}${now.getMinutes().toString().padStart(2, '0')}.xlsx`

    // 下载文件
    XLSX.writeFile(wb, fileName)

    ElMessage.success(`成功导出 ${exportData.length} 条用户数据`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 获取性别文本
const getGenderText = (gender: number) => {
  const texts = ['未知', '男', '女']
  return texts[gender] || '未知'
}

// 获取状态文本
const getStatusText = (status: number) => {
  return status === 1 ? '正常' : '禁用'
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 初始化图表
const initCharts = () => {
  initUserTrendChart()
  initUserStatusChart()
  initGenderChart()
  initActivityChart()
}

// 刷新图表
const refreshCharts = () => {
  initCharts()
}

// 用户注册趋势图
const initUserTrendChart = async () => {
  if (!userTrendChartRef.value) return

  const chart = echarts.init(userTrendChartRef.value)

  try {
    // 尝试获取真实的趋势数据
    const response = await adminApi.getUserTrendData()
    const trendData = response.data || []

    const dates = trendData.map((item: any) => dayjs(item.date).format('MM-DD'))
    const values = trendData.map((item: any) => item.count)

    const option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        data: values,
        type: 'line',
        smooth: true,
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.2)'
        },
        lineStyle: {
          color: '#409eff'
        },
        itemStyle: {
          color: '#409eff'
        }
      }]
    }

    chart.setOption(option)
  } catch (error) {
    console.error('获取用户趋势数据失败:', error)
    // 如果API不可用，显示空图表
    const option = {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999'
        }
      }
    }
    chart.setOption(option)
  }
}

// 用户状态分布图
const initUserStatusChart = async () => {
  if (!userStatusChartRef.value) return

  const chart = echarts.init(userStatusChartRef.value)

  try {
    // 获取真实的用户状态分布数据
    const response = await adminApi.getUserStatusDistribution()
    const statusData = response.data || []

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [{
        name: '用户状态',
        type: 'pie',
        radius: '50%',
        data: statusData.map((item: any) => ({
          value: item.count,
          name: item.statusName,
          itemStyle: {
            color: item.status === 1 ? '#67c23a' : '#f56c6c'
          }
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    }

    chart.setOption(option)
  } catch (error) {
    console.error('获取用户状态分布数据失败:', error)
    // 如果API不可用，显示空图表
    const option = {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999'
        }
      }
    }
    chart.setOption(option)
  }
}

// 性别分布图
const initGenderChart = async () => {
  if (!genderChartRef.value) return

  const chart = echarts.init(genderChartRef.value)

  try {
    // 尝试获取真实的性别分布数据
    const response = await adminApi.getGenderDistribution()
    const genderData = response.data || []

    const option = {
      tooltip: {
        trigger: 'item'
      },
      series: [{
        name: '性别分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '30',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: genderData.map((item: any) => ({
          value: item.count,
          name: item.gender === 1 ? '男性' : item.gender === 2 ? '女性' : '未知',
          itemStyle: {
            color: item.gender === 1 ? '#409eff' : item.gender === 2 ? '#f56c6c' : '#909399'
          }
        }))
      }]
    }

    chart.setOption(option)
  } catch (error) {
    console.error('获取性别分布数据失败:', error)
    // 如果API不可用，显示空图表
    const option = {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999'
        }
      }
    }
    chart.setOption(option)
  }
}

// 活跃度分析图
const initActivityChart = async () => {
  if (!activityChartRef.value) return

  const chart = echarts.init(activityChartRef.value)

  try {
    // 尝试获取真实的活跃度数据
    const response = await adminApi.getActivityData()
    const activityData = response.data || {}

    const option = {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['登录用户数']
      },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '登录用户数',
          type: 'bar',
          data: activityData.weeklyData || [0, 0, 0, 0, 0, 0, 0],
          itemStyle: { color: '#409eff' }
        }
      ]
    }

    chart.setOption(option)
  } catch (error) {
    console.error('获取活跃度数据失败:', error)
    // 如果API不可用，显示空图表
    const option = {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999'
        }
      }
    }
    chart.setOption(option)
  }
}
</script>

<style lang="scss" scoped>
.user-manage {
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

.statistics {
  margin-bottom: 24px;

  .stat-card {
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .stat-content {
      .stat-number {
        font-size: 28px;
        font-weight: 600;
        color: #409eff;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        color: #666;
      }
    }

    .stat-icon {
      position: absolute;
      right: 20px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 32px;
      color: #409eff;
      opacity: 0.3;

      &.active {
        color: #67c23a;
      }

      &.inactive {
        color: #f56c6c;
      }

      &.new {
        color: #e6a23c;
      }
    }
  }
}

.charts-section {
  margin-bottom: 24px;

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  
  .search-section {
    display: flex;
    gap: 12px;
    align-items: center;
  }
  
  .action-section {
    display: flex;
    gap: 8px;
  }
}

.user-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 16px;
    
    .search-section {
      flex-wrap: wrap;
    }
  }
  
  .statistics {
    .el-col {
      margin-bottom: 16px;
    }
  }
}
</style>
