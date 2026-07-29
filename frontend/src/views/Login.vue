<template>
  <div class="login-shell">
    <!-- 左侧品牌展示区 -->
    <div class="brand-panel">
      <div class="brand-bg-pattern" />
      <div class="brand-content">
        <div class="brand-logo">
          <svg width="44" height="44" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="rgba(255,255,255,0.15)" />
            <path d="M8 10h12M8 14h12M8 18h7" stroke="#fff" stroke-width="2" stroke-linecap="round" />
            <circle cx="20" cy="18" r="3" fill="#818cf8" stroke="#fff" stroke-width="1.5" />
          </svg>
        </div>
        <h1 class="brand-heading">人员-项目<br>时间匹配管理系统</h1>
        <p class="brand-desc">
          高效管理人员与项目的时间分配，智能检测冲突，<br>辅助管理者合理排期，提升团队资源利用率。
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M6 5h8M6 10h8M6 15h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
            </div>
            <span>智能冲突检测</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <rect x="3" y="4" width="14" height="13" rx="2" stroke="currentColor" stroke-width="1.8" />
                <path d="M3 8h14M7 2v4M13 2v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              </svg>
            </div>
            <span>日历可视化</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M3 17l4-4M7 13l3 3 7-7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                <circle cx="17" cy="6" r="2" stroke="currentColor" stroke-width="1.8" />
              </svg>
            </div>
            <span>自动化报表</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请输入账号密码登录系统</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>人员-项目时间匹配管理工具</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.push(redirect)
    } catch {
      // 错误信息已由 axios 响应拦截器统一弹出
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-shell {
  display: flex;
  height: 100%;
}

/* ---- 左侧品牌区 ---- */
.brand-panel {
  flex: 1;
  position: relative;
  background: linear-gradient(135deg, #4338ca 0%, #4f46e5 50%, #6366f1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .brand-bg-pattern {
    position: absolute;
    inset: 0;
    background-image:
      radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.08) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(129, 140, 248, 0.15) 0%, transparent 50%);
  }

  .brand-content {
    position: relative;
    z-index: 1;
    max-width: 420px;
    padding: 48px;
    color: #fff;
  }

  .brand-logo {
    margin-bottom: 32px;
    opacity: 0.9;
  }

  .brand-heading {
    font-size: 32px;
    font-weight: 800;
    line-height: 1.3;
    letter-spacing: -0.02em;
    margin-bottom: 16px;
  }

  .brand-desc {
    font-size: 14px;
    line-height: 1.8;
    color: rgba(255, 255, 255, 0.75);
    margin-bottom: 40px;
  }

  .brand-features {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .feature-icon {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.12);
        display: flex;
        align-items: center;
        justify-content: center;
        color: rgba(255, 255, 255, 0.9);
        flex-shrink: 0;
      }

      span {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.85);
        font-weight: 500;
      }
    }
  }
}

/* ---- 右侧表单区 ---- */
.form-panel {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;

  .form-wrapper {
    width: 100%;
    max-width: 340px;
    padding: 48px 24px;
  }

  .form-header {
    margin-bottom: 32px;

    .form-title {
      font-size: 26px;
      font-weight: 700;
      color: var(--pw-text-primary);
      letter-spacing: -0.02em;
      margin-bottom: 8px;
    }

    .form-subtitle {
      font-size: 14px;
      color: var(--pw-text-secondary);
    }
  }

  .login-btn {
    width: 100%;
    height: 44px;
    font-size: 15px;
    font-weight: 600;
    letter-spacing: 0.05em;
  }

  .form-footer {
    margin-top: 32px;
    text-align: center;

    span {
      font-size: 12px;
      color: var(--pw-text-placeholder);
    }
  }
}

/* ---- 响应式 ---- */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    width: 100%;
  }
}
</style>
