package com.pmtool.service;

import com.pmtool.dto.CalendarEvent;
import com.pmtool.dto.ConflictResult;
import com.pmtool.dto.ConflictSuggestion;

import java.time.LocalDate;
import java.util.List;

/**
 * 冲突检测服务接口
 * <p>
 * 核心功能：检测同一人员在多个项目中的时间区间重叠冲突
 * 冲突检测算法：两个区间 [start1, end1] 和 [start2, end2] 重叠的条件是
 * start1 &lt;= end2 &amp;&amp; start2 &lt;= end1
 */
public interface ConflictDetectionService {

    /**
     * 检测所有人员的分配冲突
     * 查询所有 assignment，按 personnel_id 分组，检测同一人员的时间区间重叠
     *
     * @param deptId 部门ID（可选，为 null 时不过滤部门）
     * @return 冲突结果列表（仅包含有冲突的人员）
     */
    List<ConflictResult> detectAllConflicts(Long deptId);

    /**
     * 检测指定人员的分配冲突
     *
     * @param personnelId 人员ID
     * @return 冲突结果
     */
    ConflictResult detectConflictsByPersonnelId(Long personnelId);

    /**
     * 获取日历视图数据（支持按人员、项目、部门、时间区间筛选）
     * 所有参数均可选，为 null 时不过滤该维度
     *
     * @param personnelId 人员ID（可选）
     * @param projectId   项目ID（可选）
     * @param deptId      部门ID（可选）
     * @param startDate   开始日期（可选）
     * @param endDate     结束日期（可选）
     * @return 日历事件列表
     */
    List<CalendarEvent> getCalendarData(Long personnelId, Long projectId, Long deptId, LocalDate startDate, LocalDate endDate);

    /**
     * 基于已检测到的冲突生成调优建议
     * 针对每个冲突，比较涉及项目的优先级：
     * 1. 对低优先级项目生成 ADJUST_HOURS（工时调整）建议
     * 2. 查找技能匹配且冲突时段可用的替代人员，生成 REPLACE_PERSONNEL（替换人员）建议
     *
     * @param deptId 部门ID（可选，仅生成该部门人员的冲突建议）
     * @return 冲突调优建议列表
     */
    List<ConflictSuggestion> generateSuggestions(Long deptId);
}
