package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.annotation.RequireRole;
import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.dto.ProjectImportDTO;
import com.pmtool.entity.Project;
import com.pmtool.service.ProjectService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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
     * 分页查询项目（所有角色可查）
     */
    @GetMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
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
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<List<Project>> list() {
        return Result.ok(projectService.list());
    }

    /**
     * 根据ID查询项目
     */
    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<Project> getById(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }

    /**
     * 新增项目（管理员、项目负责人）
     */
    @PostMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Project> create(@RequestBody Project project) {
        projectService.save(project);
        return Result.ok(project);
    }

    /**
     * 更新项目（管理员、项目负责人）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Project> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateById(project);
        return Result.ok(project);
    }

    /**
     * 删除项目（逻辑删除，仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return Result.ok(null);
    }

    /**
     * 批量导入项目 - JSON 方式（管理员、项目负责人）
     * 项目名存在则更新，不存在则新增
     */
    @PostMapping("/batch")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<List<Project>> batchImport(@RequestBody List<Project> projectList) {
        return Result.ok(projectService.importProjects(projectList));
    }

    /**
     * 批量导入项目 - Excel 文件上传（管理员、项目负责人）
     * 接受 .xlsx 文件，使用 EasyExcel 解析
     */
    @PostMapping("/import-excel")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Integer> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            return Result.fail("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
        }

        // 使用 EasyExcel 解析上传的文件
        List<ProjectImportDTO> importList = EasyExcel.read(file.getInputStream())
                .head(ProjectImportDTO.class)
                .sheet()
                .doReadSync();

        if (importList.isEmpty()) {
            return Result.fail("文件中没有可导入的数据");
        }

        // 转换为 Project 实体
        List<Project> projectList = new ArrayList<>();
        for (ProjectImportDTO dto : importList) {
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                continue; // 跳过名称为空的行
            }
            Project p = new Project();
            p.setName(dto.getName().trim());

            if (dto.getStartDate() != null && !dto.getStartDate().trim().isEmpty()) {
                p.setStartDate(LocalDate.parse(dto.getStartDate().trim()));
            }
            if (dto.getEndDate() != null && !dto.getEndDate().trim().isEmpty()) {
                p.setEndDate(LocalDate.parse(dto.getEndDate().trim()));
            }
            p.setPriority(dto.getPriority() != null ? dto.getPriority() : 3);
            p.setRequiredPosition(dto.getRequiredPosition() != null ? dto.getRequiredPosition().trim() : null);
            p.setDailyHours(dto.getDailyHours() != null ? dto.getDailyHours() : new BigDecimal("8.0"));
            p.setWeeklyHours(dto.getWeeklyHours() != null ? dto.getWeeklyHours() : new BigDecimal("40.0"));

            projectList.add(p);
        }

        if (projectList.isEmpty()) {
            return Result.fail("文件中没有有效的数据行（项目名称不能为空）");
        }

        List<Project> result = projectService.importProjects(projectList);
        return Result.ok(result.size());
    }

    /**
     * 下载项目导入模板（管理员、项目负责人）
     */
    @GetMapping("/import-template")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("项目导入模板", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 构建模板数据（含示例行）
        List<ProjectImportDTO> templateData = new ArrayList<>();
        templateData.add(new ProjectImportDTO() {{
            setName("电商平台升级");
            setStartDate("2026-02-01");
            setEndDate("2026-06-30");
            setPriority(5);
            setRequiredPosition("运维工程师,DBA");
            setDailyHours(new BigDecimal("8.0"));
            setWeeklyHours(new BigDecimal("40.0"));
        }});

        EasyExcel.write(response.getOutputStream())
                .head(ProjectImportDTO.class)
                .sheet("项目导入模板")
                .doWrite(templateData);
    }
}
