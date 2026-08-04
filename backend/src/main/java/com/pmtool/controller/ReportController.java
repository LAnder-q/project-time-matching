package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.annotation.RequireRole;
import com.pmtool.entity.*;
import com.pmtool.mapper.OperationLogMapper;
import com.pmtool.service.*;
import com.pmtool.utils.PdfExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表导出 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 * 报表导出需管理员或项目负责人权限
 */
@RestController
@RequestMapping("/report")
@RequireRole({"ADMIN", "PROJECT_LEAD"})
public class ReportController {

    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private ConflictDetectionService conflictDetectionService;
    @Autowired
    private OperationLogMapper operationLogMapper;

    // ======================== 人员时间分配表 ========================

    @GetMapping("/export/assignment")
    public void exportAssignment(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) throws IOException {
        String title = "人员时间分配表";

        // 构建数据
        List<Personnel> allPersonnel = personnelService.list();
        // 按部门过滤人员ID集合
        final Set<Long> allowedPersonnelIds = deptId == null ? null : allPersonnel.stream()
                .filter(p -> deptId.equals(p.getDeptId()))
                .map(Personnel::getId)
                .collect(Collectors.toSet());
        List<Assignment> assignments = assignmentService.list();
        if (allowedPersonnelIds != null) {
            assignments = assignments.stream()
                    .filter(a -> allowedPersonnelIds.contains(a.getPersonnelId()))
                    .collect(Collectors.toList());
        }
        Map<Long, Personnel> personnelMap = allPersonnel.stream()
                .collect(Collectors.toMap(Personnel::getId, p -> p));
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 查询所有操作日志，用于统计调整记录
        List<OperationLog> allLogs = operationLogMapper.selectList(null);
        Map<Long, List<OperationLog>> logByEntityId = allLogs.stream()
                .filter(l -> "ASSIGNMENT".equals(l.getEntityType()))
                .collect(Collectors.groupingBy(OperationLog::getEntityId));

        List<String> headers = Arrays.asList("人员姓名", "工号", "项目名称", "开始日期", "结束日期", "每日工时", "版本号", "调整次数", "调整记录");
        List<List<String>> data = new ArrayList<>();
        for (Assignment a : assignments) {
            Personnel p = personnelMap.get(a.getPersonnelId());
            Project proj = projectMap.get(a.getProjectId());

            // 统计该分配的操作记录
            List<OperationLog> logs = logByEntityId.getOrDefault(a.getId(), Collections.emptyList());
            // 调整次数 = 非新增操作（UPDATE/DELETE）次数；新增不计入“调整”
            long adjustCount = logs.stream()
                    .filter(l -> l.getAction() != null && !l.getAction().toUpperCase().startsWith("CREATE"))
                    .count();
            // 构建调整记录摘要：操作类型 + 操作人 + 操作时间（归档前缀不展示）
            String adjustRecords = logs.stream()
                    .map(l -> String.format("[%s] %s %s",
                            l.getAction() != null ? l.getAction().replaceFirst("^ARCHIVED_", "") : "",
                            l.getOperator() != null ? l.getOperator() : "",
                            l.getOperateTime() != null ? l.getOperateTime().toLocalDate().toString() : ""))
                    .collect(Collectors.joining("; "));

            data.add(Arrays.asList(
                    p != null ? p.getName() : "",
                    p != null ? p.getEmpNo() : "",
                    proj != null ? proj.getName() : "",
                    a.getStartDate() != null ? a.getStartDate().toString() : "",
                    a.getEndDate() != null ? a.getEndDate().toString() : "",
                    a.getDailyHours() != null ? a.getDailyHours().toString() : "",
                    a.getVersion() != null ? a.getVersion().toString() : "",
                    String.valueOf(adjustCount),
                    adjustRecords
            ));
        }

        if ("pdf".equalsIgnoreCase(format)) {
            setPdfResponse(response, title);
            PdfExportUtils.writePdf(response.getOutputStream(), title, headers, data);
        } else {
            setExcelResponse(response, title);
            writeExcel(title, headers, data, response);
        }
    }

    // ======================== 项目人员冲突报表 ========================

    @GetMapping("/export/conflict")
    public void exportConflict(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) throws IOException {
        String title = "项目人员冲突报表";

        List<com.pmtool.dto.ConflictResult> conflicts = conflictDetectionService.detectAllConflicts(deptId);

        List<String> headers = Arrays.asList("人员姓名", "工号", "项目1", "项目2", "重叠开始日期", "重叠结束日期", "重叠天数", "严重程度", "人员总冲突次数");
        List<List<String>> data = new ArrayList<>();
        for (com.pmtool.dto.ConflictResult cr : conflicts) {
            int totalConflicts = cr.getConflicts().size();
            for (com.pmtool.dto.ConflictDetail cd : cr.getConflicts()) {
                // 计算重叠天数
                long overlapDays = 0;
                if (cd.getOverlapStart() != null && cd.getOverlapEnd() != null) {
                    overlapDays = ChronoUnit.DAYS.between(cd.getOverlapStart(), cd.getOverlapEnd()) + 1;
                }
                data.add(Arrays.asList(
                        cr.getPersonnelName(),
                        cr.getEmpNo(),
                        cd.getProjectName1(),
                        cd.getProjectName2(),
                        cd.getOverlapStart() != null ? cd.getOverlapStart().toString() : "",
                        cd.getOverlapEnd() != null ? cd.getOverlapEnd().toString() : "",
                        String.valueOf(overlapDays),
                        cd.getSeverity(),
                        String.valueOf(totalConflicts)
                ));
            }
        }

        if ("pdf".equalsIgnoreCase(format)) {
            setPdfResponse(response, title);
            PdfExportUtils.writePdf(response.getOutputStream(), title, headers, data);
        } else {
            setExcelResponse(response, title);
            writeExcel(title, headers, data, response);
        }
    }

    // ======================== 人员利用率统计报表 ========================

    @GetMapping("/export/utilization")
    public void exportUtilization(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) throws IOException {
        String title = "人员利用率统计报表";

        List<Personnel> personnelList = personnelService.list();
        // 按部门过滤人员
        if (deptId != null) {
            personnelList = personnelList.stream()
                    .filter(p -> deptId.equals(p.getDeptId()))
                    .collect(Collectors.toList());
        }
        List<Assignment> assignments = assignmentService.list();
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 检测冲突，统计每人冲突次数（按部门过滤以保持一致）
        List<com.pmtool.dto.ConflictResult> conflictResults = conflictDetectionService.detectAllConflicts(deptId);
        Map<Long, Integer> conflictCountMap = new HashMap<>();
        for (com.pmtool.dto.ConflictResult cr : conflictResults) {
            conflictCountMap.put(cr.getPersonnelId(), cr.getConflicts().size());
        }

        List<String> headers = Arrays.asList("人员姓名", "工号", "岗位", "技能", "分配项目数", "总分配天数", "总工时", "工时占比", "冲突次数", "涉及项目列表");
        List<List<String>> data = new ArrayList<>();
        for (Personnel p : personnelList) {
            List<Assignment> pAssignments = assignments.stream()
                    .filter(a -> a.getPersonnelId().equals(p.getId()))
                    .toList();
            long totalDays = 0;
            double totalHours = 0;
            Set<String> projectNames = new LinkedHashSet<>();
            for (Assignment a : pAssignments) {
                if (a.getStartDate() != null && a.getEndDate() != null) {
                    long days = ChronoUnit.DAYS.between(a.getStartDate(), a.getEndDate()) + 1;
                    totalDays += days;
                    totalHours += days * (a.getDailyHours() != null ? a.getDailyHours().doubleValue() : 0);
                }
                Project proj = projectMap.get(a.getProjectId());
                if (proj != null) projectNames.add(proj.getName());
            }

            // 计算工时占比：总工时 / 可用周期内标准工时（按可用日期范围计算）
            String utilizationRate = "0%";
            if (p.getAvailableStartDate() != null && p.getAvailableEndDate() != null) {
                long availableDays = ChronoUnit.DAYS.between(p.getAvailableStartDate(), p.getAvailableEndDate()) + 1;
                if (availableDays > 0) {
                    double standardHours = availableDays * 8.0; // 每天8小时标准工时
                    double rate = totalHours / standardHours * 100;
                    utilizationRate = String.format("%.1f%%", Math.min(rate, 999.9));
                }
            }

            int conflictCount = conflictCountMap.getOrDefault(p.getId(), 0);

            data.add(Arrays.asList(
                    p.getName(),
                    p.getEmpNo(),
                    p.getPositions() != null ? p.getPositions() : "",
                    p.getSkills() != null ? p.getSkills() : "",
                    String.valueOf(pAssignments.size()),
                    String.valueOf(totalDays),
                    String.valueOf(totalHours),
                    utilizationRate,
                    String.valueOf(conflictCount),
                    String.join(", ", projectNames)
            ));
        }

        if ("pdf".equalsIgnoreCase(format)) {
            setPdfResponse(response, title);
            PdfExportUtils.writePdf(response.getOutputStream(), title, headers, data);
        } else {
            setExcelResponse(response, title);
            writeExcel(title, headers, data, response);
        }
    }

    // ======================== 工具方法 ========================

    private void setExcelResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
    }

    private void setPdfResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/pdf");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded + ".pdf");
    }

    private void writeExcel(String sheetName, List<String> headers, List<List<String>> data,
                            HttpServletResponse response) throws IOException {
        List<List<String>> head = new ArrayList<>();
        for (String h : headers) {
            head.add(List.of(h));
        }
        List<List<Object>> excelData = new ArrayList<>();
        for (List<String> row : data) {
            List<Object> objRow = new ArrayList<>(row);
            excelData.add(objRow);
        }
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet(sheetName)
                .doWrite(excelData);
    }
}
