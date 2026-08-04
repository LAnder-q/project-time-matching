import { http } from './request'
import type { PageResult } from '@/types'
import type { OperationLog } from './assignment'

export interface OperationLogQuery {
  pageNum: number
  pageSize: number
  operator?: string
  action?: string
  personnelId?: number
}

export type OperationLogExportFormat = 'xlsx' | 'pdf'

/**
 * 分页查询全局操作日志（可按操作人、操作类型、涉及人员筛选）
 */
export function getOperationLogPage(params: OperationLogQuery): Promise<PageResult<OperationLog>> {
  return http.get<PageResult<OperationLog>>('/operation-log/page', { params })
}

/**
 * 导出操作日志（Excel / PDF），筛选条件与页面一致
 * 使用 window.open 触发浏览器下载，token 通过 query 参数传递
 */
export function exportOperationLog(
  format: OperationLogExportFormat,
  params: { operator?: string; action?: string; personnelId?: number }
): void {
  const token = localStorage.getItem('token') || ''
  const query = new URLSearchParams({ format, token })
  if (params.operator) query.set('operator', params.operator)
  if (params.action) query.set('action', params.action)
  if (params.personnelId) query.set('personnelId', String(params.personnelId))
  window.open(`/api/operation-log/export?${query.toString()}`, '_blank')
}
