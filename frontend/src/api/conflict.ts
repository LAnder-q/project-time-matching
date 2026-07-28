import { http } from './request'
import type { ConflictResult, CalendarEvent } from '@/types'

export interface CalendarQuery {
  personnelId?: number
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

export function detectAllConflicts(): Promise<ConflictResult[]> {
  return http.get<ConflictResult[]>('/conflict/detect')
}

// 后端实际接口为 GET /conflict/personnel/{personnelId}，返回单个 ConflictResult
export function detectConflictsByPersonnel(personnelId: number): Promise<ConflictResult> {
  return http.get<ConflictResult>(`/conflict/personnel/${personnelId}`)
}

// 获取全部冲突调优建议
export function getSuggestions(): Promise<ConflictSuggestion[]> {
  return http.get<ConflictSuggestion[]>('/conflict/suggestions')
}

export function getCalendarData(params?: CalendarQuery): Promise<CalendarEvent[]> {
  return http.get<CalendarEvent[]>('/conflict/calendar', { params })
}
