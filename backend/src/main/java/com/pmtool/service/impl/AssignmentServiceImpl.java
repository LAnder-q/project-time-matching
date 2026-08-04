package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.common.PageResult;
import com.pmtool.dto.AssignmentDTO;
import com.pmtool.dto.AssignmentVO;
import com.pmtool.entity.Assignment;
import com.pmtool.entity.OperationLog;
import com.pmtool.entity.Personnel;
import com.pmtool.entity.Project;
import com.pmtool.mapper.AssignmentMapper;
import com.pmtool.mapper.OperationLogMapper;
import com.pmtool.service.AssignmentService;
import com.pmtool.service.PersonnelService;
import com.pmtool.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人员项目分配服务实现
 * 修改时记录 operation_log 实现历史版本追溯
 */
@Service
public class AssignmentServiceImpl extends ServiceImpl<AssignmentMapper, Assignment> implements AssignmentService {

    private final OperationLogMapper operationLogMapper;
    private final PersonnelService personnelService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public AssignmentServiceImpl(OperationLogMapper operationLogMapper,
                                 PersonnelService personnelService,
                                 ProjectService projectService,
                                 ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.personnelService = personnelService;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AssignmentVO> pageByConditions(Integer pageNum, Integer pageSize, Long personnelId, Long projectId) {
        Page<Assignment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Assignment> wrapper = buildQueryWrapper(personnelId, projectId);
        Page<Assignment> result = this.page(page, wrapper);
        List<AssignmentVO> voList = fillVoFields(result.getRecords());
        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<AssignmentVO> listByConditions(Long personnelId, Long projectId) {
        LambdaQueryWrapper<Assignment> wrapper = buildQueryWrapper(personnelId, projectId);
        List<Assignment> assignments = this.list(wrapper);
        return fillVoFields(assignments);
    }

    /**
     * 构建查询条件（分页与全量共用）
     */
    private LambdaQueryWrapper<Assignment> buildQueryWrapper(Long personnelId, Long projectId) {
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        if (personnelId != null) {
            wrapper.eq(Assignment::getPersonnelId, personnelId);
        }
        if (projectId != null) {
            wrapper.eq(Assignment::getProjectId, projectId);
        }
        wrapper.orderByDesc(Assignment::getStartDate);
        return wrapper;
    }

    /**
     * 填充联表字段（人员姓名、工号、项目名称），供分页与全量共用
     */
    private List<AssignmentVO> fillVoFields(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询关联的人员和项目信息
        Set<Long> personnelIds = assignments.stream().map(Assignment::getPersonnelId).collect(Collectors.toSet());
        Set<Long> projectIds = assignments.stream().map(Assignment::getProjectId).collect(Collectors.toSet());

        Map<Long, Personnel> personnelMap = personnelIds.isEmpty() ? Map.of() :
                personnelService.listByIds(personnelIds).stream()
                        .collect(Collectors.toMap(Personnel::getId, p -> p));
        Map<Long, Project> projectMap = projectIds.isEmpty() ? Map.of() :
                projectService.listByIds(projectIds).stream()
                        .collect(Collectors.toMap(Project::getId, p -> p));

        // 转换为 VO 并填充联表字段
        return assignments.stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            BeanUtils.copyProperties(a, vo);
            Personnel p = personnelMap.get(a.getPersonnelId());
            if (p != null) {
                vo.setPersonnelName(p.getName());
                vo.setEmpNo(p.getEmpNo());
            }
            Project proj = projectMap.get(a.getProjectId());
            if (proj != null) {
                vo.setProjectName(proj.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Assignment createAssignment(AssignmentDTO dto) {
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(dto, assignment);
        assignment.setVersion(1);
        this.save(assignment);
        // 回读保存后的完整记录（含数据库填充的 createTime/updateTime），保证日志快照完整
        Assignment saved = this.getById(assignment.getId());
        // 记录操作日志
        logOperation(null, saved, "CREATE", dto.getOperator());
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Assignment updateAssignment(Long id, AssignmentDTO dto) {
        Assignment old = this.getById(id);
        if (old == null) {
            throw new RuntimeException("分配记录不存在: " + id);
        }
        // 记录旧值用于日志
        Assignment oldCopy = new Assignment();
        BeanUtils.copyProperties(old, oldCopy);

        // 更新字段
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(dto, assignment);
        assignment.setId(id);
        // 版本号 +1
        assignment.setVersion(old.getVersion() + 1);
        this.updateById(assignment);

        // 合并最新数据用于日志记录
        Assignment updated = this.getById(id);
        logOperation(oldCopy, updated, "UPDATE", dto.getOperator());
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssignment(Long id, String operator) {
        Assignment old = this.getById(id);
        if (old == null) {
            throw new RuntimeException("分配记录不存在: " + id);
        }
        // 逻辑删除
        this.removeById(id);

        // 记录删除日志（保留删除前的完整快照）
        OperationLog log = new OperationLog();
        log.setEntityType("ASSIGNMENT");
        log.setEntityId(id);
        log.setAction("DELETE");
        log.setOldValue(toJson(old));
        log.setNewValue(null);
        log.setOperator(operator);
        operationLogMapper.insert(log);
    }

    @Override
    public List<OperationLog> listLogs(Long assignmentId) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getEntityType, "ASSIGNMENT");
        wrapper.eq(OperationLog::getEntityId, assignmentId);
        wrapper.orderByDesc(OperationLog::getOperateTime);
        return operationLogMapper.selectList(wrapper);
    }

    /**
     * 记录操作日志
     *
     * @param oldValue 旧值（CREATE 时为 null）
     * @param newValue 新值
     * @param action   操作类型
     * @param operator 操作人
     */
    private void logOperation(Assignment oldValue, Assignment newValue, String action, String operator) {
        OperationLog log = new OperationLog();
        log.setEntityType("ASSIGNMENT");
        log.setEntityId(newValue.getId());
        log.setAction(action);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        log.setOperator(operator);
        operationLogMapper.insert(log);
    }

    /**
     * 将分配快照序列化为 JSON。
     * 使用 Spring 的 ObjectMapper（日期输出为 ISO 字符串，如 2026-09-30），
     * 避免 Hutool 默认将日期序列化为毫秒时间戳导致历史记录难以阅读。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("操作日志序列化失败", e);
        }
    }
}
