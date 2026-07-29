import { http } from './request'
import type { LoginVO } from '@/types'

/**
 * 用户登录
 */
export function login(data: { username: string; password: string }) {
  return http.post<LoginVO>('/auth/login', data)
}

/**
 * 获取当前登录用户信息
 */
export function getUserInfo() {
  return http.get<LoginVO>('/auth/info')
}
