package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmtool.annotation.RequireRole;
import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.entity.OperationLog;
import com.pmtool.entity.Personnel;
import com.pmtool.entity.Project;
import com.pmtool.mapper.OperationLogMapper;
import com.pmtool.service.PersonnelService;
import com.pmtool.service.ProjectService;
import com.pmtool.utils.PdfExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志 Controller
 * 提供全局调整历史查询与导出（管理员、项目负责人可访问）
 */
@RestController
@RequestMapping("/operation-log")
@RequireRole({"ADMIN", "PROJECT_LEAD"})
public class OperationLogController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> DIFF_FIELDS = Arrays.asList("personnelId", "projectId", "startDate", "endDate", "dailyHours");
    private static final Map<String, String> FIELD_LABELS = new HashMap<>();

    static {
        FIELD_LABELS.put("personnelId", "人员");
        FIELD_LABELS.put("projectId", "项目");
        FIELD_LABELS.put("startDate", "开始日期");
        FIELD_LABELS.put("endDate", "结束日期");
        FIELD_LABELS.put("dailyHours", "每日工时");
    }

    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private PersonnelService personnelService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分页查询操作日志
     *
     * @param operator    操作人（模糊）
     * @param action      操作类型（CREATE/UPDATE/DELETE，模糊）
     * @param personnelId 涉及人员ID（通过日志 JSON 快照匹配，含被替换前的原人员）
     */
    @GetMapping("/page")
    public Result<PageResult<OperationLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long personnelId) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = buildWrapper(operator, action, personnelId);
        wrapper.orderByDesc(OperationLog::getOperateTime);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()));
    }

    /**
     * 导出操作日志（Excel / PDF），筛选条件与页面一致
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long personnelId,
            HttpServletResponse response) throws IOException {
        String title = "操作日志";

        LambdaQueryWrapper<OperationLog> wrapper = buildWrapper(operator, action, personnelId);
        wrapper.orderByDesc(OperationLog::getOperateTime);
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        // 预加载人员/项目名称，用于把 JSON 快照中的 ID 翻译成可读名称
        Map<Long, String> personnelNameMap = personnelService.list().stream()
                .collect(Collectors.toMap(Personnel::getId, Personnel::getName));
        Map<Long, String> projectNameMap = projectService.list().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));

        List<String> headers = Arrays.asList("操作类型", "操作时间", "操作人", "版本号", "分配ID", "变更内容");
        List<List<String>> data = new ArrayList<>();
        for (OperationLog log : logs) {
            JsonNode oldNode = parse(log.getOldValue());
            JsonNode newNode = parse(log.getNewValue());
            data.add(Arrays.asList(
                    actionText(log.getAction()),
                    formatTime(log.getOperateTime()),
                    log.getOperator() != null ? log.getOperator() : "",
                    versionText(log.getAction(), oldNode, newNode),
                    log.getEntityId() != null ? String.valueOf(log.getEntityId()) : "",
                    changeDescription(log.getAction(), oldNode, newNode, personnelNameMap, projectNameMap)
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

    /**
     * 构建通用筛选条件（分页与导出共用）
     */
    private LambdaQueryWrapper<OperationLog> buildWrapper(String operator, String action, Long personnelId) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (operator != null && !operator.trim().isEmpty()) {
            wrapper.like(OperationLog::getOperator, operator.trim());
        }
        if (action != null && !action.trim().isEmpty()) {
            wrapper.like(OperationLog::getAction, action.trim());
        }
        if (personnelId != null) {
            // 日志 JSON 快照中的 personnelId 形如 "personnelId":5，用带分隔符的精确子串避免误匹配 50/51 等
            String withComma = "\"personnelId\":" + personnelId + ",";
            String withBrace = "\"personnelId\":" + personnelId + "}";
            wrapper.and(w -> w
                    .like(OperationLog::getOldValue, withComma)
                    .or()
                    .like(OperationLog::getOldValue, withBrace)
                    .or()
                    .like(OperationLog::getNewValue, withComma)
                    .or()
                    .like(OperationLog::getNewValue, withBrace));
        }
        return wrapper;
    }

    // ======================== 日志展示辅助 ========================

    private JsonNode parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String actionText(String action) {
        String a = action == null ? "" : action.replaceFirst("^ARCHIVED_", "");
        if ("CREATE".equalsIgnoreCase(a)) return "新增";
        if ("DELETE".equalsIgnoreCase(a)) return "删除";
        return "修改";
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FMT) : "-";
    }

    private String versionText(String action, JsonNode oldNode, JsonNode newNode) {
        String a = action == null ? "" : action.replaceFirst("^ARCHIVED_", "");
        JsonNode v = "DELETE".equalsIgnoreCase(a)
                ? (oldNode != null ? oldNode.get("version") : null)
                : (newNode != null ? newNode.get("version") : null);
        return v != null && !v.isNull() ? v.asText() : "-";
    }

    private String formatField(String key, JsonNode node,
                               Map<Long, String> personnelNameMap, Map<Long, String> projectNameMap) {
        if (node == null || node.isNull() || node.asText().isEmpty()) {
            return "空";
        }
        if ("personnelId".equals(key)) {
            String name = personnelNameMap.get(node.asLong());
            return name != null ? name : "#" + node.asText();
        }
        if ("projectId".equals(key)) {
            String name = projectNameMap.get(node.asLong());
            return name != null ? name : "#" + node.asText();
        }
        if (("startDate".equals(key) || "endDate".equals(key)) && node.isNumber()) {
            // 早期日志中的日期是毫秒时间戳，转回 YYYY-MM-DD
            return Instant.ofEpochMilli(node.asLong()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        if ("dailyHours".equals(key) && node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        return node.asText();
    }

    /**
     * 生成变更内容描述，与前端展示保持一致（含项目名上下文）
     */
    private String changeDescription(String action, JsonNode oldNode, JsonNode newNode,
                                     Map<Long, String> personnelNameMap, Map<Long, String> projectNameMap) {
        String a = action == null ? "" : action.replaceFirst("^ARCHIVED_", "");
        if ("CREATE".equalsIgnoreCase(a)) {
            return String.format("新增分配：%s → %s，%s ~ %s，每日 %s 小时",
                    formatField("personnelId", newNode != null ? newNode.get("personnelId") : null, personnelNameMap, projectNameMap),
                    formatField("projectId", newNode != null ? newNode.get("projectId") : null, personnelNameMap, projectNameMap),
                    formatField("startDate", newNode != null ? newNode.get("startDate") : null, personnelNameMap, projectNameMap),
                    formatField("endDate", newNode != null ? newNode.get("endDate") : null, personnelNameMap, projectNameMap),
                    formatField("dailyHours", newNode != null ? newNode.get("dailyHours") : null, personnelNameMap, projectNameMap));
        }
        if ("DELETE".equalsIgnoreCase(a)) {
            return String.format("删除分配：%s → %s",
                    formatField("personnelId", oldNode != null ? oldNode.get("personnelId") : null, personnelNameMap, projectNameMap),
                    formatField("projectId", oldNode != null ? oldNode.get("projectId") : null, personnelNameMap, projectNameMap));
        }

        // UPDATE：对比关键字段，比较“格式化后的值”，兼容旧日志毫秒时间戳与现日志 ISO 字符串
        List<String> diffs = new ArrayList<>();
        for (String key : DIFF_FIELDS) {
            String oldVal = formatField(key, oldNode != null ? oldNode.get(key) : null, personnelNameMap, projectNameMap);
            String newVal = formatField(key, newNode != null ? newNode.get(key) : null, personnelNameMap, projectNameMap);
            if (!oldVal.equals(newVal)) {
                diffs.add(key);
            }
        }
        String projectName = formatField("projectId", newNode != null ? newNode.get("projectId") : null,
                personnelNameMap, projectNameMap);
        if (diffs.isEmpty()) {
            return "空".equals(projectName) ? "分配信息更新" : "项目「" + projectName + "」：分配信息更新";
        }
        String prefix = diffs.contains("projectId") ? "" : "项目「" + projectName + "」；";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < diffs.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            String key = diffs.get(i);
            sb.append(FIELD_LABELS.getOrDefault(key, key)).append("：")
                    .append(formatField(key, oldNode != null ? oldNode.get(key) : null, personnelNameMap, projectNameMap))
                    .append(" → ")
                    .append(formatField(key, newNode != null ? newNode.get(key) : null, personnelNameMap, projectNameMap));
        }
        return sb.toString();
    }

    // ======================== 导出工具 ========================

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
            excelData.add(new ArrayList<>(row));
        }
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet(sheetName)
                .doWrite(excelData);
    }
}
