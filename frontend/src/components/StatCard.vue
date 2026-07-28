<template>
  <el-card class="stat-card" shadow="hover">
    <div class="stat-card__content">
      <div class="stat-card__icon" :style="{ backgroundColor: color }">
        <el-icon :size="28" color="#fff">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-card__info">
        <div class="stat-card__title">{{ title }}</div>
        <div class="stat-card__value" :style="{ color }">{{ value }}</div>
      </div>
    </div>
  </el-card>
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
  &__content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__title {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }
}
</style>
