<template>
  <div class="conflict-page">
    <!-- 页面头部 -->
    <header class="page-head">
      <div class="page-head__lead">
        <h1 class="page-head__title">冲突清单</h1>
        <p class="page-head__desc">自动检测人员跨项目的时间冲突，并提供替换人员与调整工时等调优建议</p>
      </div>
      <div class="page-head__aside">
        <el-button type="primary" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="success" :icon="Download" @click="exportConflictReport">
          导出冲突报表
        </el-button>
      </div>
    </header>

    <!-- 筛选工具栏 -->
    <div class="filter-bar">
      <el-tree-select
        v-model="filterDeptId"
        :data="deptTreeData"
        :props="{ label: 'name', value: 'id', children: 'children' }"
        check-strictly
        clearable
        placeholder="按部门筛选"
        class="filter-bar__dept"
        @change="onFilterChange"
      />
      <span class="filter-bar__hint">
        共检测到 {{ conflictList.length }} 位人员存在冲突
      </span>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && conflictList.length === 0" class="empty-wrap">
      <el-empty description="暂无冲突" />
    </div>

    <!-- 冲突人员列表 -->
    <div v-loading="loading" class="conflict-list">
      <article
        v-for="item in pagedConflictList"
        :key="item.personnelId"
        class="conflict-card"
      >
        <!-- 卡片头部 -->
        <div class="conflict-card__head">
          <div class="conflict-card__identity">
            <span class="conflict-card__avatar">
              <el-icon><User /></el-icon>
            </span>
            <div class="conflict-card__meta">
              <div class="conflict-card__name">{{ item.personnelName }}</div>
              <div class="conflict-card__sub">
                工号 {{ item.empNo }}
                <span v-if="getDeptNameByPersonnelId(item.personnelId)" class="conflict-card__dept">
                  · {{ getDeptNameByPersonnelId(item.personnelId) }}
                </span>
              </div>
            </div>
          </div>
          <div class="conflict-card__badges">
            <span class="conflict-badge conflict-badge--danger">
              冲突 {{ item.conflicts.length }} 项
            </span>
            <el-button type="primary" link @click="goCalendar(item.personnelId)">
              查看日历
            </el-button>
          </div>
        </div>

        <!-- 冲突明细表格 -->
        <div class="conflict-card__body">
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
        </div>

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
                      <el-table-column label="推荐档位" width="110" align="center">
                        <template #default="{ row }">
                          <el-tag :type="recommendationTagType(row.recommendationLevel)" size="small">
                            {{ recommendationText(row.recommendationLevel) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="姓名" prop="name" width="100" />
                      <el-table-column label="工号" prop="empNo" width="110" />
                      <el-table-column label="岗位" prop="position" width="120" />
                      <el-table-column
                        label="技能"
                        prop="skills"
                        min-width="160"
                        show-overflow-tooltip
                      />
                      <el-table-column label="技能匹配度" width="130" align="center">
                        <template #default="{ row }">
                          <el-progress
                            :percentage="matchPercentage(row.matchScore)"
                            :color="matchScoreColor(row.matchScore)"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column label="可用率" width="130" align="center">
                        <template #default="{ row }">
                          <el-progress
                            :percentage="matchPercentage(row.availabilityRate)"
                            :color="availabilityColor(row.availabilityRate)"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        label="推荐理由"
                        prop="reason"
                        min-width="200"
                        show-overflow-tooltip
                      />
                      <el-table-column label="操作" width="100" align="center" fixed="right">
                        <template #default="{ row }">
                          <el-button
                            type="primary"
                            size="small"
                            :loading="isReplacing(sug.conflictAssignmentId, row.personnelId)"
                            @click="confirmReplace(sug, row)"
                          >
                            替换
                          </el-button>
                        </template>
                      </el-table-column>
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
      </article>

      <!-- 前端分页 -->
      <div v-if="conflictList.length > 0" class="conflict-list__pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="conflictList.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Download, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { detectAllConflicts, getSuggestions } from '@/api/conflict'
import { updateAssignment } from '@/api/assignment'
import { exportReport } from '@/api/report'
import { getDepartmentTree, flattenDepartments, type DepartmentVO } from '@/api/department'
import { getPersonnelAll } from '@/api/personnel'
import { useUserStore } from '@/stores/user'
import type { ConflictResult, Personnel } from '@/types'
import type { ConflictSuggestion, ReplacementCandidate } from '@/api/conflict'

type TagType = '' | 'success' | 'warning' | 'info' | 'danger' | 'primary'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const conflictList = ref<ConflictResult[]>([])

// 前端分页（冲突为内存计算结果，后端不分页）
const pageNum = ref(1)
const pageSize = ref(10)
const pagedConflictList = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return conflictList.value.slice(start, start + pageSize.value)
})

// 部门筛选
const filterDeptId = ref<number | undefined>(undefined)
const deptTreeData = ref<DepartmentVO[]>([])
const deptFlatList = ref<DepartmentVO[]>([])
// 全部人员（用于 personnelId → deptId 反查部门名）
const personnelList = ref<Personnel[]>([])

// 调优建议相关状态
const suggestionLoading = ref(false)
const suggestionsLoaded = ref(false)
const allSuggestions = ref<ConflictSuggestion[]>([])
// 各人员建议面板的展开状态，key 为 personnelId
const expanded = reactive<Record<number, boolean>>({})
// 正在执行替换的记录集合，key 为 `${assignmentId}-${candidatePersonnelId}`
const replacingKeys = reactive<Record<string, boolean>>({})

async function loadData() {
  loading.value = true
  try {
    conflictList.value = await detectAllConflicts(filterDeptId.value)
  } catch {
    conflictList.value = []
  } finally {
    loading.value = false
  }
  // 数据刷新后回到第一页
  pageNum.value = 1
}

// 加载部门树 + 全部人员（用于反查部门名）
async function loadDepartments() {
  try {
    deptTreeData.value = await getDepartmentTree()
    deptFlatList.value = flattenDepartments(deptTreeData.value)
  } catch {
    deptTreeData.value = []
    deptFlatList.value = []
  }
  try {
    personnelList.value = await getPersonnelAll()
  } catch {
    personnelList.value = []
  }
}

// 根据人员ID反查部门名称
function getDeptNameByPersonnelId(personnelId: number): string {
  const p = personnelList.value.find(x => x.id === personnelId)
  if (!p || !p.deptId) return ''
  return deptFlatList.value.find(d => d.id === p.deptId)?.name || ''
}

// 部门筛选变化时重新加载冲突与建议（若已加载过建议）
async function onFilterChange() {
  await loadData()
  if (suggestionsLoaded.value) {
    await loadSuggestions()
  }
}

async function loadSuggestions() {
  suggestionLoading.value = true
  try {
    allSuggestions.value = await getSuggestions(filterDeptId.value)
  } catch {
    allSuggestions.value = []
  } finally {
    suggestionLoading.value = false
  }
}

function isExpanded(personnelId: number): boolean {
  return !!expanded[personnelId]
}

// 替换按钮 loading 状态判断
function isReplacing(assignmentId: number, candidateId: number): boolean {
  return !!replacingKeys[`${assignmentId}-${candidateId}`]
}

// 当前操作人（用于后端记录操作日志）
function currentOperator(): string {
  const info = userStore.userInfo as { realName?: string; username?: string } | null
  return info?.realName || info?.username || 'unknown'
}

// 二次确认替换人员：不推荐档位多一道风险提示
async function confirmReplace(sug: ConflictSuggestion, candidate: ReplacementCandidate) {
  const isNotRecommended =
    (candidate.recommendationLevel || '').toUpperCase() === 'NOT_RECOMMENDED'

  const message = [
    `确认将【${sug.personnelName}】在项目【${sug.projectName}】的分配替换为【${candidate.name}（${candidate.empNo}）】？`,
    isNotRecommended
      ? '\n⚠️ 该候选人为「不推荐」档位，可用率或匹配度较低，替换后仍可能存在风险。'
      : ''
  ].join('')

  try {
    await ElMessageBox.confirm(message, '替换人员确认', {
      type: isNotRecommended ? 'warning' : 'info',
      confirmButtonText: '确认替换',
      cancelButtonText: '取消',
      confirmButtonClass: isNotRecommended ? 'el-button--warning' : 'el-button--primary'
    })
  } catch {
    return
  }

  await doReplace(sug, candidate)
}

// 执行替换：仅更新 personnelId 与 operator，其余字段由后端保留原值
async function doReplace(sug: ConflictSuggestion, candidate: ReplacementCandidate) {
  const key = `${sug.conflictAssignmentId}-${candidate.personnelId}`
  replacingKeys[key] = true
  try {
    await updateAssignment(sug.conflictAssignmentId, {
      personnelId: candidate.personnelId,
      operator: currentOperator()
    })
    ElMessage.success(`已替换为 ${candidate.name}（${candidate.empNo}）`)

    // 替换成功后重新检测冲突并刷新建议面板
    expanded[sug.personnelId] = false
    suggestionsLoaded.value = false
    await loadData()
    suggestionsLoaded.value = true
    await loadSuggestions()
    expanded[sug.personnelId] = true
  } catch (e) {
    ElMessage.error('替换失败，请稍后重试')
    console.error('[ConflictList] 替换人员失败:', e)
  } finally {
    replacingKeys[key] = false
  }
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
  exportReport('conflict', 'xlsx', { deptId: filterDeptId.value })
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

// 推荐档位标签类型
function recommendationTagType(level: string): TagType {
  const l = (level || '').toUpperCase()
  if (l === 'RECOMMENDED') return 'success'
  if (l === 'CONSIDERABLE') return 'warning'
  return 'info'
}

// 推荐档位中文文案
function recommendationText(level: string): string {
  const l = (level || '').toUpperCase()
  if (l === 'RECOMMENDED') return '推荐'
  if (l === 'CONSIDERABLE') return '可考虑'
  return '不推荐'
}

// 可用率进度条颜色
function availabilityColor(rate: number): string {
  const normalized = rate > 1 ? rate / 100 : rate
  if (normalized >= 0.8) return '#67c23a'
  if (normalized >= 0.5) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  loadDepartments()
  loadData()
})
</script>

<style scoped lang="scss">
.conflict-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---- 页面头部 ---- */
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
    max-width: 560px;
  }

  &__aside {
    flex-shrink: 0;
    display: flex;
    gap: 10px;
  }
}

/* ---- 空状态 ---- */
.empty-wrap {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);
}

/* ---- 筛选工具栏 ---- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);

  &__dept {
    width: 240px;
  }

  &__hint {
    font-size: 13px;
    color: var(--pw-text-secondary);
  }
}

/* ---- 冲突人员卡片 ---- */
.conflict-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__pager {
    display: flex;
    justify-content: flex-end;
    padding: 4px 0;
  }
}

.conflict-card {
  position: relative;
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-left: 4px solid var(--pw-danger);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--pw-transition), border-color var(--pw-transition);

  &:hover {
    box-shadow: var(--pw-shadow-md);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid var(--pw-border-light);
  }

  &__identity {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }

  &__avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    background: #fef2f2;
    color: var(--pw-danger);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__name {
    font-size: 16px;
    font-weight: 700;
    color: var(--pw-text-primary);
    line-height: 1.2;
  }

  &__sub {
    font-size: 12px;
    color: var(--pw-text-secondary);
  }

  &__dept {
    color: var(--pw-text-secondary);
  }

  &__badges {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
  }

  &__body {
    padding: 14px 20px 4px;
  }
}

.conflict-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;

  &--danger {
    background: #fef2f2;
    color: var(--pw-danger);
  }
}

/* ---- 调优建议区域 ---- */
.suggestion-area {
  padding: 12px 20px 20px;

  .suggestion-panel {
    margin-top: 14px;
    background: var(--pw-bg-hover);
    border: 1px dashed var(--pw-border);
    border-radius: var(--pw-radius);
    padding: 16px;

    .suggestion-content {
      min-height: 40px;
    }

    .suggestion-item {
      padding: 14px;
      margin-bottom: 12px;
      background: var(--pw-bg-card);
      border: 1px solid var(--pw-border-light);
      border-radius: var(--pw-radius);
      transition: border-color var(--pw-transition);

      &:hover {
        border-color: var(--pw-primary-lighter);
      }

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
          font-weight: 500;
          color: var(--pw-text-primary);
        }
      }

      .suggestion-desc {
        margin: 0 0 12px;
        font-size: 13px;
        color: var(--pw-text-regular);
        line-height: 1.6;
      }

      .candidate-table {
        margin-top: 4px;
      }
    }
  }
}
</style>
