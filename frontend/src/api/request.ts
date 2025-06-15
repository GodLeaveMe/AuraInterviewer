import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { ApiResponse } from '@/types/api'

// 创建axios实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.DEV ? '/api' : 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  },
  // 处理大整数精度丢失问题
  transformResponse: [
    function (data) {
      if (typeof data === 'string') {
        try {
          // 使用正则表达式将大整数转换为字符串，避免精度丢失
          // 只匹配真正的大整数ID字段（15位以上），避免影响其他数字字段如status
          const processedData = data.replace(
            /"(id|sessionId|userId|templateId|questionId|qaId|createBy|updateBy)":\s*(\d{15,})/g,
            '"$1":"$2"'
          )
          return JSON.parse(processedData)
        } catch (e) {
          return data
        }
      }
      return data
    }
  ]
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 添加token
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response

    // 如果是文件下载等特殊响应，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    // 检查业务状态码
    if (data.code === 200) {
      return data
    }
    
    // 处理业务错误
    if (data.code === 401) {
      // 未授权，清除token并跳转到登录页
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error('未授权'))
    }
    
    if (data.code === 403) {
      ElMessage.error('没有权限访问该资源')
      return Promise.reject(new Error('权限不足'))
    }
    
    // 其他业务错误（静默处理）
    console.warn('业务错误:', data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          console.warn('请求参数错误:', data?.message)
          break
        case 401:
          console.warn('登录已过期')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          console.warn('没有权限访问该资源')
          break
        case 404:
          console.warn('请求的资源不存在')
          break
        case 429:
          console.warn('请求过于频繁')
          break
        case 500:
          console.warn('服务器内部错误')
          break
        case 502:
          console.warn('网关错误')
          break
        case 503:
          console.warn('服务暂时不可用')
          break
        default:
          console.warn('网络错误:', data?.message)
      }
    } else if (error.request) {
      console.warn('网络连接失败')
    } else {
      console.warn('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

// 请求方法封装
export const http = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.get(url, config)
  },
  
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.post(url, data, config)
  },
  
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.put(url, data, config)
  },
  
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.delete(url, config)
  },
  
  upload<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data',
        ...config?.headers
      }
    })
  },
  
  download(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse> {
    return request.get(url, {
      ...config,
      responseType: 'blob'
    })
  }
}

export default request
