// 报表导出 API
// 使用 window.open 触发浏览器下载，token 通过 query 参数传递（后端 JwtInterceptor 已兼容此方式）

const REPORT_BASE_URL = '/api/report/export'

export type ReportType = 'assignment' | 'conflict' | 'utilization'
export type ExportFormat = 'xlsx' | 'pdf'

/**
 * 导出报表
 * @param type   报表类型：assignment(人员时间分配表) / conflict(冲突报表) / utilization(人员利用率统计)
 * @param format 导出格式：xlsx / pdf
 */
export function exportReport(type: ReportType, format: ExportFormat = 'xlsx'): void {
  const token = localStorage.getItem('token') || ''
  window.open(`${REPORT_BASE_URL}/${type}?format=${format}&token=${encodeURIComponent(token)}`, '_blank')
}
