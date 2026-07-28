package com.pmtool.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AssignmentServiceImpl(OperationLogMapper operationLogMapper,
                                 PersonnelService personnelService,
                                 ProjectService projectService) {
        this.operationLogMapper = operationLogMapper;
        this.personnelService = personnelService;
        this.projectService = projectService;
    }

    @Override
    public List<AssignmentVO> listByConditions(Long personnelId, Long projectId) {
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        if (personnelId != null) {
            wrapper.eq(Assignment::getPersonnelId, personnelId);
        }
        if (projectId != null) {
            wrapper.eq(Assignment::getProjectId, projectId);
        }
        wrapper.orderByDesc(Assignment::getStartDate);
        List<Assignment> assignments = this.list(wrapper);

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
        // 记录操作日志
        logOperation(null, assignment, "CREATE", dto.getOperator());
        return assignment;
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
        log.setOldValue(oldValue != null ? JSONUtil.toJsonStr(oldValue) : null);
        log.setNewValue(JSONUtil.toJsonStr(newValue));
        log.setOperator(operator);
        operationLogMapper.insert(log);
    }
}
