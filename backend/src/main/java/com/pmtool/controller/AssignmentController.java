package com.pmtool.controller;

import com.pmtool.annotation.RequireRole;
import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.dto.AssignmentDTO;
import com.pmtool.dto.AssignmentVO;
import com.pmtool.entity.Assignment;
import com.pmtool.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员项目分配 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 * 读操作所有角色可访问，写操作需管理员或项目负责人
 */
@RestController
@RequestMapping("/assignment")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    /**
     * 分页查询分配记录（支持按人员ID、项目ID筛选，含人员姓名、项目名称等联表字段）
     */
    @GetMapping("/page")
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<PageResult<AssignmentVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) Long projectId) {
        return Result.ok(assignmentService.pageByConditions(pageNum, pageSize, personnelId, projectId));
    }

    /**
     * 查询全部分配记录（支持按人员ID、项目ID筛选，含人员姓名、项目名称等联表字段）
     * 用于日历、报表等不需要分页的场景
     */
    @GetMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<List<AssignmentVO>> list(
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) Long projectId) {
        return Result.ok(assignmentService.listByConditions(personnelId, projectId));
    }

    /**
     * 创建分配（同时记录操作日志）
     */
    @PostMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Assignment> create(@RequestBody AssignmentDTO dto) {
        return Result.ok(assignmentService.createAssignment(dto));
    }

    /**
     * 更新分配（版本号+1，记录操作日志实现历史版本追溯）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Assignment> update(@PathVariable Long id, @RequestBody AssignmentDTO dto) {
        return Result.ok(assignmentService.updateAssignment(id, dto));
    }

    /**
     * 删除分配（逻辑删除，仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        assignmentService.removeById(id);
        return Result.ok(null);
    }
}
