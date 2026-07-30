import { http } from './request'
import type { Personnel, PageResult } from '@/types'

export interface PersonnelPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  positions?: string
  deptId?: number
}

export function getPersonnelPage(params: PersonnelPageQuery): Promise<PageResult<Personnel>> {
  // 后端接口是 GET /personnel，参数名为 name（前端 keyword 映射到 name）
  const { pageNum, pageSize, keyword, positions, deptId } = params
  return http.get<PageResult<Personnel>>('/personnel', {
    params: {
      pageNum,
      pageSize,
      name: keyword || undefined,
      positions: positions || undefined,
      deptId: deptId || undefined
    }
  })
}

export function getPersonnelById(id: number): Promise<Personnel> {
  return http.get<Personnel>(`/personnel/${id}`)
}

export function getPersonnelAll(): Promise<Personnel[]> {
  return http.get<Personnel[]>('/personnel/list')
}

export function createPersonnel(data: Partial<Personnel>): Promise<Personnel> {
  return http.post<Personnel>('/personnel', data)
}

export function updatePersonnel(id: number, data: Partial<Personnel>): Promise<Personnel> {
  return http.put<Personnel>(`/personnel/${id}`, data)
}

export function deletePersonnel(id: number): Promise<void> {
  return http.delete<void>(`/personnel/${id}`)
}

export function batchImportPersonnel(data: Partial<Personnel>[]): Promise<number> {
  return http.post<number>('/personnel/batch', data)
}
