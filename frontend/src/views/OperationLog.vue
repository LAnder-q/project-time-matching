<template>
  <div class="operation-log-page">
    <!-- 页面头部 -->
    <header class="page-head">
      <div class="page-head__lead">
        <h1 class="page-head__title">操作日志</h1>
        <p class="page-head__desc">
          全部分配的调整历史，可按人员、操作人、操作类型筛选；被替换的原负责人也能在这里查到记录
        </p>
      </div>
      <div class="page-head__aside">
        <el-dropdown @command="(cmd: string) => handleExport(cmd as 'xlsx' | 'pdf')">
          <el-button type="success" :icon="Download">
            导出<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item>
              <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </header>

    <!-- 筛选工具栏 -->
    <section class="filter-bar">
      <el-select
        v-model="filterPersonnelId"
        placeholder="按涉及人员筛选"
        clearable
        filterable
        class="filter-bar__item"
        @change="handleSearch"
      >
        <el-option
          v-for="p in personnelOptions"
          :key="p.id"
          :label="`${p.name} (${p.empNo})`"
          :value="p.id!"
        />
      </el-select>

      <el-select
        v-model="filterAction"
        placeholder="按操作类型筛选"
        clearable
        class="filter-bar__item"
        @change="handleSearch"
      >
        <el-option label="新增" value="CREATE" />
        <el-option label="修改" value="UPDATE" />
        <el-option label="删除" value="DELETE" />
      </el-select>

      <el-input
        v-model="filterOperator"
        placeholder="按操作人搜索"
        clearable
        class="filter-bar__item"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </section>

    <!-- 日志表格 -->
    <section class="table-panel">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logActionTagType(row.action)" size="small">
              {{ logActionText(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="180">
          <template #default="{ row }">{{ row.operateTime?.replace('T', ' ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" prop="operator" width="120">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="版本号" width="90" align="center">
          <template #default="{ row }">{{ row.version }}</template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">{{ row.change }}</template>
        </el-table-column>
        <el-table-column label="分配ID" prop="entityId" width="100" align="center" />
      </el-table>

      <div class="table-panel__footer">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Download, ArrowDown } from '@element-plus/icons-vue'
import { getOperationLogPage, exportOperationLog } from '@/api/operationLog'
import { getPersonnelAll } from '@/api/personnel'
import { getProjectAll } from '@/api/project'
import type { Personnel, Project } from '@/types'
import type { OperationLog } from '@/api/assignment'
import {
  parseAssignJson,
  describeAssignChange,
  assignLogVersion,
  logActionText,
  logActionTagType
} from '@/utils/assignmentLog'

interface LogRow {
  id: number
  action: string
  operator: string
  operateTime: string
  version: number | string
  change: string
  entityId: number
}

const loading = ref(false)
const tableData = ref<LogRow[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const personnelOptions = ref<Personnel[]>([])
const projectOptions = ref<Project[]>([])
const filterPersonnelId = ref<number | undefined>(undefined)
const filterAction = ref<string | undefined>(undefined)
const filterOperator = ref('')

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleExport(format: 'xlsx' | 'pdf') {
  exportOperationLog(format, {
    operator: filterOperator.value.trim() || undefined,
    action: filterAction.value,
    personnelId: filterPersonnelId.value
  })
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOperationLogPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      personnelId: filterPersonnelId.value,
      action: filterAction.value,
      operator: filterOperator.value.trim() || undefined
    })
    const resolver = {
      personnel: (id: unknown) => personnelOptions.value.find((p) => p.id === id)?.name || '',
      project: (id: unknown) => projectOptions.value.find((p) => p.id === id)?.name || ''
    }
    tableData.value = res.list.map((log: OperationLog) => {
      const oldObj = parseAssignJson(log.oldValue)
      const newObj = parseAssignJson(log.newValue)
      return {
        id: log.id,
        action: log.action,
        operator: log.operator || '-',
        operateTime: log.operateTime || '',
        version: assignLogVersion(log.action, oldObj, newObj),
        change: describeAssignChange(log.action, oldObj, newObj, resolver),
        entityId: log.entityId
      }
    })
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    personnelOptions.value = await getPersonnelAll()
  } catch {
    personnelOptions.value = []
  }
  try {
    projectOptions.value = await getProjectAll()
  } catch {
    projectOptions.value = []
  }
  loadData()
})
</script>

<style scoped lang="scss">
.operation-log-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 2px;

  &__lead {
    min-width: 0;
  }

  &__title {
    font-size: 22px;
    font-weight: 700;
    color: var(--pw-text-primary);
    letter-spacing: -0.02em;
    line-height: 1.2;
  }

  &__desc {
    margin-top: 6px;
    font-size: 13px;
    color: var(--pw-text-secondary);
    line-height: 1.5;
    max-width: 640px;
  }

  &__aside {
    flex-shrink: 0;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 14px 18px;
  box-shadow: var(--pw-shadow-sm);

  &__item {
    width: 220px;
  }
}

.table-panel {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);
  padding: 14px 16px;

  &__footer {
    display: flex;
    justify-content: flex-end;
    padding-top: 14px;
  }
}
</style>
