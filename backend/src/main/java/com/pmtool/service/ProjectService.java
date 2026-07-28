package com.pmtool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Project;

/**
 * 项目服务接口
 */
public interface ProjectService extends IService<Project> {

    /**
     * 分页查询项目
     *
     * @param pageNum  当前页
     * @param pageSize 每页大小
     * @param name     项目名称模糊查询
     * @return 分页结果
     */
    PageResult<Project> pageQuery(Integer pageNum, Integer pageSize, String name);
}
