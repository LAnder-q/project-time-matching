<template>
  <div class="assignment-page">
    <!-- 页面头部 -->
    <header class="page-head">
      <div class="page-head__lead">
        <h1 class="page-head__title">分配管理</h1>
        <p class="page-head__desc">管理人员与项目的时间分配记录，支持按人员或项目维度筛选</p>
      </div>
      <div class="page-head__aside">
        <el-button type="primary" @click="handleAdd">新增分配</el-button>
      </div>
    </header>

    <!-- 筛选工具栏 -->
    <section class="filter-bar">
      <div class="filter-bar__group">
        <span class="filter-bar__label">筛选</span>
        <el-tree-select
          v-model="filterDeptId"
          :data="deptTreeData"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          clearable
          placeholder="按部门筛选"
          class="filter-bar__dept"
          @change="onFilterDeptChange"
        />
        <el-select
          v-model="filterPersonnelId"
          placeholder="按人员筛选"
          clearable
          filterable
          :filter-method="filterPersonnel"
          style="width: 200px"
          @change="loadData"
          @visible-change="onPersonnelSelectClose"
        >
          <el-option
            v-for="p in filteredPersonnelOptions"
            :key="p.id"
            :label="`${p.name} (${p.empNo})`"
            :value="p.id!"
          />
        </el-select>
        <el-select
          v-model="filterProjectId"
          placeholder="按项目筛选"
          clearable
          style="width: 200px"
          @change="loadData"
        >
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </div>
      <div class="filter-bar__meta">
        共 <strong>{{ tableData.length }}</strong> 条分配记录
      </div>
    </section>

    <!-- 数据表格 -->
    <section class="table-panel">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="personnelName" label="人员姓名" width="140">
          <template #default="{ row }">{{ row.personnelName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="empNo" label="工号" width="120">
          <template #default="{ row }">{{ row.empNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.projectName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="130" />
        <el-table-column prop="endDate" label="结束日期" width="130" />
        <el-table-column prop="dailyHours" label="每日工时" width="100" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分配' : '新增分配'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTreeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="请选择部门（可缩小人员范围）"
            style="width: 100%"
            @change="onFormDeptChange"
          />
        </el-form-item>
        <el-form-item label="人员" prop="personnelId">
          <el-select
            v-model="form.personnelId"
            placeholder="请选择人员"
            filterable
            :filter-method="filterPersonnel"
            style="width: 100%"
            @visible-change="onPersonnelSelectClose"
          >
            <el-option
              v-for="p in formPersonnelOptions"
              :key="p.id"
              :label="`${p.name} (${p.empNo})`"
              :value="p.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <el-select
            v-model="form.projectId"
            placeholder="请选择项目"
            filterable
            style="width: 100%"
          >
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="每日工时" prop="dailyHours">
          <el-input-number v-model="form.dailyHours" :min="1" :max="24" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAssignmentList,
  createAssignment,
  updateAssignment,
  deleteAssignment
} from '@/api/assignment'
import { getPersonnelAll } from '@/api/personnel'
import { getProjectAll } from '@/api/project'
import { getDepartmentTree, type DepartmentVO } from '@/api/department'
import type { Assignment, Personnel, Project } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Assignment[]>([])
const personnelOptions = ref<Personnel[]>([])
const projectOptions = ref<Project[]>([])
const deptTreeData = ref<DepartmentVO[]>([])

// 顶部筛选：部门 + 人员级联
const filterDeptId = ref<number | undefined>(undefined)
const filterPersonnelId = ref<number | undefined>(undefined)
const filterProjectId = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): Assignment & { deptId?: number | null } => ({
  personnelId: undefined as unknown as number,
  projectId: undefined as unknown as number,
  startDate: '',
  endDate: '',
  dailyHours: 8,
  // 部门仅用于表单内人员筛选联动，不提交后端
  deptId: undefined
})

const form = reactive<Assignment & { deptId?: number | null }>(defaultForm())

// 顶部筛选部门变化时：若已选人员不在该部门，则清空人员
function onFilterDeptChange() {
  if (
    filterPersonnelId.value &&
    !filteredPersonnelOptions.value.some(p => p.id === filterPersonnelId.value)
  ) {
    filterPersonnelId.value = undefined
  }
  loadData()
}

// 表单内部门变化时：清空已选人员（避免部门与人员不一致）
function onFormDeptChange() {
  form.personnelId = undefined as unknown as number
}

// 人员下拉自定义搜索：匹配姓名、工号、岗位、技能（不区分大小写）
// el-select 的 filter-method 接收用户输入值，这里将其存入 ref 触发 computed 重新过滤
const personnelFilterKeyword = ref('')

function filterPersonnel(val: string) {
  personnelFilterKeyword.value = (val || '').trim().toLowerCase()
}

// 下拉关闭时清空搜索关键词，避免下次打开仍处于过滤状态
function onPersonnelSelectClose(visible: boolean) {
  if (!visible) {
    personnelFilterKeyword.value = ''
  }
}

// 按关键词匹配人员（姓名 / 工号 / 岗位 / 技能）
function matchPersonnel(p: Personnel): boolean {
  const kw = personnelFilterKeyword.value
  if (!kw) return true
  const haystack = [
    p.name || '',
    p.empNo || '',
    p.positions || '',
    p.skills || ''
  ].join(' ').toLowerCase()
  return haystack.includes(kw)
}

// 顶部筛选：按部门 + 关键词双重过滤
const filteredPersonnelOptions = computed(() => {
  let list = personnelOptions.value
  if (filterDeptId.value) {
    list = list.filter(p => p.deptId === filterDeptId.value)
  }
  return list.filter(matchPersonnel)
})

// 表单内：按部门 + 关键词双重过滤
const formPersonnelOptions = computed(() => {
  let list = personnelOptions.value
  if (form.deptId) {
    list = list.filter(p => p.deptId === form.deptId)
  }
  return list.filter(matchPersonnel)
})

const rules: FormRules = {
  personnelId: [{ required: true, message: '请选择人员', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  dailyHours: [{ required: true, message: '请输入每日工时', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAssignmentList({
      personnelId: filterPersonnelId.value,
      projectId: filterProjectId.value
    })
    tableData.value = res
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
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
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row: Assignment) {
  isEdit.value = true
  Object.assign(form, defaultForm(), row)
  // 编辑时根据 personnelId 反查 deptId 回填，便于人员下拉联动展示
  const p = personnelOptions.value.find(x => x.id === row.personnelId)
  form.deptId = p?.deptId
  dialogVisible.value = true
}

function handleDelete(row: Assignment) {
  ElMessageBox.confirm('确定要删除该分配吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteAssignment(row.id!)
        ElMessage.success('删除成功')
        loadData()
      } catch {
        // 错误已在拦截器统一提示
      }
    })
    .catch(() => {})
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      // 剥除 deptId（仅用于表单内人员筛选联动，不提交后端）
      const { deptId, ...payload } = form
      if (isEdit.value) {
        await updateAssignment(form.id!, payload)
        ElMessage.success('修改成功')
      } else {
        await createAssignment(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped lang="scss">
.assignment-page {
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
  }

  &__aside {
    flex-shrink: 0;
  }
}

/* ---- 筛选工具栏 ---- */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  padding: 14px 18px;
  box-shadow: var(--pw-shadow-sm);

  &__group {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--pw-text-secondary);
    letter-spacing: 0.04em;
    text-transform: uppercase;
    padding-right: 4px;
    border-right: 1px solid var(--pw-border);
    margin-right: 2px;
  }

  &__dept {
    width: 200px;
  }

  &__meta {
    font-size: 13px;
    color: var(--pw-text-secondary);

    strong {
      color: var(--pw-primary);
      font-weight: 700;
      font-size: 15px;
      margin: 0 3px;
    }
  }
}

/* ---- 表格面板 ---- */
.table-panel {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);
  padding: 14px 16px;
}
</style>
