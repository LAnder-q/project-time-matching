package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.annotation.RequireRole;
import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.dto.PersonnelImportDTO;
import com.pmtool.entity.Personnel;
import com.pmtool.service.DepartmentService;
import com.pmtool.service.PersonnelService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 人员管理 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 */
@RestController
@RequestMapping("/personnel")
public class PersonnelController {

    @Autowired
    private PersonnelService personnelService;

    @Autowired
    private DepartmentService departmentService;

    /**
     * 分页查询人员（所有角色可查）
     */
    @GetMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<PageResult<Personnel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String positions,
            @RequestParam(required = false) Long deptId) {
        return Result.ok(personnelService.pageQuery(pageNum, pageSize, name, positions, deptId));
    }

    /**
     * 查询全部人员（不分页，供下拉选择使用）
     */
    @GetMapping("/list")
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<List<Personnel>> list() {
        return Result.ok(personnelService.list());
    }

    /**
     * 根据ID查询人员
     */
    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "PROJECT_LEAD", "USER"})
    public Result<Personnel> getById(@PathVariable Long id) {
        return Result.ok(personnelService.getById(id));
    }

    /**
     * 新增人员（管理员、项目负责人）
     */
    @PostMapping
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Personnel> create(@RequestBody Personnel personnel) {
        personnelService.save(personnel);
        return Result.ok(personnel);
    }

    /**
     * 更新人员（管理员、项目负责人）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<Personnel> update(@PathVariable Long id, @RequestBody Personnel personnel) {
        personnel.setId(id);
        personnelService.updateById(personnel);
        return Result.ok(personnel);
    }

    /**
     * 删除人员（逻辑删除，仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        personnelService.removeById(id);
        return Result.ok(null);
    }

    /**
     * 批量导入人员 - JSON 方式（管理员、项目负责人）
     * 工号存在则更新，不存在则新增
     */
    @PostMapping("/batch")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public Result<List<Personnel>> batchImport(@RequestBody List<Personnel> personnelList) {
        return Result.ok(personnelService.importPersonnel(personnelList));
    }

    /**
     * 批量导入人员 - Excel 文件上传（管理员、项目负责人）
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
        List<PersonnelImportDTO> importList = EasyExcel.read(file.getInputStream())
                .head(PersonnelImportDTO.class)
                .sheet()
                .doReadSync();

        if (importList.isEmpty()) {
            return Result.fail("文件中没有可导入的数据");
        }

        // 转换为 Personnel 实体
        List<Personnel> personnelList = new ArrayList<>();
        for (PersonnelImportDTO dto : importList) {
            if (dto.getEmpNo() == null || dto.getEmpNo().trim().isEmpty()) {
                continue; // 跳过工号为空的行
            }
            Personnel p = new Personnel();
            p.setEmpNo(dto.getEmpNo().trim());
            p.setName(dto.getName() != null ? dto.getName().trim() : "");
            p.setPositions(dto.getPositions() != null ? dto.getPositions().trim() : "");
            p.setSkills(dto.getSkills() != null ? dto.getSkills().trim() : "");

            // 部门按名称匹配
            if (dto.getDeptName() != null && !dto.getDeptName().trim().isEmpty()) {
                Long deptId = departmentService.findIdByName(dto.getDeptName().trim());
                p.setDeptId(deptId);
            }

            // 日期解析
            if (dto.getAvailableStartDate() != null && !dto.getAvailableStartDate().trim().isEmpty()) {
                p.setAvailableStartDate(LocalDate.parse(dto.getAvailableStartDate().trim()));
            }
            if (dto.getAvailableEndDate() != null && !dto.getAvailableEndDate().trim().isEmpty()) {
                p.setAvailableEndDate(LocalDate.parse(dto.getAvailableEndDate().trim()));
            }

            personnelList.add(p);
        }

        if (personnelList.isEmpty()) {
            return Result.fail("文件中没有有效的数据行（工号不能为空）");
        }

        List<Personnel> result = personnelService.importPersonnel(personnelList);
        return Result.ok(result.size());
    }

    /**
     * 下载人员导入模板（管理员、项目负责人）
     */
    @GetMapping("/import-template")
    @RequireRole({"ADMIN", "PROJECT_LEAD"})
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("人员导入模板", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 构建模板数据（含示例行）
        List<PersonnelImportDTO> templateData = new ArrayList<>();
        templateData.add(new PersonnelImportDTO() {{
            setEmpNo("EMP001");
            setName("张伟");
            setDeptName("基础运维组");
            setPositions("运维工程师");
            setSkills("Linux,Docker,Kubernetes");
            setAvailableStartDate("2026-01-01");
            setAvailableEndDate("2026-12-31");
        }});

        EasyExcel.write(response.getOutputStream())
                .head(PersonnelImportDTO.class)
                .sheet("人员导入模板")
                .doWrite(templateData);
    }
}
