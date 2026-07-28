import { defineStore } from 'pinia'
import { ref } from 'vue'

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

  // 登录（脚手架内置 mock，接入后端后替换为真实接口调用）
  async function login(payload: { username: string; password: string }): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockToken = 'mock-token-' + Date.now()
        setToken(mockToken)
        setUserInfo({ username: payload.username })
        setRole('admin')
        resolve()
      }, 300)
    })
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
