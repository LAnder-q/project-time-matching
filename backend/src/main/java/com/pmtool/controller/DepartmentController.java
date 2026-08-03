package com.pmtool.controller;

import com.pmtool.annotation.RequireRole;
import com.pmtool.common.Result;
import com.pmtool.dto.DepartmentVO;
import com.pmtool.entity.Department;
import com.pmtool.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 * 读操作（树/列表）所有角色可访问（供下拉筛选），写操作仅管理员
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取部门树形结构（所有角色可查，供下拉筛选使用）
     */
    @GetMapping("/tree")
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<List<DepartmentVO>> tree() {
        return Result.ok(departmentService.getDepartmentTree());
    }

    /**
     * 查询全部部门（扁平列表，供下拉使用）
     */
    @GetMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<List<Department>> list() {
        return Result.ok(departmentService.list());
    }

    /**
     * 新增部门（仅管理员）
     */
    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Department> create(@RequestBody Department department) {
        departmentService.save(department);
        return Result.ok(department);
    }

    /**
     * 更新部门（仅管理员）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Department> update(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        departmentService.updateById(department);
        return Result.ok(department);
    }

    /**
     * 删除部门（逻辑删除，仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.removeById(id);
        return Result.ok(null);
    }
}
