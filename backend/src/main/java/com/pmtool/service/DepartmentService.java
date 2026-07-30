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
}
