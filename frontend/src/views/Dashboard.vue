<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <StatCard title="总人数" :value="stats.personnelCount" :icon="User" color="#4f46e5" />
      <StatCard title="项目数" :value="stats.projectCount" :icon="Folder" color="#059669" />
      <StatCard title="分配数" :value="stats.assignmentCount" :icon="Connection" color="#d97706" />
      <StatCard title="冲突数" :value="stats.conflictCount" :icon="Warning" color="#dc2626" />
    </div>

    <!-- 欢迎区 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h2 class="welcome-title">欢迎使用资源匹配管理系统</h2>
        <p class="welcome-desc">
          通过本系统可以管理人员信息、项目信息，进行人员与项目的时间分配，并自动检测时间冲突，辅助管理者合理排期。
        </p>
      </div>
      <div class="welcome-decoration">
        <svg width="120" height="120" viewBox="0 0 120 120" fill="none">
          <circle cx="60" cy="60" r="56" stroke="#eef2ff" stroke-width="2" stroke-dasharray="8 4" />
          <circle cx="60" cy="60" r="36" fill="#eef2ff" />
          <path d="M44 54h32M44 60h32M44 66h20" stroke="#4f46e5" stroke-width="2.5" stroke-linecap="round" />
          <circle cx="72" cy="66" r="6" fill="#818cf8" stroke="#fff" stroke-width="2" />
        </svg>
      </div>
    </div>

    <!-- 报表导出 -->
    <div class="export-section">
      <div class="section-header">
        <div>
          <h3 class="section-title">报表导出</h3>
          <p class="section-desc">点击下方按钮可导出对应的报表文件，支持 Excel 和 PDF 两种格式</p>
        </div>
      </div>

      <div class="export-grid">
        <div class="export-card">
          <div class="export-card__icon export-card__icon--primary">
            <el-icon :size="22"><Connection /></el-icon>
          </div>
          <div class="export-card__info">
            <div class="export-card__name">人员时间分配表</div>
            <div class="export-card__desc">人员与项目的时间分配明细</div>
          </div>
          <el-dropdown @command="(cmd: string) => exportReport('assignment', cmd as 'xlsx' | 'pdf')">
            <el-button type="primary" :icon="Download" size="small">
              导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item>
                <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="export-card">
          <div class="export-card__icon export-card__icon--danger">
            <el-icon :size="22"><Warning /></el-icon>
          </div>
          <div class="export-card__info">
            <div class="export-card__name">冲突报表</div>
            <div class="export-card__desc">人员时间冲突详情汇总</div>
          </div>
          <el-dropdown @command="(cmd: string) => exportReport('conflict', cmd as 'xlsx' | 'pdf')">
            <el-button type="danger" :icon="Download" size="small">
              导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item>
                <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="export-card">
          <div class="export-card__icon export-card__icon--success">
            <el-icon :size="22"><DataAnalysis /></el-icon>
          </div>
          <div class="export-card__info">
            <div class="export-card__name">人员利用率统计</div>
            <div class="export-card__desc">人员工时利用率分析报告</div>
          </div>
          <el-dropdown @command="(cmd: string) => exportReport('utilization', cmd as 'xlsx' | 'pdf')">
            <el-button type="success" :icon="Download" size="small">
              导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item>
                <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { User, Folder, Connection, Warning, Download, ArrowDown, DataAnalysis } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getPersonnelPage } from '@/api/personnel'
import { getProjectPage } from '@/api/project'
import { getAssignmentList } from '@/api/assignment'
import { detectAllConflicts } from '@/api/conflict'
import { exportReport } from '@/api/report'

const stats = reactive({
  personnelCount: 0,
  projectCount: 0,
  assignmentCount: 0,
  conflictCount: 0
})

async function loadStats() {
  try {
    const res = await getPersonnelPage({ pageNum: 1, pageSize: 1 })
    stats.personnelCount = res.total
  } catch {
    stats.personnelCount = 0
  }

  try {
    const res = await getProjectPage({ pageNum: 1, pageSize: 1 })
    stats.projectCount = res.total
  } catch {
    stats.projectCount = 0
  }

  try {
    const res = await getAssignmentList()
    stats.assignmentCount = res.length
  } catch {
    stats.assignmentCount = 0
  }

  try {
    const res = await detectAllConflicts()
    let count = 0
    res.forEach((item) => (count += item.conflicts.length))
    stats.conflictCount = count
  } catch {
    stats.conflictCount = 0
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---- 统计卡片网格 ---- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* ---- 欢迎区 ---- */
.welcome-section {
  background: #fff;
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, var(--pw-primary-lightest) 0%, transparent 60%);
    pointer-events: none;
  }

  .welcome-content {
    position: relative;
    z-index: 1;

    .welcome-title {
      font-size: 22px;
      font-weight: 700;
      color: var(--pw-text-primary);
      letter-spacing: -0.02em;
      margin-bottom: 8px;
    }

    .welcome-desc {
      font-size: 14px;
      color: var(--pw-text-regular);
      line-height: 1.7;
      max-width: 600px;
    }
  }

  .welcome-decoration {
    flex-shrink: 0;
    position: relative;
    z-index: 1;
  }
}

/* ---- 报表导出 ---- */
.export-section {
  background: #fff;
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 24px;

  .section-header {
    margin-bottom: 20px;

    .section-title {
      font-size: 17px;
      font-weight: 700;
      color: var(--pw-text-primary);
      margin-bottom: 4px;
    }

    .section-desc {
      font-size: 13px;
      color: var(--pw-text-secondary);
    }
  }
}

.export-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.export-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--pw-border-light);
  border-radius: var(--pw-radius);
  transition: all var(--pw-transition);

  &:hover {
    border-color: var(--pw-primary-lighter);
    background: var(--pw-bg-hover);
  }

  &__icon {
    width: 44px;
    height: 44px;
    border-radius: var(--pw-radius);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &--primary {
      background: var(--pw-primary-lightest);
      color: var(--pw-primary);
    }

    &--danger {
      background: #fef2f2;
      color: var(--pw-danger);
    }

    &--success {
      background: #ecfdf5;
      color: var(--pw-success);
    }
  }

  &__info {
    flex: 1;
    min-width: 0;

    .export-card__name {
      font-size: 14px;
      font-weight: 600;
      color: var(--pw-text-primary);
      margin-bottom: 2px;
    }

    .export-card__desc {
      font-size: 12px;
      color: var(--pw-text-secondary);
    }
  }
}

/* ---- 响应式 ---- */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .welcome-decoration {
    display: none;
  }
}
</style>
