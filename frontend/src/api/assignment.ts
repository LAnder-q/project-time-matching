import { http } from './request'
import type { Assignment, PageResult } from '@/types'

export interface AssignmentQuery {
  personnelId?: number
  projectId?: number
}

export interface AssignmentPageQuery extends AssignmentQuery {
  pageNum: number
  pageSize: number
}

/**
 * 分页查询分配记录
 */
export function getAssignmentPage(params: AssignmentPageQuery): Promise<PageResult<Assignment>> {
  return http.get<PageResult<Assignment>>('/assignment/page', { params })
}

/**
 * 全量查询分配记录（用于日历、报表等不分页场景）
 */
export function getAssignmentList(params?: AssignmentQuery): Promise<Assignment[]> {
  // 后端接口是 GET /assignment（不是 /assignment/list）
  return http.get<Assignment[]>('/assignment', { params })
}

export function createAssignment(data: Partial<Assignment>): Promise<Assignment> {
  return http.post<Assignment>('/assignment', data)
}

export function updateAssignment(id: number, data: Partial<Assignment>): Promise<Assignment> {
  return http.put<Assignment>(`/assignment/${id}`, data)
}

export function deleteAssignment(id: number): Promise<void> {
  return http.delete<void>(`/assignment/${id}`)
}
