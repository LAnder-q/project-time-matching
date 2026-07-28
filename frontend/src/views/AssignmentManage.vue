<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select
          v-model="filterPersonnelId"
          placeholder="按人员筛选"
          clearable
          style="width: 200px"
          @change="loadData"
        >
          <el-option
            v-for="p in personnelOptions"
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
        <el-button type="success" @click="handleAdd">新增分配</el-button>
      </div>

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
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分配' : '新增分配'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="人员" prop="personnelId">
          <el-select
            v-model="form.personnelId"
            placeholder="请选择人员"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in personnelOptions"
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAssignmentList,
  createAssignment,
  updateAssignment,
  deleteAssignment
} from '@/api/assignment'
import { getPersonnelAll } from '@/api/personnel'
import { getProjectAll } from '@/api/project'
import type { Assignment, Personnel, Project } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Assignment[]>([])
const personnelOptions = ref<Personnel[]>([])
const projectOptions = ref<Project[]>([])
const filterPersonnelId = ref<number | undefined>(undefined)
const filterProjectId = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): Assignment => ({
  personnelId: undefined as unknown as number,
  projectId: undefined as unknown as number,
  startDate: '',
  endDate: '',
  dailyHours: 8
})

const form = reactive<Assignment>(defaultForm())

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
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row: Assignment) {
  isEdit.value = true
  Object.assign(form, defaultForm(), row)
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
      if (isEdit.value) {
        await updateAssignment(form.id!, { ...form })
        ElMessage.success('修改成功')
      } else {
        await createAssignment({ ...form })
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
.page-container {
  .toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }
}
</style>
