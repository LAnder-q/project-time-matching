<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按姓名/工号搜索"
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
        <el-button type="success" @click="handleAdd">新增人员</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
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
  deletePersonnel
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
