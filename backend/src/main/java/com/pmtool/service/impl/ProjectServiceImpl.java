package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Project;
import com.pmtool.mapper.ProjectMapper;
import com.pmtool.service.ProjectService;
import com.pmtool.utils.DataEncryptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 * 敏感字段（项目名称）使用 AES 加密存储，读取时自动解密
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    /**
     * 加密项目敏感字段（保存前调用）
     */
    private void encryptFields(Project project) {
        if (project == null) return;
        if (project.getName() != null && !project.getName().isEmpty()) {
            project.setName(DataEncryptUtils.encrypt(project.getName()));
        }
    }

    /**
     * 解密项目敏感字段（读取后调用）
     */
    private void decryptFields(Project project) {
        if (project == null) return;
        if (project.getName() != null && !project.getName().isEmpty()) {
            project.setName(DataEncryptUtils.decrypt(project.getName()));
        }
    }

    @Override
    public PageResult<Project> pageQuery(Integer pageNum, Integer pageSize, String name) {
        boolean hasName = name != null && !name.trim().isEmpty();
        if (!hasName) {
            Page<Project> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(Project::getPriority);
            wrapper.orderByDesc(Project::getCreateTime);
            Page<Project> result = this.page(page, wrapper);
            result.getRecords().forEach(this::decryptFields);
            return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
        }

        // 项目名称已加密存储，无法使用 SQL LIKE 模糊查询，
        // 改为全量解密后内存过滤（项目规模小，文档要求 ≤50 个项目，性能可接受）
        String keyword = name.trim();
        Comparator<Project> comparator = Comparator
                .comparing((Project p) -> p.getPriority() == null ? Integer.MIN_VALUE : p.getPriority(),
                        Comparator.reverseOrder())
                .thenComparing(p -> p.getCreateTime() == null ? LocalDateTime.MIN : p.getCreateTime(),
                        Comparator.reverseOrder());
        List<Project> filtered = this.list().stream()
                .filter(p -> p.getName() != null && p.getName().contains(keyword))
                .sorted(comparator)
                .collect(Collectors.toList());
        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return PageResult.of(new ArrayList<>(filtered.subList(from, to)),
                (long) filtered.size(), (long) pageNum, (long) pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Project> importProjects(List<Project> projectList) {
        List<Project> result = new ArrayList<>();
        for (Project project : projectList) {
            // 项目名称已加密存储，查重时对查询值做同等加密；同时兼容历史明文数据
            LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Project::getName, DataEncryptUtils.encrypt(project.getName()))
                    .or()
                    .eq(Project::getName, project.getName());
            Project existing = this.getOne(wrapper, false);
            if (existing != null) {
                project.setId(existing.getId());
                // 由 updateById 统一处理加密与返回前解密
                this.updateById(project);
            } else {
                // 由 save 统一处理加密与返回前解密
                this.save(project);
            }
            result.add(project);
        }
        return result;
    }

    @Override
    public boolean save(Project project) {
        encryptFields(project);
        boolean ok = super.save(project);
        // 返回前解密，避免接口响应携带密文
        decryptFields(project);
        return ok;
    }

    @Override
    public boolean updateById(Project project) {
        encryptFields(project);
        boolean ok = super.updateById(project);
        // 返回前解密，避免接口响应携带密文
        decryptFields(project);
        return ok;
    }

    @Override
    public Project getById(Serializable id) {
        Project project = super.getById(id);
        decryptFields(project);
        return project;
    }

    @Override
    public List<Project> list() {
        List<Project> list = super.list();
        list.forEach(this::decryptFields);
        return list;
    }

    @Override
    public List<Project> listByIds(Collection<? extends Serializable> idList) {
        List<Project> list = super.listByIds(idList);
        list.forEach(this::decryptFields);
        return list;
    }
}
