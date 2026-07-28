package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pmtool.dto.CalendarEvent;
import com.pmtool.dto.ConflictDetail;
import com.pmtool.dto.ConflictResult;
import com.pmtool.dto.ConflictSuggestion;
import com.pmtool.entity.Assignment;
import com.pmtool.entity.Personnel;
import com.pmtool.entity.Project;
import com.pmtool.service.AssignmentService;
import com.pmtool.service.ConflictDetectionService;
import com.pmtool.service.PersonnelService;
import com.pmtool.service.ProjectService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 冲突检测服务实现
 * <p>
 * 核心算法：检测同一人员在多个项目中的时间区间重叠
 * 两个区间 [start1, end1] 和 [start2, end2] 重叠的条件是 start1 <= end2 && start2 <= end1
 */
@Service
public class ConflictDetectionServiceImpl implements ConflictDetectionService {

    private final AssignmentService assignmentService;
    private final PersonnelService personnelService;
    private final ProjectService projectService;

    /** 项目颜色调色板（用于日历视图区分不同项目） */
    private static final String[] COLOR_PALETTE = {
            "#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399",
            "#9B59B6", "#1ABC9C", "#34495E", "#E67E22", "#2ECC71",
            "#3498DB", "#95A5A6", "#F39C12", "#C0392B", "#8E44AD"
    };

    public ConflictDetectionServiceImpl(AssignmentService assignmentService,
                                        PersonnelService personnelService,
                                        ProjectService projectService) {
        this.assignmentService = assignmentService;
        this.personnelService = personnelService;
        this.projectService = projectService;
    }

    @Override
    public List<ConflictResult> detectAllConflicts() {
        // 查询所有分配记录
        List<Assignment> allAssignments = assignmentService.list();
        // 按 personnel_id 分组
        Map<Long, List<Assignment>> grouped = allAssignments.stream()
                .collect(Collectors.groupingBy(Assignment::getPersonnelId));

        List<ConflictResult> results = new ArrayList<>();
        for (Map.Entry<Long, List<Assignment>> entry : grouped.entrySet()) {
            ConflictResult result = detectConflictsForPersonnel(entry.getKey(), entry.getValue());
            if (result.getConflicts() != null && !result.getConflicts().isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public ConflictResult detectConflictsByPersonnelId(Long personnelId) {
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assignment::getPersonnelId, personnelId);
        wrapper.orderByAsc(Assignment::getStartDate);
        List<Assignment> assignments = assignmentService.list(wrapper);
        return detectConflictsForPersonnel(personnelId, assignments);
    }

    /**
     * 检测指定人员分配列表中的冲突
     */
    private ConflictResult detectConflictsForPersonnel(Long personnelId, List<Assignment> assignments) {
        ConflictResult result = new ConflictResult();
        result.setPersonnelId(personnelId);

        // 获取人员信息
        Personnel personnel = personnelService.getById(personnelId);
        result.setPersonnelName(personnel != null ? personnel.getName() : "未知人员");
        result.setEmpNo(personnel != null ? personnel.getEmpNo() : "未知");

        List<ConflictDetail> conflicts = new ArrayList<>();

        // 获取所有相关项目名称
        Map<Long, String> projectNameMap = getProjectNameMap(assignments);

        // 两两比较所有分配的时间区间
        for (int i = 0; i < assignments.size(); i++) {
            for (int j = i + 1; j < assignments.size(); j++) {
                Assignment a1 = assignments.get(i);
                Assignment a2 = assignments.get(j);

                // 同一项目的分配不算冲突
                if (a1.getProjectId().equals(a2.getProjectId())) {
                    continue;
                }

                // 冲突检测算法：start1 <= end2 && start2 <= end1
                LocalDate start1 = a1.getStartDate();
                LocalDate end1 = a1.getEndDate();
                LocalDate start2 = a2.getStartDate();
                LocalDate end2 = a2.getEndDate();

                if (start1.compareTo(end2) <= 0 && start2.compareTo(end1) <= 0) {
                    ConflictDetail detail = new ConflictDetail();
                    detail.setProjectId1(a1.getProjectId());
                    detail.setProjectName1(projectNameMap.getOrDefault(a1.getProjectId(), "未知项目"));
                    detail.setProjectId2(a2.getProjectId());
                    detail.setProjectName2(projectNameMap.getOrDefault(a2.getProjectId(), "未知项目"));

                    // 重叠区间：取两个区间的交集
                    LocalDate overlapStart = start1.isAfter(start2) ? start1 : start2;
                    LocalDate overlapEnd = end1.isBefore(end2) ? end1 : end2;
                    detail.setOverlapStart(overlapStart);
                    detail.setOverlapEnd(overlapEnd);

                    // 根据重叠天数判定严重程度
                    long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                    if (overlapDays >= 30) {
                        detail.setSeverity("HIGH");
                    } else if (overlapDays >= 7) {
                        detail.setSeverity("MEDIUM");
                    } else {
                        detail.setSeverity("LOW");
                    }
                    conflicts.add(detail);
                }
            }
        }
        result.setConflicts(conflicts);
        return result;
    }

    @Override
    public List<CalendarEvent> getCalendarData() {
        List<Assignment> allAssignments = assignmentService.list();

        // 预加载所有人员和项目信息
        Map<Long, Personnel> personnelMap = personnelService.list().stream()
                .collect(Collectors.toMap(Personnel::getId, p -> p));
        Map<Long, Project> projectMap = projectService.list().stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 为每个项目分配颜色
        Map<Long, String> projectColorMap = new HashMap<>();
        int colorIndex = 0;
        for (Long projectId : projectMap.keySet()) {
            projectColorMap.put(projectId, COLOR_PALETTE[colorIndex % COLOR_PALETTE.length]);
            colorIndex++;
        }

        List<CalendarEvent> events = new ArrayList<>();
        for (Assignment assignment : allAssignments) {
            CalendarEvent event = new CalendarEvent();
            event.setId(assignment.getId());
            event.setStart(assignment.getStartDate());
            event.setEnd(assignment.getEndDate());
            event.setPersonnelId(assignment.getPersonnelId());
            event.setProjectId(assignment.getProjectId());
            event.setDailyHours(assignment.getDailyHours());

            // 人员名称
            Personnel personnel = personnelMap.get(assignment.getPersonnelId());
            String personnelName = personnel != null ? personnel.getName() : "未知人员";
            event.setPersonnelName(personnelName);

            // 项目名称和标题
            Project project = projectMap.get(assignment.getProjectId());
            String projectName = project != null ? project.getName() : "未知项目";
            event.setProjectName(projectName);
            event.setTitle(personnelName + " - " + projectName);

            // 项目颜色
            event.setColor(projectColorMap.get(assignment.getProjectId()));

            events.add(event);
        }
        return events;
    }

    @Override
    public List<ConflictSuggestion> generateSuggestions() {
        // 1. 调用 detectAllConflicts() 获取所有冲突
        List<ConflictResult> conflictResults = detectAllConflicts();
        List<ConflictSuggestion> suggestions = new ArrayList<>();

        if (conflictResults == null || conflictResults.isEmpty()) {
            return suggestions;
        }

        // 预加载所有人员和分配记录，避免在循环中反复查询
        List<Personnel> allPersonnel = personnelService.list();
        List<Assignment> allAssignments = assignmentService.list();

        // 2. 对每个冲突生成调优建议
        for (ConflictResult cr : conflictResults) {
            Long personnelId = cr.getPersonnelId();
            if (personnelId == null || cr.getConflicts() == null) {
                continue;
            }
            Personnel conflictPersonnel = personnelService.getById(personnelId);

            for (ConflictDetail cd : cr.getConflicts()) {
                // 查找冲突涉及的两个项目，比较优先级
                Project project1 = projectService.getById(cd.getProjectId1());
                Project project2 = projectService.getById(cd.getProjectId2());
                if (project1 == null || project2 == null) {
                    continue;
                }

                int p1Priority = project1.getPriority() != null ? project1.getPriority() : 1;
                int p2Priority = project2.getPriority() != null ? project2.getPriority() : 1;

                // 确定低优先级项目（priority 数值越小优先级越低）
                Project lowerProject;
                Project higherProject;
                Long lowerProjectId;
                String lowerProjectName;
                if (p1Priority <= p2Priority) {
                    lowerProject = project1;
                    higherProject = project2;
                    lowerProjectId = cd.getProjectId1();
                    lowerProjectName = cd.getProjectName1();
                } else {
                    lowerProject = project2;
                    higherProject = project1;
                    lowerProjectId = cd.getProjectId2();
                    lowerProjectName = cd.getProjectName2();
                }

                // 查找冲突涉及的分配ID（低优先级项目对应的分配记录）
                Long conflictAssignmentId = findAssignmentId(personnelId, lowerProjectId, allAssignments);

                // 2.b 对低优先级项目生成 ADJUST_HOURS 建议：
                //    提示将人员在低优先级项目的工时减少或调整时间避开冲突
                ConflictSuggestion adjustSuggestion = new ConflictSuggestion();
                adjustSuggestion.setPersonnelId(personnelId);
                adjustSuggestion.setPersonnelName(cr.getPersonnelName());
                adjustSuggestion.setEmpNo(cr.getEmpNo());
                adjustSuggestion.setConflictAssignmentId(conflictAssignmentId);
                adjustSuggestion.setProjectId(lowerProjectId);
                adjustSuggestion.setProjectName(lowerProjectName);
                adjustSuggestion.setSuggestionType("ADJUST_HOURS");
                adjustSuggestion.setDescription(String.format(
                        "人员 %s 在低优先级项目「%s」(优先级%d) 与高优先级项目「%s」(优先级%d) 存在时间冲突（%s 至 %s），建议调整低优先级项目的工时或时间安排以避开冲突时段",
                        cr.getPersonnelName(), lowerProject.getName(), lowerProject.getPriority(),
                        higherProject.getName(), higherProject.getPriority(),
                        cd.getOverlapStart(), cd.getOverlapEnd()));
                adjustSuggestion.setAdjustAdvice(String.format(
                        "建议将人员 %s 在项目「%s」的每日工时减少，或将分配时间调整至 %s 之前或 %s 之后，以避开与项目「%s」的冲突时段",
                        cr.getPersonnelName(), lowerProject.getName(),
                        cd.getOverlapStart(), cd.getOverlapEnd(), higherProject.getName()));
                suggestions.add(adjustSuggestion);

                // 2.c 查找技能匹配的替代人员（技能重叠且在冲突时间段内可用），生成 REPLACE_PERSONNEL 建议
                List<ConflictSuggestion.ReplacementCandidate> candidates = findReplacementCandidates(
                        personnelId, conflictPersonnel, cd.getOverlapStart(), cd.getOverlapEnd(),
                        allPersonnel, allAssignments);

                ConflictSuggestion replaceSuggestion = new ConflictSuggestion();
                replaceSuggestion.setPersonnelId(personnelId);
                replaceSuggestion.setPersonnelName(cr.getPersonnelName());
                replaceSuggestion.setEmpNo(cr.getEmpNo());
                replaceSuggestion.setConflictAssignmentId(conflictAssignmentId);
                replaceSuggestion.setProjectId(lowerProjectId);
                replaceSuggestion.setProjectName(lowerProjectName);
                replaceSuggestion.setSuggestionType("REPLACE_PERSONNEL");
                replaceSuggestion.setDescription(String.format(
                        "人员 %s 在项目「%s」与项目「%s」存在时间冲突，建议为低优先级项目「%s」寻找技能匹配的替代人员",
                        cr.getPersonnelName(), cd.getProjectName1(), cd.getProjectName2(), lowerProjectName));
                replaceSuggestion.setCandidates(candidates);
                suggestions.add(replaceSuggestion);
            }
        }
        return suggestions;
    }

    /**
     * 根据人员ID和项目ID查找分配记录ID
     */
    private Long findAssignmentId(Long personnelId, Long projectId, List<Assignment> allAssignments) {
        for (Assignment a : allAssignments) {
            if (personnelId.equals(a.getPersonnelId()) && projectId.equals(a.getProjectId())) {
                return a.getId();
            }
        }
        return null;
    }

    /**
     * 查找技能匹配的替代人员
     * 条件：与冲突人员技能存在重叠，且在冲突时间段内可用（无重叠分配且可用日期覆盖冲突时段）
     * 技能匹配算法：将 skills 按逗号分割成集合，计算交集比例（Jaccard）作为 matchScore
     */
    private List<ConflictSuggestion.ReplacementCandidate> findReplacementCandidates(
            Long conflictPersonnelId, Personnel conflictPersonnel,
            LocalDate overlapStart, LocalDate overlapEnd,
            List<Personnel> allPersonnel, List<Assignment> allAssignments) {

        List<ConflictSuggestion.ReplacementCandidate> candidates = new ArrayList<>();
        if (conflictPersonnel == null || overlapStart == null || overlapEnd == null) {
            return candidates;
        }

        // 冲突人员的技能集合
        Set<String> conflictSkills = parseSkills(conflictPersonnel.getSkills());
        if (conflictSkills.isEmpty()) {
            return candidates;
        }

        for (Personnel candidate : allPersonnel) {
            // 排除冲突人员自身
            if (candidate.getId().equals(conflictPersonnelId)) {
                continue;
            }

            // 2.d 技能匹配算法：计算交集比例
            Set<String> candidateSkills = parseSkills(candidate.getSkills());
            double matchScore = calculateMatchScore(conflictSkills, candidateSkills);
            if (matchScore <= 0) {
                continue;  // 无技能交集，跳过
            }

            // 校验在冲突时间段内可用
            if (!isAvailable(candidate, overlapStart, overlapEnd, allAssignments)) {
                continue;
            }

            ConflictSuggestion.ReplacementCandidate rc = new ConflictSuggestion.ReplacementCandidate();
            rc.setPersonnelId(candidate.getId());
            rc.setName(candidate.getName());
            rc.setEmpNo(candidate.getEmpNo());
            rc.setPosition(candidate.getPosition());
            rc.setSkills(candidate.getSkills());
            rc.setMatchScore(matchScore);
            rc.setReason(String.format("技能匹配度 %.0f%%，在冲突时段（%s 至 %s）可用",
                    matchScore, overlapStart, overlapEnd));
            candidates.add(rc);
        }

        // 按匹配度从高到低排序
        candidates.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        return candidates;
    }

    /**
     * 将技能字符串按逗号分割为集合（忽略大小写、去除空白）
     */
    private Set<String> parseSkills(String skills) {
        Set<String> set = new HashSet<>();
        if (skills == null || skills.trim().isEmpty()) {
            return set;
        }
        for (String s : skills.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed.toLowerCase());
            }
        }
        return set;
    }

    /**
     * 计算两个技能集合的交集比例（Jaccard 相似度），返回 0-100 的分值
     */
    private double calculateMatchScore(Set<String> skills1, Set<String> skills2) {
        if (skills1.isEmpty() || skills2.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(skills1);
        intersection.retainAll(skills2);
        Set<String> union = new HashSet<>(skills1);
        union.addAll(skills2);
        if (union.isEmpty()) {
            return 0;
        }
        return intersection.size() * 100.0 / union.size();
    }

    /**
     * 判断候选人在指定时间段内是否可用
     * 可用条件：1) 无任何分配记录与该时间段重叠；2) 可用日期范围覆盖该时间段（如已设置）
     */
    private boolean isAvailable(Personnel candidate, LocalDate start, LocalDate end,
                                List<Assignment> allAssignments) {
        // 1. 校验不存在与冲突时段重叠的分配记录
        for (Assignment a : allAssignments) {
            if (!candidate.getId().equals(a.getPersonnelId())) {
                continue;
            }
            if (a.getStartDate() == null || a.getEndDate() == null) {
                continue;
            }
            // 区间重叠判断：start1 <= end2 && start2 <= end1
            if (a.getStartDate().compareTo(end) <= 0 && start.compareTo(a.getEndDate()) <= 0) {
                return false;
            }
        }

        // 2. 校验可用日期范围覆盖冲突时段（如已设置可用日期）
        if (candidate.getAvailableStartDate() != null
                && candidate.getAvailableStartDate().isAfter(start)) {
            return false;
        }
        if (candidate.getAvailableEndDate() != null
                && candidate.getAvailableEndDate().isBefore(end)) {
            return false;
        }
        return true;
    }

    /**
     * 获取分配列表中涉及的项目名称映射
     */
    private Map<Long, String> getProjectNameMap(List<Assignment> assignments) {
        Set<Long> projectIds = assignments.stream()
                .map(Assignment::getProjectId)
                .collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        for (Long projectId : projectIds) {
            Project project = projectService.getById(projectId);
            if (project != null) {
                map.put(projectId, project.getName());
            }
        }
        return map;
    }
}
