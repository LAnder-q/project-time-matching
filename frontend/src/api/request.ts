import axios from 'axios'
import type {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosResponse
} from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：添加 token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token') || ''
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 Result 返回体
service.interceptors.response.use(
  (response: AxiosResponse<Result<any>>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    // 直接返回 data 字段，方便业务层使用
    return res.data
  },
  (error) => {
    // 401 未认证：清除 token 并跳转登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('role')
      ElMessage.error('登录已过期，请重新登录')
      // 避免在登录页重复跳转
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    // 403 无权限：提示权限不足
    if (error.response?.status === 403) {
      const msg = error.response?.data?.message || '权限不足，无法执行此操作'
      ElMessage.error(msg)
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

// 类型化的请求辅助方法
export const http = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  }
}

export default service
