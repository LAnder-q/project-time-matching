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
import com.pmtool.service.PersonnelService;
import com.pmtool.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * 冲突检测与智能替换推荐核心算法单元测试
 */
@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceImplTest {

    @Mock
    private AssignmentService assignmentService;
    @Mock
    private PersonnelService personnelService;
    @Mock
    private ProjectService projectService;

    private ConflictDetectionServiceImpl service;

    private Personnel zhangwei;
    private Personnel lina;
    private Project projHigh;
    private Project projLow;

    @BeforeEach
    void setUp() {
        service = new ConflictDetectionServiceImpl(assignmentService, personnelService, projectService);
        zhangwei = personnel(1L, "张伟", "EMP001", "运维工程师", "Linux,Docker", 1L);
        lina = personnel(2L, "李娜", "EMP002", "运维工程师", "Linux,Docker,Kubernetes", 1L);
        projHigh = project(10L, "电商平台升级", 5, "2026-02-01", "2026-06-30", "运维工程师");
        projLow = project(20L, "安全审计加固", 3, "2026-03-01", "2026-07-31", "运维工程师");
    }

    @Test
    @DisplayName("重叠分配被识别为冲突，重叠区间与 HIGH 严重度正确")
    void overlappingAssignmentsReturnHighConflict() {
        ConflictResult result = detectOverlap(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-06-30"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31")));

        assertEquals(1, result.getConflicts().size());
        ConflictDetail detail = result.getConflicts().get(0);
        assertEquals(LocalDate.of(2026, 3, 1), detail.getOverlapStart());
        assertEquals(LocalDate.of(2026, 6, 30), detail.getOverlapEnd());
        assertEquals("HIGH", detail.getSeverity());
        assertEquals("张伟", result.getPersonnelName());
        assertEquals("EMP001", result.getEmpNo());
    }

    @Test
    @DisplayName("同一项目内的多条分配不算冲突")
    void sameProjectAssignmentsAreNotConflicts() {
        ConflictResult result = detectOverlap(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-06-30"),
                assignment(2L, 1L, 10L, "2026-03-01", "2026-07-31")));

        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("时间区间不重叠的分配不产生冲突")
    void nonOverlappingAssignmentsHaveNoConflict() {
        ConflictResult result = detectOverlap(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-02-28"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31")));

        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("严重度分级正确：>=30 天 HIGH，>=7 天 MEDIUM，其余 LOW")
    void severityLevelsFollowOverlapDays() {
        assertEquals("HIGH", detectOverlap(overlapAssignments(30)).getConflicts().get(0).getSeverity());
        assertEquals("MEDIUM", detectOverlap(overlapAssignments(7)).getConflicts().get(0).getSeverity());
        assertEquals("LOW", detectOverlap(overlapAssignments(6)).getConflicts().get(0).getSeverity());
    }

    @Test
    @DisplayName("全量检测按人员分组，并可按部门过滤")
    void detectAllConflictsGroupsAndFiltersByDepartment() {
        Personnel wangwu = personnel(3L, "王强", "EMP003", "高级运维工程师", "Kubernetes,Python", 2L);
        List<Assignment> assignments = List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-06-30"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31"),
                assignment(3L, 3L, 10L, "2026-02-01", "2026-06-30"),
                assignment(4L, 3L, 20L, "2026-03-01", "2026-07-31"));

        when(assignmentService.list()).thenReturn(assignments);
        when(personnelService.list()).thenReturn(List.of(zhangwei, wangwu));
        when(projectService.list()).thenReturn(List.of(projHigh, projLow));

        List<ConflictResult> results = service.detectAllConflicts(1L);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getPersonnelId());
    }

    @Test
    @DisplayName("无冲突时不返回任何结果")
    void detectAllConflictsEmptyWhenNoOverlap() {
        when(assignmentService.list()).thenReturn(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-02-28"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31")));
        when(personnelService.list()).thenReturn(List.of(zhangwei));
        when(projectService.list()).thenReturn(List.of(projHigh, projLow));

        assertTrue(service.detectAllConflicts(null).isEmpty());
    }

    @Test
    @DisplayName("智能推荐：生成工时调整与替换人员建议，候选按匹配度和可用率排序")
    void generateSuggestionsReturnsAdjustAndReplace() {
        when(assignmentService.list()).thenReturn(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-06-30"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31")));
        when(personnelService.list()).thenReturn(List.of(zhangwei, lina));
        when(projectService.list()).thenReturn(List.of(projHigh, projLow));

        List<ConflictSuggestion> suggestions = service.generateSuggestions(null);

        assertEquals(2, suggestions.size());
        ConflictSuggestion adjust = suggestions.stream()
                .filter(s -> "ADJUST_HOURS".equals(s.getSuggestionType()))
                .findFirst().orElseThrow();
        ConflictSuggestion replace = suggestions.stream()
                .filter(s -> "REPLACE_PERSONNEL".equals(s.getSuggestionType()))
                .findFirst().orElseThrow();

        assertEquals(20L, adjust.getProjectId());
        assertNotNull(adjust.getAdjustAdvice());
        assertEquals(1, replace.getCandidates().size());

        ConflictSuggestion.ReplacementCandidate candidate = replace.getCandidates().get(0);
        assertEquals(2L, candidate.getPersonnelId());
        assertEquals("RECOMMENDED", candidate.getRecommendationLevel());
        assertEquals(100.0, candidate.getMatchScore());
        assertEquals(100.0, candidate.getAvailabilityRate());
    }

    @Test
    @DisplayName("无冲突时智能推荐返回空列表")
    void generateSuggestionsEmptyWhenNoConflict() {
        when(assignmentService.list()).thenReturn(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-02-28"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31")));
        when(personnelService.list()).thenReturn(List.of(zhangwei));
        when(projectService.list()).thenReturn(List.of(projHigh, projLow));

        assertTrue(service.generateSuggestions(null).isEmpty());
    }

    @Test
    @DisplayName("日历数据按时间区间过滤，标题与颜色正确")
    void calendarDataFiltersByDateRange() {
        when(assignmentService.list()).thenReturn(List.of(
                assignment(1L, 1L, 10L, "2026-02-01", "2026-06-30"),
                assignment(2L, 1L, 20L, "2026-03-01", "2026-07-31"),
                assignment(3L, 1L, 20L, "2026-08-01", "2026-09-30")));
        when(personnelService.list()).thenReturn(List.of(zhangwei));
        when(projectService.list()).thenReturn(List.of(projHigh, projLow));

        List<CalendarEvent> events = service.getCalendarData(
                null, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(2, events.size());
        assertEquals("张伟 - 电商平台升级", events.get(0).getTitle());
        assertNotEquals(events.get(0).getColor(), events.get(1).getColor());
    }

    private ConflictResult detectOverlap(List<Assignment> assignments) {
        when(personnelService.getById(1L)).thenReturn(zhangwei);
        when(assignmentService.list(any(LambdaQueryWrapper.class))).thenReturn(assignments);
        when(projectService.listByIds(anyCollection())).thenReturn(List.of(projHigh, projLow));
        return service.detectConflictsByPersonnelId(1L);
    }

    private List<Assignment> overlapAssignments(int days) {
        LocalDate aEnd = LocalDate.of(2026, 1, 31);
        return List.of(
                assignment(1L, 1L, 10L, LocalDate.of(2026, 1, 1), aEnd),
                assignment(2L, 1L, 20L, aEnd.minusDays(days - 1L), LocalDate.of(2026, 2, 28)));
    }

    private Personnel personnel(Long id, String name, String empNo, String positions, String skills, Long deptId) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setName(name);
        p.setEmpNo(empNo);
        p.setPositions(positions);
        p.setSkills(skills);
        p.setDeptId(deptId);
        p.setAvailableStartDate(LocalDate.of(2026, 1, 1));
        p.setAvailableEndDate(LocalDate.of(2026, 12, 31));
        return p;
    }

    private Project project(Long id, String name, int priority, String start, String end, String requiredPosition) {
        Project p = new Project();
        p.setId(id);
        p.setName(name);
        p.setPriority(priority);
        p.setStartDate(LocalDate.parse(start));
        p.setEndDate(LocalDate.parse(end));
        p.setRequiredPosition(requiredPosition);
        return p;
    }

    private Assignment assignment(Long id, Long personnelId, Long projectId, String start, String end) {
        return assignment(id, personnelId, projectId, LocalDate.parse(start), LocalDate.parse(end));
    }

    private Assignment assignment(Long id, Long personnelId, Long projectId, LocalDate start, LocalDate end) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setPersonnelId(personnelId);
        a.setProjectId(projectId);
        a.setStartDate(start);
        a.setEndDate(end);
        a.setDailyHours(BigDecimal.valueOf(8));
        return a;
    }
}
