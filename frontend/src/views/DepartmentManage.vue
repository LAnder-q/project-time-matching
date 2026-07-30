<template>
  <div class="page-container">
    <header class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">部门管理</h1>
        <p class="page-subtitle">维护公司部门层级，支持父子部门结构</p>
      </div>
    </header>

    <div class="toolbar">
      <el-button type="primary" @click="handleAdd(null)">新增顶级部门</el-button>
      <el-button @click="loadData">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
      >
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column prop="code" label="部门编码" width="160" />
        <el-table-column prop="sort" label="同级排序" width="100" align="center" />
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleAdd(row)">新增子部门</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      width="480px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentSelectData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="不选则为顶级部门"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入部门编码（如 OPS）" />
        </el-form-item>
        <el-form-item label="同级排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
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
  getDepartmentTree,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  type DepartmentVO,
  type Department
} from '@/api/department'

const loading = ref(false)
const treeData = ref<DepartmentVO[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Department>({
  name: '',
  code: '',
  parentId: null,
  sort: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

// 上级部门下拉数据（树形）
const parentSelectData = ref<DepartmentVO[]>([])

async function loadData() {
  loading.value = true
  try {
    treeData.value = await getDepartmentTree()
    parentSelectData.value = treeData.value
  } catch {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function handleAdd(parent: DepartmentVO | null) {
  isEdit.value = false
  resetForm()
  form.parentId = parent?.id ?? null
  parentSelectData.value = treeData.value
  dialogVisible.value = true
}

function handleEdit(row: DepartmentVO) {
  isEdit.value = true
  resetForm()
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.parentId = row.parentId
  form.sort = row.sort
  // 编辑时排除自身及其子树作为可选父级，避免循环引用
  parentSelectData.value = filterSelfAndDescendants(treeData.value, row.id)
  dialogVisible.value = true
}

// 过滤掉自身及其所有子孙节点
function filterSelfAndDescendants(nodes: DepartmentVO[], excludeId: number): DepartmentVO[] {
  return nodes
    .filter(n => n.id !== excludeId)
    .map(n => ({
      ...n,
      children: n.children ? filterSelfAndDescendants(n.children, excludeId) : []
    }))
}

async function handleDelete(row: DepartmentVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除部门「${row.name}」？子部门将一并归到上级部门下。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteDepartment(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.code = ''
  form.parentId = null
  form.sort = 0
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value && form.id) {
        await updateDepartment(form.id, { ...form })
        ElMessage.success('修改成功')
      } else {
        await createDepartment({ ...form })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      ElMessage.error(isEdit.value ? '修改失败' : '新增失败')
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header__main {
  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0 0 6px;
  }
  .page-subtitle {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin: 0;
  }
}

.toolbar {
  display: flex;
  gap: 8px;
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.full-width {
  width: 100%;
}
</style>
