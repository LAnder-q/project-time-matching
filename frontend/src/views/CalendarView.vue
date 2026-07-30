<template>
  <div class="calendar-page">
    <!-- 页面标题 -->
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-head__title">项目排期日历</h1>
        <p class="page-head__subtitle">
          查看人员与项目的时间分配，冲突安排将以红色高亮标识
        </p>
      </div>
    </div>

    <!-- 工具栏：白色圆角条，非 el-card -->
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-tree-select
          v-model="selectedDeptId"
          :data="deptTreeData"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          clearable
          placeholder="按部门筛选"
          class="toolbar__select"
          @change="loadCalendar"
        />

        <el-select
          v-model="selectedPersonnelId"
          placeholder="选择人员（全部）"
          clearable
          filterable
          class="toolbar__select"
          @change="loadCalendar"
        >
          <el-option
            v-for="p in personnelOptions"
            :key="p.id"
            :label="`${p.name} (${p.empNo})`"
            :value="p.id!"
          />
        </el-select>

        <el-select
          v-model="selectedProjectId"
          placeholder="选择项目（全部）"
          clearable
          filterable
          class="toolbar__select"
          @change="loadCalendar"
        >
          <el-option
            v-for="p in projectOptions"
            :key="p.id"
            :label="p.name"
            :value="p.id!"
          />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="toolbar__date"
          @change="loadCalendar"
        />
      </div>

      <div class="toolbar__actions">
        <transition name="tag-fade">
          <span v-if="conflictAssignmentKeys.size > 0" class="conflict-badge">
            <span class="conflict-badge__dot"></span>
            存在冲突的项目已用红色标记
          </span>
        </transition>

        <el-button type="primary" class="toolbar__refresh" @click="loadCalendar">
          <svg
            class="toolbar__refresh-icon"
            viewBox="0 0 24 24"
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <path d="M21 3v6h-6" />
          </svg>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 日历容器：白色圆角，非 el-card -->
    <div class="calendar-wrapper">
      <FullCalendar ref="calendarRef" :options="calendarOptions" />
    </div>

    <!-- 分配详情对话框 -->
    <el-dialog v-model="detailVisible" width="460px" class="detail-dialog">
      <template #header>
        <div class="detail-dialog__header">
          <span class="detail-dialog__icon">
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3" y="4" width="18" height="18" rx="2" />
              <path d="M16 2v4M8 2v4M3 10h18" />
            </svg>
          </span>
          <span class="detail-dialog__title">分配详情</span>
        </div>
      </template>

      <div v-if="currentEvent" class="detail-list">
        <div class="detail-list__row">
          <span class="detail-list__label">项目名称</span>
          <span class="detail-list__value detail-list__value--strong">
            {{ currentEvent.projectName }}
          </span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">人员姓名</span>
          <span class="detail-list__value">{{ currentEvent.personnelName }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">开始时间</span>
          <span class="detail-list__value detail-list__value--mono">
            {{ currentEvent.start }}
          </span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">结束时间</span>
          <span class="detail-list__value detail-list__value--mono">
            {{ currentEvent.end }}
          </span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">每日工时</span>
          <span class="detail-list__value">
            <span class="detail-list__hours">{{ currentEvent.dailyHours }}</span> 小时
          </span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">冲突提示</span>
          <span
            class="detail-list__value"
            :class="{
              'detail-list__value--conflict': currentEvent.hasConflict,
              'detail-list__value--ok': !currentEvent.hasConflict
            }"
          >
            <span
              class="status-pill"
              :class="currentEvent.hasConflict ? 'status-pill--danger' : 'status-pill--success'"
            >
              <span class="status-pill__dot"></span>
              {{ currentEvent.hasConflict ? '存在时间冲突' : '无冲突' }}
            </span>
          </span>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type {
  CalendarOptions,
  CalendarApi,
  EventClickArg,
  EventInput
} from '@fullcalendar/core'
import zhCnLocale from '@fullcalendar/core/locales/zh-cn'
import { getCalendarData, detectAllConflicts } from '@/api/conflict'
import { getPersonnelAll } from '@/api/personnel'
import { getProjectAll } from '@/api/project'
import { getDepartmentTree, type DepartmentVO } from '@/api/department'
import type { Personnel, Project, CalendarEvent } from '@/types'

const route = useRoute()
const calendarRef = ref<InstanceType<typeof FullCalendar>>()
const personnelOptions = ref<Personnel[]>([])
const projectOptions = ref<Project[]>([])
const deptTreeData = ref<DepartmentVO[]>([])
const selectedPersonnelId = ref<number | undefined>(undefined)
const selectedProjectId = ref<number | undefined>(undefined)
const selectedDeptId = ref<number | undefined>(undefined)
const dateRange = ref<[string, string] | undefined>(undefined)
const calendarEvents = ref<CalendarEvent[]>([])
const conflictAssignmentKeys = reactive(new Set<string>())
const detailVisible = ref(false)
const currentEvent = ref<(CalendarEvent & { hasConflict?: boolean }) | null>(null)

const calendarOptions = reactive<CalendarOptions>({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    // 日 / 周 / 月 / 年 四个视图切换按钮
    right: 'timeGridDay,timeGridWeek,dayGridMonth,dayGridYear'
  },
  // 自定义视图：年视图 = dayGrid 模式 + duration 1 年
  // buttonText（日/周/月/年）由 zh-cn locale 内置提供，无需重复配置
  views: {
    dayGridYear: {
      type: 'dayGrid',
      duration: { years: 1 },
      titleFormat: { year: 'numeric' }
    }
  },
  locale: zhCnLocale,
  height: 700,
  events: [],
  eventClick: handleEventClick
})

function getApi(): CalendarApi | undefined {
  return calendarRef.value?.getApi()
}

function handleEventClick(info: EventClickArg) {
  const found = calendarEvents.value.find((e) => e.id === info.event.id)
  if (found) {
    currentEvent.value = {
      ...found,
      hasConflict: conflictAssignmentKeys.has(`${found.personnelId}-${found.projectId}`)
    }
    detailVisible.value = true
  }
}

function buildEventInputs(data: CalendarEvent[]): EventInput[] {
  return data.map((e) => {
    const color = e.color || '#409eff'
    const hasConflict = conflictAssignmentKeys.has(`${e.personnelId}-${e.projectId}`)
    return {
      id: e.id,
      title: `${e.projectName}${hasConflict ? ' [冲突]' : ''}`,
      start: e.start,
      end: e.end,
      backgroundColor: hasConflict ? '#f56c6c' : color,
      borderColor: hasConflict ? '#f56c6c' : color
    }
  })
}

async function loadPersonnel() {
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
  try {
    deptTreeData.value = await getDepartmentTree()
  } catch {
    deptTreeData.value = []
  }
  const queryId = route.query.personnelId
  if (queryId) {
    selectedPersonnelId.value = Number(queryId)
  }
}

async function loadConflicts() {
  try {
    const list = await detectAllConflicts()
    conflictAssignmentKeys.clear()
    list.forEach((item) => {
      item.conflicts.forEach((cd) => {
        conflictAssignmentKeys.add(`${item.personnelId}-${cd.projectId1}`)
        conflictAssignmentKeys.add(`${item.personnelId}-${cd.projectId2}`)
      })
    })
  } catch {
    conflictAssignmentKeys.clear()
  }
}

async function loadCalendar() {
  try {
    const data = await getCalendarData({
      personnelId: selectedPersonnelId.value,
      projectId: selectedProjectId.value,
      deptId: selectedDeptId.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    })
    calendarEvents.value = data
    const api = getApi()
    if (api) {
      // 选了日期区间时，自动跳转到区间起始日期，避免视图仍停留在原月份
      if (dateRange.value?.[0]) {
        api.gotoDate(dateRange.value[0])
      }
      api.removeAllEvents()
      buildEventInputs(data).forEach((ev) => api.addEvent(ev))
    } else {
      calendarOptions.events = buildEventInputs(data)
    }
  } catch {
    calendarEvents.value = []
    const api = getApi()
    if (api) {
      api.removeAllEvents()
    }
  }
}

onMounted(async () => {
  await loadPersonnel()
  await loadConflicts()
  // 等待 FullCalendar 挂载完成后再渲染事件
  await loadCalendar()
})
</script>

<style scoped lang="scss">
.calendar-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---- 页面标题 ---- */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;

  &__title {
    font-size: 22px;
    font-weight: 700;
    color: var(--pw-text-primary);
    letter-spacing: -0.02em;
  }

  &__subtitle {
    font-size: 13px;
    color: var(--pw-text-secondary);
    margin-top: 4px;
  }
}

/* ---- 工具栏：白色圆角条 ---- */
.toolbar {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 14px 18px;
  box-shadow: var(--pw-shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  &__filters {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__select {
    width: 200px;
  }

  &__date {
    width: 280px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__refresh {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  &__refresh-icon {
    transition: transform 0.5s ease;
  }

  &__refresh:hover &__refresh-icon {
    transform: rotate(180deg);
  }
}

/* ---- 冲突提示徽章 ---- */
.conflict-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--pw-danger);
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 100px;
  line-height: 1;

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--pw-danger);
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.18);
    animation: pulse 1.6s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.18);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(220, 38, 38, 0.08);
  }
}

.tag-fade-enter-active,
.tag-fade-leave-active {
  transition: all var(--pw-transition);
}

.tag-fade-enter-from,
.tag-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ---- 日历容器 ---- */
.calendar-wrapper {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 18px;
  box-shadow: var(--pw-shadow-sm);
  overflow: hidden;

  :deep(.fc) {
    font-size: 13px;
    font-family: inherit;
  }

  /* 顶部工具条 */
  :deep(.fc-toolbar.fc-header-toolbar) {
    margin-bottom: 16px;
    flex-wrap: wrap;
    gap: 10px;
  }

  :deep(.fc-toolbar-title) {
    font-size: 17px;
    font-weight: 700;
    color: var(--pw-text-primary);
    letter-spacing: -0.01em;
  }

  :deep(.fc-button) {
    border-radius: var(--pw-radius-sm) !important;
    border: 1px solid var(--pw-border) !important;
    background: var(--pw-bg-card) !important;
    color: var(--pw-text-regular) !important;
    font-weight: 500;
    text-transform: none !important;
    box-shadow: none !important;
    padding: 6px 12px;
    transition: all var(--pw-transition);

    &:hover {
      background: var(--pw-bg-hover) !important;
      color: var(--pw-primary) !important;
      border-color: var(--pw-primary-lighter) !important;
    }

    &.fc-button-active {
      background: var(--pw-primary) !important;
      color: #fff !important;
      border-color: var(--pw-primary) !important;
    }

    &:focus {
      box-shadow: none !important;
    }
  }

  :deep(.fc-button-group .fc-button) {
    border-radius: 0 !important;

    &:first-child {
      border-top-left-radius: var(--pw-radius-sm) !important;
      border-bottom-left-radius: var(--pw-radius-sm) !important;
    }

    &:last-child {
      border-top-right-radius: var(--pw-radius-sm) !important;
      border-bottom-right-radius: var(--pw-radius-sm) !important;
    }
  }

  /* 表头 */
  :deep(.fc-col-header-cell) {
    background: var(--pw-bg-hover);
    border-color: var(--pw-border) !important;
    padding: 10px 0;

    .fc-col-header-cell-cushion {
      font-weight: 600;
      color: var(--pw-text-regular);
      font-size: 12.5px;
      padding: 6px 4px;
    }
  }

  /* 日期单元格 */
  :deep(.fc-daygrid-day) {
    border-color: var(--pw-border) !important;
    transition: background var(--pw-transition);

    &:hover {
      background: var(--pw-bg-hover);
    }

    &.fc-day-today {
      background: var(--pw-primary-lightest) !important;

      .fc-daygrid-day-number {
        color: var(--pw-primary);
        font-weight: 700;
      }
    }
  }

  :deep(.fc-daygrid-day-number) {
    color: var(--pw-text-regular);
    font-weight: 500;
    padding: 6px 8px;
  }

  :deep(.fc-day-other) {
    background: #fbfcfd;

    .fc-daygrid-day-number {
      color: var(--pw-text-placeholder);
    }
  }

  /* 事件块 */
  :deep(.fc-event) {
    border-radius: var(--pw-radius-sm) !important;
    border: none !important;
    padding: 3px 7px !important;
    font-weight: 500;
    font-size: 12px;
    cursor: pointer;
    transition: transform var(--pw-transition), box-shadow var(--pw-transition);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 3px 8px rgba(15, 23, 42, 0.15);
    }
  }

  :deep(.fc-daygrid-event) {
    margin-top: 3px;
  }

  :deep(.fc-daygrid-day-frame) {
    min-height: 88px;
  }

  /* 时间网格视图 */
  :deep(.fc-timegrid-slot) {
    height: 2.5em;
    border-color: var(--pw-border-light) !important;
  }

  :deep(.fc-timegrid-axis) {
    border-color: var(--pw-border) !important;

    .fc-timegrid-axis-cushion {
      color: var(--pw-text-secondary);
      font-size: 11px;
    }
  }

  :deep(.fc-timegrid-now-indicator-line) {
    border-color: var(--pw-danger);
  }

  :deep(.fc .fc-scrollgrid) {
    border-color: var(--pw-border) !important;
    border-radius: var(--pw-radius);
    overflow: hidden;
  }

  :deep(.fc-scrollgrid-section > td) {
    border-color: var(--pw-border) !important;
  }

  :deep(.fc .fc-scrollgrid-liquid) {
    height: 100%;
  }
}

/* ---- 详情对话框 ---- */
.detail-dialog {
  :deep(.el-dialog__header) {
    padding: 18px 22px 14px;
  }

  :deep(.el-dialog__body) {
    padding: 8px 22px 20px;
  }
}

.detail-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-dialog__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--pw-radius-sm);
  background: var(--pw-primary-lightest);
  color: var(--pw-primary);
}

.detail-dialog__title {
  font-size: 16px;
  font-weight: 700;
  color: var(--pw-text-primary);
}

.detail-list {
  display: flex;
  flex-direction: column;

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 13px 0;
    border-bottom: 1px dashed var(--pw-border);

    &:last-child {
      border-bottom: none;
    }
  }

  &__label {
    font-size: 13px;
    color: var(--pw-text-secondary);
    flex-shrink: 0;
  }

  &__value {
    font-size: 14px;
    color: var(--pw-text-primary);
    text-align: right;
    display: inline-flex;
    align-items: center;
    gap: 6px;

    &--strong {
      font-weight: 700;
    }

    &--mono {
      font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
      font-size: 13px;
      color: var(--pw-text-regular);
    }

    &--conflict {
      color: var(--pw-danger);
    }

    &--ok {
      color: var(--pw-success);
    }
  }

  &__hours {
    font-size: 16px;
    font-weight: 700;
    color: var(--pw-primary);
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &--success {
    background: #ecfdf5;
    color: var(--pw-success);

    .status-pill__dot {
      background: var(--pw-success);
    }
  }

  &--danger {
    background: #fef2f2;
    color: var(--pw-danger);

    .status-pill__dot {
      background: var(--pw-danger);
      animation: pulse-danger 1.5s ease-in-out infinite;
    }
  }
}

@keyframes pulse-danger {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

/* ---- 响应式 ---- */
@media (max-width: 992px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;

    &__actions {
      justify-content: flex-start;
    }
  }
}
</style>
