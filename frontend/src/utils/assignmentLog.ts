// 分配操作日志的解析与展示工具（分配管理历史弹窗、操作日志页共用）

export interface LogNameResolver {
  personnel: (id: unknown) => string
  project: (id: unknown) => string
}

export function parseAssignJson(raw?: string | null): Record<string, unknown> {
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return {}
  }
}

const fieldLabels: Record<string, string> = {
  personnelId: '人员',
  projectId: '项目',
  startDate: '开始日期',
  endDate: '结束日期',
  dailyHours: '每日工时'
}

export function formatAssignField(key: string, value: unknown, resolver: LogNameResolver): string {
  if (value === null || value === undefined || value === '') return '空'
  if (key === 'personnelId') {
    const name = resolver.personnel(value)
    return name || `#${value}`
  }
  if (key === 'projectId') {
    const name = resolver.project(value)
    return name || `#${value}`
  }
  // 早期版本日志中的日期存的是毫秒时间戳，统一转回 YYYY-MM-DD
  if ((key === 'startDate' || key === 'endDate') && typeof value === 'number') {
    const d = new Date(value)
    if (!Number.isNaN(d.getTime())) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
  }
  return String(value)
}

export function describeAssignChange(
  action: string,
  oldObj: Record<string, unknown>,
  newObj: Record<string, unknown>,
  resolver: LogNameResolver
): string {
  const normAction = action.replace(/^ARCHIVED_/, '')
  if (normAction === 'CREATE') {
    return (
      `新增分配：${formatAssignField('personnelId', newObj.personnelId, resolver)} → ` +
      `${formatAssignField('projectId', newObj.projectId, resolver)}，` +
      `${formatAssignField('startDate', newObj.startDate, resolver)} ~ ` +
      `${formatAssignField('endDate', newObj.endDate, resolver)}，` +
      `每日 ${formatAssignField('dailyHours', newObj.dailyHours, resolver)} 小时`
    )
  }
  if (normAction === 'DELETE') {
    return (
      `删除分配：${formatAssignField('personnelId', oldObj.personnelId, resolver)} → ` +
      `${formatAssignField('projectId', oldObj.projectId, resolver)}`
    )
  }
  const fields = ['personnelId', 'projectId', 'startDate', 'endDate', 'dailyHours']
  // 比较“格式化后的值”，兼容旧日志毫秒时间戳与现日志 ISO 字符串（避免同一天被误判为变更）
  const diffs = fields.filter(
    (k) => formatAssignField(k, oldObj[k], resolver) !== formatAssignField(k, newObj[k], resolver)
  )
  // 项目未变化时，把项目名作为上下文前缀带上，避免“人员：A → B”看不出是哪个项目
  const projectName = formatAssignField('projectId', newObj.projectId ?? oldObj.projectId, resolver)
  if (diffs.length === 0) {
    return projectName !== '空' ? `项目「${projectName}」：分配信息更新` : '分配信息更新'
  }
  const projectPrefix = diffs.includes('projectId') ? '' : `项目「${projectName}」；`
  return (
    projectPrefix +
    diffs
      .map(
        (k) =>
          `${fieldLabels[k] || k}：${formatAssignField(k, oldObj[k], resolver)} → ${formatAssignField(
            k,
            newObj[k],
            resolver
          )}`
      )
      .join('；')
  )
}

export function assignLogVersion(
  action: string,
  oldObj: Record<string, unknown>,
  newObj: Record<string, unknown>
): number | string {
  const normAction = action.replace(/^ARCHIVED_/, '')
  const v = normAction === 'DELETE' ? oldObj.version : newObj.version
  return typeof v === 'number' ? v : v != null ? String(v) : '-'
}

export function logActionText(action: string): string {
  const a = action.replace(/^ARCHIVED_/, '')
  if (a === 'CREATE') return '新增'
  if (a === 'DELETE') return '删除'
  return '修改'
}

export function logActionTagType(action: string): 'success' | 'warning' | 'danger' | 'info' {
  const a = action.replace(/^ARCHIVED_/, '')
  if (a === 'CREATE') return 'success'
  if (a === 'DELETE') return 'danger'
  return 'warning'
}
