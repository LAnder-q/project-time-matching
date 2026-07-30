package com.pmtool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Personnel;

import java.util.List;

/**
 * 人员服务接口
 */
public interface PersonnelService extends IService<Personnel> {

    /**
     * 分页查询人员
     *
     * @param pageNum   当前页
     * @param pageSize  每页大小
     * @param name      姓名模糊查询
     * @param positions 岗位模糊查询（匹配 positions 多值字段）
     * @param deptId    部门ID精确筛选
     * @return 分页结果
     */
    PageResult<Personnel> pageQuery(Integer pageNum, Integer pageSize, String name, String positions, Long deptId);

    /**
     * 批量导入人员（工号存在则更新，不存在则新增）
     *
     * @param personnelList 人员列表
     * @return 导入后的列表（含ID）
     */
    List<Personnel> importPersonnel(List<Personnel> personnelList);
}
