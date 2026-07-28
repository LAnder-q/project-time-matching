package com.pmtool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pmtool.dto.AssignmentDTO;
import com.pmtool.dto.AssignmentVO;
import com.pmtool.entity.Assignment;

import java.util.List;

/**
 * 人员项目分配服务接口
 */
public interface AssignmentService extends IService<Assignment> {

    /**
     * 按条件查询分配记录（含人员姓名、工号、项目名称等联表字段）
     *
     * @param personnelId 人员ID（可选）
     * @param projectId   项目ID（可选）
     * @return 分配列表（VO）
     */
    List<AssignmentVO> listByConditions(Long personnelId, Long projectId);

    /**
     * 创建分配（同时记录操作日志）
     *
     * @param dto 分配DTO
     * @return 创建后的分配
     */
    Assignment createAssignment(AssignmentDTO dto);

    /**
     * 更新分配（版本号+1，同时记录操作日志实现历史版本追溯）
     *
     * @param id  分配ID
     * @param dto 分配DTO
     * @return 更新后的分配
     */
    Assignment updateAssignment(Long id, AssignmentDTO dto);
}
