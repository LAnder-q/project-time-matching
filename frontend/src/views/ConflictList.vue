<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">冲突清单</span>
        <div class="toolbar-actions">
          <el-button type="success" :icon="Download" @click="exportConflictReport">
            导出冲突报表
          </el-button>
          <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && conflictList.length === 0" description="暂无冲突" />

      <div v-loading="loading">
        <el-card
          v-for="item in conflictList"
          :key="item.personnelId"
          class="conflict-person-card"
          shadow="hover"
        >
          <template #header>
            <div class="person-header">
              <el-icon><User /></el-icon>
              <span class="person-name">{{ item.personnelName }}</span>
              <el-tag type="info">工号：{{ item.empNo }}</el-tag>
              <el-tag type="danger">冲突 {{ item.conflicts.length }} 项</el-tag>
              <el-button
                type="primary"
                link
                class="view-calendar"
                @click="goCalendar(item.personnelId)"
              >
                查看日历
              </el-button>
            </div>
          </template>

          <el-table :data="item.conflicts" border size="small">
            <el-table-column label="项目1" min-width="160">
              <template #default="{ row }">{{ row.projectName1 }}</template>
            </el-table-column>
            <el-table-column label="项目2" min-width="160">
              <template #default="{ row }">{{ row.projectName2 }}</template>
            </el-table-column>
            <el-table-column label="重叠开始" width="130">
              <template #default="{ row }">{{ row.overlapStart }}</template>
            </el-table-column>
            <el-table-column label="重叠结束" width="130">
              <template #default="{ row }">{{ row.overlapEnd }}</template>
            </el-table-column>
            <el-table-column label="严重程度" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)">
                  {{ severityText(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <!-- 调优建议区域 -->
          <div class="suggestion-area">
            <el-button
              type="warning"
              plain
              :icon="MagicStick"
              @click="toggleSuggestion(item.personnelId)"
            >
              {{ isExpanded(item.personnelId) ? '收起调优建议' : '查看调优建议' }}
            </el-button>

            <el-collapse-transition>
              <div v-show="isExpanded(item.personnelId)" class="suggestion-panel">
                <div v-loading="suggestionLoading" class="suggestion-content">
                  <el-empty
                    v-if="!suggestionLoading && getPersonnelSuggestions(item.personnelId).length === 0"
                    description="暂无调优建议"
                    :image-size="60"
                  />

                  <div
                    v-for="(sug, idx) in getPersonnelSuggestions(item.personnelId)"
                    :key="idx"
                    class="suggestion-item"
                  >
                    <div class="suggestion-header">
                      <el-tag :type="suggestionTagType(sug.suggestionType)">
                        {{ suggestionTypeText(sug.suggestionType) }}
                      </el-tag>
                      <span class="suggestion-project">涉及项目：{{ sug.projectName }}</span>
                    </div>

                    <p class="suggestion-desc">{{ sug.description }}</p>

                    <!-- 替换人员：展示替代候选人列表 -->
                    <template v-if="sug.suggestionType === 'REPLACE_PERSONNEL'">
                      <el-table
                        :data="sug.candidates || []"
                        border
                        size="small"
                        class="candidate-table"
                      >
                        <el-table-column label="姓名" prop="name" width="100" />
                        <el-table-column label="工号" prop="empNo" width="110" />
                        <el-table-column label="岗位" prop="position" width="120" />
                        <el-table-column
                          label="技能"
                          prop="skills"
                          min-width="160"
                          show-overflow-tooltip
                        />
                        <el-table-column label="匹配度" width="170" align="center">
                          <template #default="{ row }">
                            <el-progress
                              :percentage="matchPercentage(row.matchScore)"
                              :color="matchScoreColor(row.matchScore)"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="推荐理由"
                          prop="reason"
                          min-width="200"
                          show-overflow-tooltip
                        />
                      </el-table>
                    </template>

                    <!-- 调整工时：展示调整建议文字 -->
                    <template v-else-if="sug.suggestionType === 'ADJUST_HOURS'">
                      <el-alert
                        :title="sug.adjustAdvice || '暂无调整建议'"
                        type="info"
                        :closable="false"
                        show-icon
                      />
                    </template>
                  </div>
                </div>
              </div>
            </el-collapse-transition>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Download, MagicStick } from '@element-plus/icons-vue'
import { detectAllConflicts, getSuggestions } from '@/api/conflict'
import type { ConflictResult } from '@/types'
import type { ConflictSuggestion } from '@/api/conflict'

type TagType = '' | 'success' | 'warning' | 'info' | 'danger' | 'primary'

// 后端报表导出接口地址
const EXPORT_CONFLICT_URL = 'http://localhost:8080/api/report/export/conflict'

const router = useRouter()
const loading = ref(false)
const conflictList = ref<ConflictResult[]>([])

// 调优建议相关状态
const suggestionLoading = ref(false)
const suggestionsLoaded = ref(false)
const allSuggestions = ref<ConflictSuggestion[]>([])
// 各人员建议面板的展开状态，key 为 personnelId
const expanded = reactive<Record<number, boolean>>({})

async function loadData() {
  loading.value = true
  try {
    conflictList.value = await detectAllConflicts()
  } catch {
    conflictList.value = []
  } finally {
    loading.value = false
  }
}

async function loadSuggestions() {
  suggestionLoading.value = true
  try {
    allSuggestions.value = await getSuggestions()
  } catch {
    allSuggestions.value = []
  } finally {
    suggestionLoading.value = false
  }
}

function isExpanded(personnelId: number): boolean {
  return !!expanded[personnelId]
}

function toggleSuggestion(personnelId: number) {
  expanded[personnelId] = !expanded[personnelId]
  // 首次展开时拉取调优建议
  if (expanded[personnelId] && !suggestionsLoaded.value) {
    suggestionsLoaded.value = true
    loadSuggestions()
  }
}

// 过滤出当前人员的调优建议
function getPersonnelSuggestions(personnelId: number): ConflictSuggestion[] {
  return allSuggestions.value.filter((s) => s.personnelId === personnelId)
}

function exportConflictReport() {
  window.open(EXPORT_CONFLICT_URL, '_blank')
}

function goCalendar(personnelId: number) {
  router.push({ path: '/calendar', query: { personnelId: String(personnelId) } })
}

function severityType(severity: string): TagType {
  const s = severity.toLowerCase()
  if (s.includes('high') || s.includes('严重')) return 'danger'
  if (s.includes('mid') || s.includes('medium') || s.includes('中')) return 'warning'
  if (s.includes('low') || s.includes('低')) return 'info'
  return 'warning'
}

function severityText(severity: string): string {
  const s = severity.toLowerCase()
  if (s.includes('high') || s.includes('严重')) return '严重'
  if (s.includes('mid') || s.includes('medium') || s.includes('中')) return '中等'
  if (s.includes('low') || s.includes('低')) return '轻微'
  return severity
}

function suggestionTypeText(type: ConflictSuggestion['suggestionType']): string {
  return type === 'REPLACE_PERSONNEL' ? '替换人员' : '调整工时'
}

function suggestionTagType(type: ConflictSuggestion['suggestionType']): TagType {
  return type === 'REPLACE_PERSONNEL' ? 'warning' : 'success'
}

// matchScore 可能是 0~1 的小数，也可能是 0~100 的整数，统一转换为百分比
function matchPercentage(score: number): number {
  const pct = score > 1 ? score : score * 100
  return Math.min(100, Math.max(0, Math.round(pct)))
}

function matchScoreColor(score: number): string {
  const normalized = score > 1 ? score / 100 : score
  if (normalized >= 0.8) return '#67c23a'
  if (normalized >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-size: 16px;
      font-weight: 600;
    }

    .toolbar-actions {
      display: flex;
      gap: 10px;
    }
  }

  .conflict-person-card {
    margin-bottom: 16px;

    .person-header {
      display: flex;
      align-items: center;
      gap: 10px;

      .person-name {
        font-size: 16px;
        font-weight: 600;
      }

      .view-calendar {
        margin-left: auto;
      }
    }

    .suggestion-area {
      margin-top: 16px;

      .suggestion-panel {
        margin-top: 12px;
        padding: 16px;
        background-color: #fafafa;
        border: 1px dashed #dcdfe6;
        border-radius: 6px;

        .suggestion-content {
          min-height: 40px;
        }

        .suggestion-item {
          padding: 12px;
          margin-bottom: 12px;
          background-color: #fff;
          border: 1px solid #ebeef5;
          border-radius: 6px;

          &:last-child {
            margin-bottom: 0;
          }

          .suggestion-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;

            .suggestion-project {
              font-size: 14px;
              color: #303133;
            }
          }

          .suggestion-desc {
            margin: 0 0 10px;
            font-size: 14px;
            color: #606266;
            line-height: 1.6;
          }

          .candidate-table {
            margin-top: 4px;
          }
        }
      }
    }
  }
}
</style>
