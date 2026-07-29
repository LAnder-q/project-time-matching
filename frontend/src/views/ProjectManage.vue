<template>
  <div class="page-container">
    <header class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">项目管理</h1>
        <p class="page-subtitle">维护项目周期、优先级与人力需求，支持批量导入</p>
      </div>
    </header>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按项目名搜索"
        clearable
        class="toolbar__search"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="handleSearch">搜索</el-button>
      <div class="toolbar__spacer"></div>
      <el-button @click="handleBatchImport">批量导入</el-button>
      <el-button type="primary" @click="handleAdd">新增项目</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" stripe class="table-card__table">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="startDate" label="开始日期" width="130" />
        <el-table-column prop="endDate" label="结束日期" width="130" />
        <el-table-column prop="priority" label="优先级" width="160" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.priority" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="requiredPosition" label="所需职位" width="140" />
        <el-table-column prop="dailyHours" label="每日工时" width="100" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑项目' : '新增项目'"
      width="540px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
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
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="选择优先级" style="width: 100%">
            <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="所需职位" prop="requiredPosition">
          <el-input v-model="form.requiredPosition" placeholder="请输入所需职位" />
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

    <el-dialog v-model="importDialogVisible" title="批量导入项目" width="680px">
      <el-alert
        title="数据格式说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        每行一个项目，字段顺序：项目名称, 开始日期, 结束日期, 优先级, 所需职位, 每日工时。日期格式 YYYY-MM-DD。项目名已存在则更新。
      </el-alert>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="10"
        placeholder="示例:&#10;项目A,2024-01-01,2024-06-30,5,Java工程师,8&#10;项目B,2024-03-01,2024-09-30,3,前端工程师,6"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImportSubmit">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getProjectPage, createProject, updateProject, deleteProject, batchImportProjects } from '@/api/project'
import type { Project } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Project[]>([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 批量导入
const importDialogVisible = ref(false)
const importText = ref('')
const importing = ref(false)

const defaultForm = (): Project => ({
  name: '',
  startDate: '',
  endDate: '',
  priority: 3,
  requiredPosition: '',
  dailyHours: 8
})

const form = reactive<Project>(defaultForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  requiredPosition: [{ required: true, message: '请输入所需职位', trigger: 'blur' }],
  dailyHours: [{ required: true, message: '请输入每日工时', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProjectPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    tableData.value = res.list
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row: Project) {
  isEdit.value = true
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

function handleDelete(row: Project) {
  ElMessageBox.confirm(`确定要删除项目【${row.name}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteProject(row.id!)
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
        await updateProject(form.id!, { ...form })
        ElMessage.success('修改成功')
      } else {
        await createProject({ ...form })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

// 批量导入
function handleBatchImport() {
  importText.value = ''
  importDialogVisible.value = true
}

async function handleImportSubmit() {
  if (!importText.value.trim()) {
    ElMessage.warning('请输入导入数据')
    return
  }
  const lines = importText.value.trim().split('\n').filter((l) => l.trim())
  const projects: Partial<Project>[] = []
  for (const line of lines) {
    const parts = line.split(',').map((s) => s.trim())
    if (parts.length < 6) {
      ElMessage.error(`数据格式错误，每行至少需要6个字段: ${line}`)
      return
    }
    projects.push({
      name: parts[0],
      startDate: parts[1],
      endDate: parts[2],
      priority: parseInt(parts[3]) || 3,
      requiredPosition: parts[4],
      dailyHours: parseFloat(parts[5]) || 8
    })
  }
  importing.value = true
  try {
    await batchImportProjects(projects)
    ElMessage.success(`成功导入 ${projects.length} 条项目数据`)
    importDialogVisible.value = false
    loadData()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;

  &__main {
    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: var(--pw-text-primary);
      letter-spacing: -0.02em;
      line-height: 1.2;
    }

    .page-subtitle {
      font-size: 13px;
      color: var(--pw-text-secondary);
      margin-top: 4px;
    }
  }
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);

  &__search {
    width: 280px;
  }

  &__spacer {
    flex: 1;
  }
}

.table-card {
  background: var(--pw-bg-card);
  border: 1px solid var(--pw-border);
  border-radius: var(--pw-radius-lg);
  box-shadow: var(--pw-shadow-sm);
  overflow: hidden;

  &__table {
    width: 100%;
  }
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 14px 16px;
  border-top: 1px solid var(--pw-border-light);
  background: var(--pw-bg-hover);
}
</style>
