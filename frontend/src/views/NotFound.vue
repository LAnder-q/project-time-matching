<template>
  <div class="not-found">
    <!-- 背景装饰 -->
    <div class="not-found__bg">
      <span class="blob blob--1"></span>
      <span class="blob blob--2"></span>
      <span class="blob blob--3"></span>
      <div class="grid-overlay"></div>
    </div>

    <div class="not-found__content">
      <div class="error-code">
        <span class="error-code__digit">4</span>
        <span class="error-code__digit error-code__digit--icon">
          <svg
            viewBox="0 0 24 24"
            width="64"
            height="64"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="3" y="4" width="18" height="18" rx="3" />
            <path d="M16 2v4M8 2v4M3 10h18" />
            <path d="M9 16l2 2 4-4" />
          </svg>
        </span>
        <span class="error-code__digit">4</span>
      </div>

      <div class="error-label">PAGE NOT FOUND</div>

      <h1 class="error-title">页面不存在</h1>
      <p class="error-desc">
        抱歉，您访问的页面可能已被移除、重命名，或暂时不可用。
      </p>

      <div class="error-actions">
        <el-button type="primary" size="large" class="home-btn" @click="goHome">
          <svg
            viewBox="0 0 24 24"
            width="17"
            height="17"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M3 9.5L12 3l9 6.5V20a2 2 0 0 1-2 2h-4v-7h-6v7H5a2 2 0 0 1-2-2V9.5z" />
          </svg>
          返回首页
        </el-button>
      </div>

      <div class="error-footnote">
        <span class="error-footnote__line"></span>
        <span class="error-footnote__text">Error 404</span>
        <span class="error-footnote__line"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped lang="scss">
.not-found {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
  overflow: hidden;
  padding: 40px 24px;
}

/* ---- 背景装饰 ---- */
.not-found__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;

  &--1 {
    width: 340px;
    height: 340px;
    background: #c7d2fe;
    top: -80px;
    right: -60px;
    animation: float 9s ease-in-out infinite;
  }

  &--2 {
    width: 260px;
    height: 260px;
    background: #e0e7ff;
    bottom: -70px;
    left: -40px;
    animation: float 11s ease-in-out infinite reverse;
  }

  &--3 {
    width: 180px;
    height: 180px;
    background: #ddd6fe;
    top: 40%;
    left: 12%;
    opacity: 0.35;
    animation: float 13s ease-in-out infinite;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(20px, -24px) scale(1.06);
  }
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
}

/* ---- 内容区 ---- */
.not-found__content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 520px;
  animation: rise 0.6s ease both;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- 大号 404 ---- */
.error-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  line-height: 1;

  &__digit {
    font-size: 150px;
    font-weight: 800;
    letter-spacing: -0.05em;
    background: linear-gradient(160deg, var(--pw-primary) 0%, var(--pw-primary-light) 50%, #818cf8 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: none;
    user-select: none;

    &--icon {
      -webkit-text-fill-color: initial;
      color: var(--pw-primary);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 92px;
      height: 92px;
      border-radius: var(--pw-radius-lg);
      background: var(--pw-primary-lightest);
      border: 1px solid #e0e7ff;
      box-shadow: var(--pw-shadow-md);
      animation: wiggle 4s ease-in-out infinite;

      svg {
        width: 56px;
        height: 56px;
      }
    }
  }
}

@keyframes wiggle {
  0%,
  88%,
  100% {
    transform: rotate(0deg);
  }
  91% {
    transform: rotate(-8deg);
  }
  94% {
    transform: rotate(7deg);
  }
  97% {
    transform: rotate(-4deg);
  }
}

.error-label {
  display: inline-block;
  margin-top: 8px;
  padding: 5px 14px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--pw-primary);
  background: var(--pw-primary-lightest);
  border: 1px solid #e0e7ff;
  border-radius: 100px;
}

.error-title {
  margin-top: 22px;
  font-size: 28px;
  font-weight: 700;
  color: var(--pw-text-primary);
  letter-spacing: -0.02em;
}

.error-desc {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--pw-text-secondary);
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
}

/* ---- 操作按钮 ---- */
.error-actions {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 26px !important;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--pw-radius) !important;
  transition: all var(--pw-transition);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 18px rgba(79, 70, 229, 0.28);
  }
}

/* ---- 底部分隔 ---- */
.error-footnote {
  margin-top: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;

  &__line {
    width: 50px;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent,
      var(--pw-border),
      transparent
    );
  }

  &__text {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.16em;
    color: var(--pw-text-secondary);
    text-transform: uppercase;
  }
}

/* ---- 响应式 ---- */
@media (max-width: 600px) {
  .error-code__digit {
    font-size: 96px;

    &--icon {
      width: 66px;
      height: 66px;

      svg {
        width: 40px;
        height: 40px;
      }
    }
  }

  .error-title {
    font-size: 22px;
  }
}
</style>
