package com.pmtool.controller;

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
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取部门树形结构
     */
    @GetMapping("/tree")
    public Result<List<DepartmentVO>> tree() {
        return Result.ok(departmentService.getDepartmentTree());
    }

    /**
     * 查询全部部门（扁平列表，供下拉使用）
     */
    @GetMapping
    public Result<List<Department>> list() {
        return Result.ok(departmentService.list());
    }

    /**
     * 新增部门
     */
    @PostMapping
    public Result<Department> create(@RequestBody Department department) {
        departmentService.save(department);
        return Result.ok(department);
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    public Result<Department> update(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        departmentService.updateById(department);
        return Result.ok(department);
    }

    /**
     * 删除部门（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.removeById(id);
        return Result.ok(null);
    }
}
