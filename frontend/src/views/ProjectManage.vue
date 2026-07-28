<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按项目名搜索"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="handleAdd">新增项目</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
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

      <div class="pagination">
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
    </el-card>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getProjectPage, createProject, updateProject, deleteProject } from '@/api/project'
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

onMounted(() => {
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

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
