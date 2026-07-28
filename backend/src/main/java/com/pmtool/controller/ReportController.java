package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.entity.*;
import com.pmtool.service.*;
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

    // 导出重复使用人员时间分配表
    @GetMapping("/export/assignment")
    public void exportAssignment(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("人员时间分配表", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 构建数据：每个分配记录一行，包含人员名和项目名
        List<Assignment> assignments = assignmentService.list();
        // 需要联表查人员名和项目名
        Map<Long, Personnel> personnelMap = personnelService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Personnel::getId, p -> p));
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        List<List<String>> head = new ArrayList<>();
        head.add(List.of("人员姓名"));
        head.add(List.of("工号"));
        head.add(List.of("项目名称"));
        head.add(List.of("开始日期"));
        head.add(List.of("结束日期"));
        head.add(List.of("每日工时"));
        head.add(List.of("版本号"));

        List<List<Object>> data = new ArrayList<>();
        for (Assignment a : assignments) {
            List<Object> row = new ArrayList<>();
            Personnel p = personnelMap.get(a.getPersonnelId());
            Project proj = projectMap.get(a.getProjectId());
            row.add(p != null ? p.getName() : "");
            row.add(p != null ? p.getEmpNo() : "");
            row.add(proj != null ? proj.getName() : "");
            row.add(a.getStartDate() != null ? a.getStartDate().toString() : "");
            row.add(a.getEndDate() != null ? a.getEndDate().toString() : "");
            row.add(a.getDailyHours());
            row.add(a.getVersion());
            data.add(row);
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("人员时间分配")
                .doWrite(data);
    }

    // 导出项目人员冲突报表
    @GetMapping("/export/conflict")
    public void exportConflict(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("项目人员冲突报表", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<com.pmtool.dto.ConflictResult> conflicts = conflictDetectionService.detectAllConflicts();

        List<List<String>> head = new ArrayList<>();
        head.add(List.of("人员姓名"));
        head.add(List.of("工号"));
        head.add(List.of("项目1"));
        head.add(List.of("项目2"));
        head.add(List.of("重叠开始日期"));
        head.add(List.of("重叠结束日期"));
        head.add(List.of("严重程度"));

        List<List<Object>> data = new ArrayList<>();
        for (com.pmtool.dto.ConflictResult cr : conflicts) {
            for (com.pmtool.dto.ConflictDetail cd : cr.getConflicts()) {
                List<Object> row = new ArrayList<>();
                row.add(cr.getPersonnelName());
                row.add(cr.getEmpNo());
                row.add(cd.getProjectName1());
                row.add(cd.getProjectName2());
                row.add(cd.getOverlapStart() != null ? cd.getOverlapStart().toString() : "");
                row.add(cd.getOverlapEnd() != null ? cd.getOverlapEnd().toString() : "");
                row.add(cd.getSeverity());
                data.add(row);
            }
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("冲突报表")
                .doWrite(data);
    }

    // 导出人员利用率统计报表
    @GetMapping("/export/utilization")
    public void exportUtilization(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("人员利用率统计报表", StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<Personnel> personnelList = personnelService.list();
        List<Assignment> assignments = assignmentService.list();
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

        List<List<String>> head = new ArrayList<>();
        head.add(List.of("人员姓名"));
        head.add(List.of("工号"));
        head.add(List.of("岗位"));
        head.add(List.of("技能"));
        head.add(List.of("分配项目数"));
        head.add(List.of("总分配天数"));
        head.add(List.of("总工时"));
        head.add(List.of("涉及项目列表"));

        List<List<Object>> data = new ArrayList<>();
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
            List<Object> row = new ArrayList<>();
            row.add(p.getName());
            row.add(p.getEmpNo());
            row.add(p.getPosition() != null ? p.getPosition() : "");
            row.add(p.getSkills() != null ? p.getSkills() : "");
            row.add(pAssignments.size());
            row.add(totalDays);
            row.add(totalHours);
            row.add(String.join(", ", projectNames));
            data.add(row);
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("人员利用率")
                .doWrite(data);
    }
}
