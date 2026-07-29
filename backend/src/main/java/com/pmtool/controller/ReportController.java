package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.entity.*;
import com.pmtool.service.*;
import com.pmtool.utils.PdfExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private ConflictDetectionService conflictDetectionService;

    // ======================== 人员时间分配表 ========================

    @GetMapping("/export/assignment")
    public void exportAssignment(
            @RequestParam(defaultValue = "xlsx") String format,
            HttpServletResponse response) throws IOException {
        String title = "人员时间分配表";

        // 构建数据
        List<Assignment> assignments = assignmentService.list();
        Map<Long, Personnel> personnelMap = personnelService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Personnel::getId, p -> p));
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        List<String> headers = Arrays.asList("人员姓名", "工号", "项目名称", "开始日期", "结束日期", "每日工时", "版本号");
        List<List<String>> data = new ArrayList<>();
        for (Assignment a : assignments) {
            Personnel p = personnelMap.get(a.getPersonnelId());
            Project proj = projectMap.get(a.getProjectId());
            data.add(Arrays.asList(
                    p != null ? p.getName() : "",
                    p != null ? p.getEmpNo() : "",
                    proj != null ? proj.getName() : "",
                    a.getStartDate() != null ? a.getStartDate().toString() : "",
                    a.getEndDate() != null ? a.getEndDate().toString() : "",
                    a.getDailyHours() != null ? a.getDailyHours().toString() : "",
                    a.getVersion() != null ? a.getVersion().toString() : ""
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
            HttpServletResponse response) throws IOException {
        String title = "项目人员冲突报表";

        List<com.pmtool.dto.ConflictResult> conflicts = conflictDetectionService.detectAllConflicts();

        List<String> headers = Arrays.asList("人员姓名", "工号", "项目1", "项目2", "重叠开始日期", "重叠结束日期", "严重程度");
        List<List<String>> data = new ArrayList<>();
        for (com.pmtool.dto.ConflictResult cr : conflicts) {
            for (com.pmtool.dto.ConflictDetail cd : cr.getConflicts()) {
                data.add(Arrays.asList(
                        cr.getPersonnelName(),
                        cr.getEmpNo(),
                        cd.getProjectName1(),
                        cd.getProjectName2(),
                        cd.getOverlapStart() != null ? cd.getOverlapStart().toString() : "",
                        cd.getOverlapEnd() != null ? cd.getOverlapEnd().toString() : "",
                        cd.getSeverity()
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
            HttpServletResponse response) throws IOException {
        String title = "人员利用率统计报表";

        List<Personnel> personnelList = personnelService.list();
        List<Assignment> assignments = assignmentService.list();
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        List<String> headers = Arrays.asList("人员姓名", "工号", "岗位", "技能", "分配项目数", "总分配天数", "总工时", "涉及项目列表");
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
                    long days = java.time.temporal.ChronoUnit.DAYS.between(a.getStartDate(), a.getEndDate()) + 1;
                    totalDays += days;
                    totalHours += days * (a.getDailyHours() != null ? a.getDailyHours().doubleValue() : 0);
                }
                Project proj = projectMap.get(a.getProjectId());
                if (proj != null) projectNames.add(proj.getName());
            }
            data.add(Arrays.asList(
                    p.getName(),
                    p.getEmpNo(),
                    p.getPosition() != null ? p.getPosition() : "",
                    p.getSkills() != null ? p.getSkills() : "",
                    String.valueOf(pAssignments.size()),
                    String.valueOf(totalDays),
                    String.valueOf(totalHours),
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
