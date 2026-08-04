// 后端统一返回体
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

// 登录响应
export interface LoginVO {
  token: string
  userId: number
  username: string
  realName: string
  role: string
}

// 分页结果
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// 人员
export interface Personnel {
  id?: number
  empNo: string
  name: string
  // 岗位（逗号分隔，支持身兼数职）
  positions: string
  skills: string
  // 所属部门ID
  deptId?: number | null
  availableStartDate: string
  availableEndDate: string
}

// 项目
export interface Project {
  id?: number
  name: string
  startDate: string
  endDate: string
  priority: number
  requiredPosition?: string
  dailyHours: number
  weeklyHours?: number
}

// 分配
export interface Assignment {
  id?: number
  personnelId: number
  projectId: number
  startDate: string
  endDate: string
  dailyHours: number
  version?: number
  operator?: string
  // 联表展示字段
  personnelName?: string
  projectName?: string
  empNo?: string
}

// 冲突详情
export interface ConflictDetail {
  projectId1: number
  projectName1: string
  projectId2: number
  projectName2: string
  overlapStart: string
  overlapEnd: string
  severity: string
}

// 冲突结果（按人员分组）
export interface ConflictResult {
  personnelId: number
  personnelName: string
  empNo: string
  conflicts: ConflictDetail[]
}

// 日历事件
export interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  color: string
  personnelId: number
  personnelName: string
  projectId: number
  projectName: string
  dailyHours: number
}
