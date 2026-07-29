package com.pmtool.controller;

import com.pmtool.common.Result;
import com.pmtool.dto.CalendarEvent;
import com.pmtool.dto.ConflictResult;
import com.pmtool.dto.ConflictSuggestion;
import com.pmtool.service.ConflictDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 冲突检测 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 */
@RestController
@RequestMapping("/conflict")
public class ConflictController {

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    /**
     * 检测所有人员的分配冲突
     */
    @GetMapping("/detect")
    public Result<List<ConflictResult>> detectAll() {
        return Result.ok(conflictDetectionService.detectAllConflicts());
    }

    /**
     * 检测指定人员的分配冲突
     */
    @GetMapping("/personnel/{personnelId}")
    public Result<ConflictResult> detectByPersonnel(@PathVariable Long personnelId) {
        return Result.ok(conflictDetectionService.detectConflictsByPersonnelId(personnelId));
    }

    /**
     * 获取日历视图数据（支持按人员、项目、时间区间筛选）
     */
    @GetMapping("/calendar")
    public Result<List<CalendarEvent>> calendar(
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(conflictDetectionService.getCalendarData(personnelId, projectId, startDate, endDate));
    }

    /**
     * 生成冲突调优建议
     */
    @GetMapping("/suggestions")
    public Result<List<ConflictSuggestion>> suggestions() {
        return Result.ok(conflictDetectionService.generateSuggestions());
    }
}
