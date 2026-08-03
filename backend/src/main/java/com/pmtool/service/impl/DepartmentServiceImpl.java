package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.dto.DepartmentVO;
import com.pmtool.entity.Department;
import com.pmtool.mapper.DepartmentMapper;
import com.pmtool.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务实现
 * <p>
 * 树形构建策略：一次查询全部部门，内存中按 parentId 分组递归构树。
 * 部门数量通常很小（几十个），无需懒加载。
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Override
    public List<DepartmentVO> getDepartmentTree() {
        // 一次查询全部部门，按 sort 升序排序，保证同级顺序稳定
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Department::getSort).orderByAsc(Department::getId);
        List<Department> all = this.list(wrapper);

        // 按 parentId 分组（parentId 为 null 归入 0L 键，便于 Map 查询）
        Map<Long, List<Department>> grouped = new LinkedHashMap<>();
        for (Department d : all) {
            Long key = d.getParentId() == null ? 0L : d.getParentId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        // 从虚拟根（0L）开始递归构建
        return buildChildren(0L, grouped);
    }

    /**
     * 递归构建子部门树
     */
    private List<DepartmentVO> buildChildren(Long parentId, Map<Long, List<Department>> grouped) {
        List<Department> children = grouped.get(parentId);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        List<DepartmentVO> result = new ArrayList<>(children.size());
        for (Department d : children) {
            DepartmentVO vo = new DepartmentVO();
            vo.setId(d.getId());
            vo.setName(d.getName());
            vo.setCode(d.getCode());
            vo.setParentId(d.getParentId());
            vo.setSort(d.getSort());
            vo.setChildren(buildChildren(d.getId(), grouped));
            result.add(vo);
        }
        // 同级按 sort 升序（已在 SQL 排序，二次保险）
        result.sort(Comparator.comparingInt(v -> v.getSort() == null ? 0 : v.getSort()));
        return result;
    }

    @Override
    public Long findIdByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getName, name.trim());
        wrapper.last("LIMIT 1");
        Department dept = this.getOne(wrapper, false);
        return dept != null ? dept.getId() : null;
    }
}
