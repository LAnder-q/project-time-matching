package com.pmtool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pmtool.dto.DepartmentVO;
import com.pmtool.entity.Department;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 获取部门树形结构（递归构建）
     *
     * @return 顶级部门列表，每个节点含 children
     */
    List<DepartmentVO> getDepartmentTree();

    /**
     * 根据部门名称查找部门ID（用于 Excel 导入时按名称匹配部门）
     *
     * @param name 部门名称
     * @return 部门ID，未找到返回 null
     */
    Long findIdByName(String name);
}
