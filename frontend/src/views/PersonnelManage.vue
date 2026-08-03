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
      <el-tree-select
        v-model="filterDeptId"
        :data="deptTreeData"
        :props="{ label: 'name', value: 'id', children: 'children' }"
        check-strictly
        clearable
        placeholder="按部门筛选"
        class="toolbar__dept"
        @change="handleSearch"
      />
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
        <el-table-column prop="positions" label="岗位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="skills" label="技能" min-width="180" show-overflow-tooltip />
        <el-table-column label="部门" width="160">
          <template #default="{ row }">
            {{ getDeptName(row.deptId) }}
          </template>
        </el-table-column>
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
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTreeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="请选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位" prop="positions">
          <el-input
            v-model="form.positions"
            placeholder="多个岗位用逗号分隔，如：运维工程师,DBA"
          />
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
      <el-tabs v-model="importTab">
        <el-tab-pane label="Excel 文件导入" name="excel">
          <el-alert
            title="操作说明"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          >
            请先下载模板，按模板格式填写数据后上传。工号已存在则更新，不存在则新增。部门按名称匹配，未匹配到则为空。
          </el-alert>
          <div style="margin-bottom: 16px">
            <el-button @click="handleDownloadTemplate">下载导入模板</el-button>
          </div>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .xlsx / .xls 格式，文件大小不超过 10MB</div>
            </template>
          </el-upload>
          <template #footer>
            <el-button @click="importDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="importing" @click="handleExcelImport">导入</el-button>
          </template>
        </el-tab-pane>
        <el-tab-pane label="文本批量导入" name="text">
          <el-alert
            title="数据格式说明"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
          >
            每行一个人员，字段顺序：工号, 姓名, 部门名称, 岗位(逗号分隔多岗位), 技能(逗号分隔), 可用开始日期, 可用结束日期。日期格式 YYYY-MM-DD。工号已存在则更新。部门按名称匹配，未匹配到则为空。
          </el-alert>
          <el-input
            v-model="importText"
            type="textarea"
            :rows="10"
            placeholder="示例:&#10;EMP001,张伟,基础运维组,运维工程师,Linux,Docker,Kubernetes,2026-01-01,2026-12-31&#10;EMP002,李娜,运维部,运维工程师,DBA,Linux,Docker,Python,2026-01-01,2026-12-31"
          />
          <template #footer>
            <el-button @click="importDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="importing" @click="handleImportSubmit">导入</el-button>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadInstance, type UploadFile, type UploadFiles } from 'element-plus'
import { Search, UploadFilled } from '@element-plus/icons-vue'
import {
  getPersonnelPage,
  createPersonnel,
  updatePersonnel,
  deletePersonnel,
  batchImportPersonnel,
  importPersonnelExcel,
  downloadPersonnelTemplate
} from '@/api/personnel'
import { getDepartmentTree, flattenDepartments, type DepartmentVO } from '@/api/department'
import type { Personnel } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Personnel[]>([])
const keyword = ref('')
// 部门筛选
const filterDeptId = ref<number | undefined>(undefined)
const deptTreeData = ref<DepartmentVO[]>([])
const deptFlatList = ref<DepartmentVO[]>([])
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
const importTab = ref('excel')
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)

const defaultForm = (): Personnel => ({
  empNo: '',
  name: '',
  positions: '',
  skills: '',
  deptId: null,
  availableStartDate: '',
  availableEndDate: ''
})

const form = reactive<Personnel>(defaultForm())

const rules: FormRules = {
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  positions: [{ required: true, message: '请输入岗位', trigger: 'blur' }],
  availableStartDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  availableEndDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

// 加载部门树
async function loadDepartments() {
  try {
    deptTreeData.value = await getDepartmentTree()
    deptFlatList.value = flattenDepartments(deptTreeData.value)
  } catch {
    deptTreeData.value = []
    deptFlatList.value = []
  }
}

// 根据部门ID查部门名称（用于表格展示）
function getDeptName(deptId?: number | null): string {
  if (!deptId) return ''
  return deptFlatList.value.find(d => d.id === deptId)?.name || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPersonnelPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      deptId: filterDeptId.value
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
  selectedFile.value = null
  importTab.value = 'excel'
  importDialogVisible.value = true
}

// 下载导入模板
function handleDownloadTemplate() {
  downloadPersonnelTemplate()
}

// 文件选择变化
function handleFileChange(file: UploadFile, _files: UploadFiles) {
  selectedFile.value = file.raw || null
}

// 超出文件数量限制
function handleExceed(_files: UploadFile[]) {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

// Excel 文件导入
async function handleExcelImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的 Excel 文件')
    return
  }
  importing.value = true
  try {
    const count = await importPersonnelExcel(selectedFile.value)
    ElMessage.success(`成功导入 ${count} 条人员数据`)
    importDialogVisible.value = false
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    loadData()
  } catch {
    // 错误已在拦截器统一提示
  } finally {
    importing.value = false
  }
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
    if (parts.length < 7) {
      ElMessage.error(`数据格式错误，每行至少需要7个字段: ${line}`)
      return
    }
    // 字段顺序：工号, 姓名, 部门名称, 岗位(逗号分隔), 技能..., 可用开始日期, 可用结束日期
    const empNo = parts[0]
    const name = parts[1]
    const deptName = parts[2]
    const positions = parts[3]
    // 技能：parts[4] 到 倒数第2个（含），倒数第2/第1为日期
    const skills = parts.slice(4, -2).join(',')
    const availableStartDate = parts[parts.length - 2]
    const availableEndDate = parts[parts.length - 1]
    // 部门按名称匹配（精确）
    const dept = deptFlatList.value.find(d => d.name === deptName)
    personnelList.push({
      empNo,
      name,
      positions,
      skills,
      deptId: dept?.id ?? null,
      availableStartDate,
      availableEndDate
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
  loadDepartments()
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

  &__dept {
    width: 200px;
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
