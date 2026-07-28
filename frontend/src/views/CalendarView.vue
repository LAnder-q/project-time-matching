<template>
  <div class="calendar-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-select
          v-model="selectedPersonnelId"
          placeholder="选择人员（全部）"
          clearable
          filterable
          style="width: 240px"
          @change="loadCalendar"
        >
          <el-option
            v-for="p in personnelOptions"
            :key="p.id"
            :label="`${p.name} (${p.empNo})`"
            :value="p.id!"
          />
        </el-select>

        <el-radio-group v-model="calendarView" @change="changeView">
          <el-radio-button value="timeGridDay">日</el-radio-button>
          <el-radio-button value="timeGridWeek">周</el-radio-button>
          <el-radio-button value="dayGridMonth">月</el-radio-button>
        </el-radio-group>

        <el-tag v-if="conflictPersonIds.size > 0" type="danger">
          存在冲突的项目已用红色标记
        </el-tag>
        <el-button type="primary" link @click="loadCalendar">刷新</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="calendar-card">
      <FullCalendar ref="calendarRef" :options="calendarOptions" />
    </el-card>

    <el-dialog v-model="detailVisible" title="分配详情" width="440px">
      <el-descriptions v-if="currentEvent" :column="1" border>
        <el-descriptions-item label="项目名称">
          {{ currentEvent.projectName }}
        </el-descriptions-item>
        <el-descriptions-item label="人员姓名">
          {{ currentEvent.personnelName }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentEvent.start }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentEvent.end }}</el-descriptions-item>
        <el-descriptions-item label="每日工时">
          {{ currentEvent.dailyHours }} 小时
        </el-descriptions-item>
        <el-descriptions-item label="冲突提示">
          <el-tag :type="currentEvent.hasConflict ? 'danger' : 'success'">
            {{ currentEvent.hasConflict ? '存在时间冲突' : '无冲突' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
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
import type { Personnel, CalendarEvent } from '@/types'

const route = useRoute()
const calendarRef = ref<InstanceType<typeof FullCalendar>>()
const personnelOptions = ref<Personnel[]>([])
const selectedPersonnelId = ref<number | undefined>(undefined)
const calendarView = ref('dayGridMonth')
const calendarEvents = ref<CalendarEvent[]>([])
const conflictPersonIds = reactive(new Set<number>())
const detailVisible = ref(false)
const currentEvent = ref<(CalendarEvent & { hasConflict?: boolean }) | null>(null)

const colorPalette = [
  '#409eff', '#67c23a', '#e6a23c', '#909399', '#9b59b6',
  '#1abc9c', '#34495e', '#f39c12', '#16a085', '#e74c3c'
]

// 按项目分配不同颜色
const projectColorMap = computed(() => {
  const map = new Map<number, string>()
  calendarEvents.value.forEach((e) => {
    if (!map.has(e.projectId)) {
      map.set(e.projectId, colorPalette[map.size % colorPalette.length])
    }
  })
  return map
})

const calendarOptions = reactive<CalendarOptions>({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: ''
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
      hasConflict: conflictPersonIds.has(found.personnelId)
    }
    detailVisible.value = true
  }
}

function buildEventInputs(data: CalendarEvent[]): EventInput[] {
  return data.map((e) => {
    const color = projectColorMap.value.get(e.projectId) || '#409eff'
    const hasConflict = conflictPersonIds.has(e.personnelId)
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
  const queryId = route.query.personnelId
  if (queryId) {
    selectedPersonnelId.value = Number(queryId)
  }
}

async function loadConflicts() {
  try {
    const list = await detectAllConflicts()
    conflictPersonIds.clear()
    list.forEach((item) => conflictPersonIds.add(item.personnelId))
  } catch {
    conflictPersonIds.clear()
  }
}

async function loadCalendar() {
  try {
    const data = await getCalendarData({
      personnelId: selectedPersonnelId.value
    })
    calendarEvents.value = data
    const api = getApi()
    if (api) {
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

function changeView(view: string) {
  const api = getApi()
  api?.changeView(view)
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
  .toolbar-card {
    margin-bottom: 16px;

    .toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }
  }

  .calendar-card {
    :deep(.fc) {
      font-size: 13px;
    }
  }
}
</style>
