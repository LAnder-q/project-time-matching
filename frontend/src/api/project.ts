import { http } from './request'
import type { Project, PageResult } from '@/types'

export interface ProjectPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
}

export function getProjectPage(params: ProjectPageQuery): Promise<PageResult<Project>> {
  // 后端接口是 GET /project，参数名为 name（前端 keyword 映射到 name）
  const { pageNum, pageSize, keyword } = params
  return http.get<PageResult<Project>>('/project', {
    params: { pageNum, pageSize, name: keyword || undefined }
  })
}

export function getProjectById(id: number): Promise<Project> {
  return http.get<Project>(`/project/${id}`)
}

export function getProjectAll(): Promise<Project[]> {
  return http.get<Project[]>('/project/list')
}

export function createProject(data: Partial<Project>): Promise<Project> {
  return http.post<Project>('/project', data)
}

export function updateProject(id: number, data: Partial<Project>): Promise<Project> {
  return http.put<Project>(`/project/${id}`, data)
}

export function deleteProject(id: number): Promise<void> {
  return http.delete<void>(`/project/${id}`)
}
