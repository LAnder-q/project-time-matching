<template>
  <div class="page-container">
    <header class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">人员管理</h1>
        <p class="page-subtitle">维护人员基础信息、技能标签与可用档期，支持批量导入</p>
      </div>
    </header>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按姓名 / 工号搜索"
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
      <el-button type="primary" @click="handleAdd">新增人员</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" stripe class="table-card__table">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="position" label="职位" width="140" />
        <el-table-column prop="skills" label="技能" min-width="180" show-overflow-tooltip />
        <el-table-column prop="availableStartDate" label="可用开始日期" width="140" />
        <el-table-column prop="availableEndDate" label="可用结束日期" width="140" />
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
      :title="isEdit ? '编辑人员' : '新增人员'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工号" prop="empNo">
          <el-input v-model="form.empNo" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="技能" prop="skills">
          <el-input
            v-model="form.skills"
            type="textarea"
            :rows="2"
            placeholder="请输入技能，多个用逗号分隔"
          />
        </el-form-item>
        <el-form-item label="可用开始日期" prop="availableStartDate">
          <el-date-picker
            v-model="form.availableStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="可用结束日期" prop="availableEndDate">
          <el-date-picker
            v-model="form.availableEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入人员" width="680px">
      <el-alert
        title="数据格式说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        每行一个人员，字段顺序：工号, 姓名, 职位, 技能(逗号分隔), 可用开始日期, 可用结束日期。日期格式 YYYY-MM-DD。工号已存在则更新。
      </el-alert>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="10"
        placeholder="示例:&#10;EMP001,张伟,运维工程师,Linux,Docker,Kubernetes,2026-01-01,2026-12-31&#10;EMP002,李娜,运维工程师,Linux,Docker,Python,2026-01-01,2026-12-31"
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
import {
  getPersonnelPage,
  createPersonnel,
  updatePersonnel,
  deletePersonnel,
  batchImportPersonnel
} from '@/api/personnel'
import type { Personnel } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Personnel[]>([])
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

const defaultForm = (): Personnel => ({
  empNo: '',
  name: '',
  position: '',
  skills: '',
  availableStartDate: '',
  availableEndDate: ''
})

const form = reactive<Personnel>(defaultForm())

const rules: FormRules = {
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  position: [{ required: true, message: '请输入职位', trigger: 'blur' }],
  availableStartDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  availableEndDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPersonnelPage({
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

function handleEdit(row: Personnel) {
  isEdit.value = true
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

function handleDelete(row: Personnel) {
  ElMessageBox.confirm(`确定要删除人员【${row.name}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deletePersonnel(row.id!)
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
        await updatePersonnel(form.id!, { ...form })
        ElMessage.success('修改成功')
      } else {
        await createPersonnel({ ...form })
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
  const personnelList: Partial<Personnel>[] = []
  for (const line of lines) {
    const parts = line.split(',').map((s) => s.trim())
    if (parts.length < 6) {
      ElMessage.error(`数据格式错误，每行至少需要6个字段: ${line}`)
      return
    }
    personnelList.push({
      empNo: parts[0],
      name: parts[1],
      position: parts[2],
      skills: parts.slice(3, -2).join(','),
      availableStartDate: parts[parts.length - 2],
      availableEndDate: parts[parts.length - 1]
    })
  }
  importing.value = true
  try {
    await batchImportPersonnel(personnelList)
    ElMessage.success(`成功导入 ${personnelList.length} 条人员数据`)
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
