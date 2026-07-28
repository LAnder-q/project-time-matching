package com.pmtool.controller;

import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.entity.Project;
import com.pmtool.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 分页查询项目
     */
    @GetMapping
    public Result<PageResult<Project>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        return Result.ok(projectService.pageQuery(pageNum, pageSize, name));
    }

    /**
     * 查询全部项目（不分页，供下拉选择使用）
     */
    @GetMapping("/list")
    public Result<List<Project>> list() {
        return Result.ok(projectService.list());
    }

    /**
     * 根据ID查询项目
     */
    @GetMapping("/{id}")
    public Result<Project> getById(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }

    /**
     * 新增项目
     */
    @PostMapping
    public Result<Project> create(@RequestBody Project project) {
        projectService.save(project);
        return Result.ok(project);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public Result<Project> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateById(project);
        return Result.ok(project);
    }

    /**
     * 删除项目（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return Result.ok(null);
    }
}
