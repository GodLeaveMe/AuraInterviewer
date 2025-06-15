<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 暂时注释掉可能有问题的代码
// import { onMounted } from 'vue'
// import { useUserStore } from '@/stores/user'

// const userStore = useUserStore()

// onMounted(() => {
//   // 初始化用户信息
//   userStore.initUserInfo()
// })
</script>

<style lang="scss">
#app {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.el-message-box {
  border-radius: 8px;
}

.el-dialog {
  border-radius: 8px;
}

.el-card {
  border-radius: 8px;
}

.el-button {
  border-radius: 6px;
}

.el-input__wrapper {
  border-radius: 6px;
}

.el-textarea__inner {
  border-radius: 6px;
}

// 滚动条样式
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

// 暗色主题滚动条
.dark {
  ::-webkit-scrollbar-track {
    background: #2c2c2c;
  }

  ::-webkit-scrollbar-thumb {
    background: #6c6c6c;
  }

  ::-webkit-scrollbar-thumb:hover {
    background: #8c8c8c;
  }
}
</style>
