import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'
import type { LoginVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<Record<string, unknown> | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )
  const role = ref<string>(localStorage.getItem('role') || '')

  function setToken(value: string) {
    token.value = value
    localStorage.setItem('token', value)
  }

  function setUserInfo(info: Record<string, unknown> | null) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function setRole(value: string) {
    role.value = value
    localStorage.setItem('role', value)
  }

  // 登录：调用后端真实接口
  async function login(payload: { username: string; password: string }): Promise<void> {
    const data: LoginVO = await loginApi(payload)
    setToken(data.token)
    setUserInfo({
      userId: data.userId,
      username: data.username,
      realName: data.realName
    })
    setRole(data.role)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('role')
  }

  return {
    token,
    userInfo,
    role,
    setToken,
    setUserInfo,
    setRole,
    login,
    logout
  }
})
