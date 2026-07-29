<template>
  <div class="stat-card" :style="{ '--accent': color }">
    <div class="stat-card__top">
      <div class="stat-card__icon-wrap">
        <el-icon :size="22" color="#fff">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-card__value">{{ value }}</div>
    </div>
    <div class="stat-card__label">{{ title }}</div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  title: string
  value: number | string
  icon: Component
  color: string
}>()
</script>

<style scoped lang="scss">
.stat-card {
  background: #fff;
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 20px;
  transition: all var(--pw-transition);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--accent);
    opacity: 0;
    transition: opacity var(--pw-transition);
  }

  &:hover {
    box-shadow: var(--pw-shadow-md);
    transform: translateY(-2px);

    &::before {
      opacity: 1;
    }
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__icon-wrap {
    width: 40px;
    height: 40px;
    border-radius: var(--pw-radius);
    background: var(--accent);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__value {
    font-size: 32px;
    font-weight: 800;
    color: var(--pw-text-primary);
    letter-spacing: -0.03em;
    line-height: 1;
  }

  &__label {
    font-size: 13px;
    color: var(--pw-text-secondary);
    font-weight: 500;
  }
}
</style>
