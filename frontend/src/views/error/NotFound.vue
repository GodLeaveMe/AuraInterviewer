<template>
  <div class="not-found">
    <div class="error-content">
      <div class="error-image">
        <el-icon size="120" color="#409eff">
          <Warning />
        </el-icon>
      </div>
      
      <div class="error-info">
        <h1>404</h1>
        <h2>页面不存在</h2>
        <p>抱歉，您访问的页面不存在或已被删除。</p>
        
        <div class="error-actions">
          <el-button type="primary" @click="goHome">
            返回首页
          </el-button>
          <el-button @click="goBack">
            返回上页
          </el-button>
        </div>
      </div>
    </div>
    
    <div class="suggestions">
      <h3>您可以尝试：</h3>
      <ul>
        <li>检查网址是否正确</li>
        <li>返回上一页重新操作</li>
        <li>访问首页查找您需要的内容</li>
        <li>联系客服获取帮助</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

// 返回首页
const goHome = () => {
  router.push('/dashboard')
}

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.go(-1)
  } else {
    router.push('/dashboard')
  }
}
</script>

<style lang="scss" scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.error-content {
  text-align: center;
  margin-bottom: 40px;
  
  .error-image {
    margin-bottom: 32px;
  }
  
  .error-info {
    h1 {
      font-size: 72px;
      font-weight: 700;
      color: #409eff;
      margin: 0 0 16px 0;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
    }
    
    h2 {
      font-size: 32px;
      color: #333;
      margin: 0 0 16px 0;
    }
    
    p {
      font-size: 16px;
      color: #666;
      margin: 0 0 32px 0;
      line-height: 1.6;
    }
  }
  
  .error-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    flex-wrap: wrap;
  }
}

.suggestions {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  max-width: 400px;
  width: 100%;
  
  h3 {
    margin: 0 0 16px 0;
    color: #333;
    font-size: 18px;
  }
  
  ul {
    margin: 0;
    padding-left: 20px;
    
    li {
      color: #666;
      line-height: 1.8;
      margin-bottom: 8px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

@media (max-width: 768px) {
  .not-found {
    padding: 20px;
  }
  
  .error-info {
    h1 {
      font-size: 48px;
    }
    
    h2 {
      font-size: 24px;
    }
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
}
</style>
