<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <StatCard title="总人数" :value="stats.personnelCount" :icon="User" color="#409eff" />
      </el-col>
      <el-col :span="6">
        <StatCard title="项目数" :value="stats.projectCount" :icon="Folder" color="#67c23a" />
      </el-col>
      <el-col :span="6">
        <StatCard
          title="分配数"
          :value="stats.assignmentCount"
          :icon="Connection"
          color="#e6a23c"
        />
      </el-col>
      <el-col :span="6">
        <StatCard title="冲突数" :value="stats.conflictCount" :icon="Warning" color="#f56c6c" />
      </el-col>
    </el-row>

    <el-card class="welcome-card" shadow="never">
      <div class="welcome">
        <h2>欢迎使用人员-项目时间匹配管理工具</h2>
        <p>
          通过本系统可以管理人员信息、项目信息，进行人员与项目的时间分配，并自动检测时间冲突，辅助管理者合理排期。
        </p>
      </div>
    </el-card>

    <el-card class="export-card" shadow="never">
      <div class="export-section">
        <h3>报表导出</h3>
        <p class="export-tip">点击下方按钮可导出对应的报表文件。</p>
        <div class="export-buttons">
          <el-button type="primary" :icon="Download" @click="exportReport('assignment')">
            导出人员时间分配表
          </el-button>
          <el-button type="danger" :icon="Download" @click="exportReport('conflict')">
            导出冲突报表
          </el-button>
          <el-button type="success" :icon="Download" @click="exportReport('utilization')">
            导出人员利用率统计
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { User, Folder, Connection, Warning, Download } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getPersonnelPage } from '@/api/personnel'
import { getProjectPage } from '@/api/project'
import { getAssignmentList } from '@/api/assignment'
import { detectAllConflicts } from '@/api/conflict'

// 后端报表导出接口基础地址
const REPORT_BASE_URL = 'http://localhost:8080/api/report/export'

function exportReport(type: 'assignment' | 'conflict' | 'utilization') {
  window.open(`${REPORT_BASE_URL}/${type}`, '_blank')
}

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
  .welcome-card {
    margin-top: 20px;

    .welcome {
      h2 {
        font-size: 22px;
        color: #303133;
        margin-bottom: 12px;
      }

      p {
        color: #606266;
        line-height: 1.8;
      }
    }
  }

  .export-card {
    margin-top: 20px;

    .export-section {
      h3 {
        font-size: 18px;
        color: #303133;
        margin: 0 0 8px;
      }

      .export-tip {
        color: #909399;
        font-size: 13px;
        margin: 0 0 16px;
      }

      .export-buttons {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }
    }
  }
}
</style>
