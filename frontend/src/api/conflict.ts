import { http } from './request'
import type { ConflictResult, CalendarEvent } from '@/types'

export interface CalendarQuery {
  personnelId?: number
  projectId?: number
  deptId?: number
  startDate?: string
  endDate?: string
}

// 调优建议中的替代候选人
export interface ReplacementCandidate {
  personnelId: number
  name: string
  empNo: string
  position: string
  skills: string
  matchScore: number
  availabilityRate: number
  recommendationLevel: 'RECOMMENDED' | 'CONSIDERABLE' | 'NOT_RECOMMENDED'
  reason: string
}

// 冲突调优建议
export interface ConflictSuggestion {
  personnelId: number
  personnelName: string
  empNo: string
  conflictAssignmentId: number
  projectId: number
  projectName: string
  suggestionType: 'REPLACE_PERSONNEL' | 'ADJUST_HOURS'
  description: string
  candidates?: ReplacementCandidate[]
  adjustAdvice?: string
}

export function detectAllConflicts(deptId?: number): Promise<ConflictResult[]> {
  return http.get<ConflictResult[]>('/conflict/detect', { params: { deptId: deptId || undefined } })
}

// 后端实际接口为 GET /conflict/personnel/{personnelId}，返回单个 ConflictResult
export function detectConflictsByPersonnel(personnelId: number): Promise<ConflictResult> {
  return http.get<ConflictResult>(`/conflict/personnel/${personnelId}`)
}

// 获取全部冲突调优建议（可按部门过滤）
export function getSuggestions(deptId?: number): Promise<ConflictSuggestion[]> {
  return http.get<ConflictSuggestion[]>('/conflict/suggestions', { params: { deptId: deptId || undefined } })
}

export function getCalendarData(params?: CalendarQuery): Promise<CalendarEvent[]> {
  return http.get<CalendarEvent[]>('/conflict/calendar', { params })
}
