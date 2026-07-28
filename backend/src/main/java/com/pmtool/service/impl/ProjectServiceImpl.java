package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Project;
import com.pmtool.mapper.ProjectMapper;
import com.pmtool.service.ProjectService;
import org.springframework.stereotype.Service;

/**
 * 项目服务实现
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Override
    public PageResult<Project> pageQuery(Integer pageNum, Integer pageSize, String name) {
        Page<Project> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Project::getName, name);
        }
        wrapper.orderByDesc(Project::getPriority);
        wrapper.orderByDesc(Project::getCreateTime);
        Page<Project> result = this.page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}
