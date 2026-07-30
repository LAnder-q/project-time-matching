import { http } from './request'

// 部门树形 VO
export interface DepartmentVO {
  id: number
  name: string
  code: string
  parentId: number | null
  sort: number
  children: DepartmentVO[]
}

// 部门实体（用于新增/编辑/扁平列表）
export interface Department {
  id?: number
  name: string
  code?: string
  parentId?: number | null
  sort?: number
}

// 获取部门树
export function getDepartmentTree(): Promise<DepartmentVO[]> {
  return http.get<DepartmentVO[]>('/department/tree')
}

// 获取全部部门（扁平列表）
export function getDepartmentAll(): Promise<Department[]> {
  return http.get<Department[]>('/department')
}

// 新增部门
export function createDepartment(data: Partial<Department>): Promise<Department> {
  return http.post<Department>('/department', data)
}

// 更新部门
export function updateDepartment(id: number, data: Partial<Department>): Promise<Department> {
  return http.put<Department>(`/department/${id}`, data)
}

// 删除部门
export function deleteDepartment(id: number): Promise<void> {
  return http.delete<void>(`/department/${id}`)
}

// 将树形部门扁平化为列表（便于 el-tree-select 节点展示）
export function flattenDepartments(tree: DepartmentVO[]): DepartmentVO[] {
  const result: DepartmentVO[] = []
  const walk = (nodes: DepartmentVO[]) => {
    for (const n of nodes) {
      result.push(n)
      if (n.children && n.children.length > 0) {
        walk(n.children)
      }
    }
  }
  walk(tree)
  return result
}
