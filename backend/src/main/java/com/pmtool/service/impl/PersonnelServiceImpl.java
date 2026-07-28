package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Personnel;
import com.pmtool.mapper.PersonnelMapper;
import com.pmtool.service.PersonnelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 人员服务实现
 */
@Service
public class PersonnelServiceImpl extends ServiceImpl<PersonnelMapper, Personnel> implements PersonnelService {

    @Override
    public PageResult<Personnel> pageQuery(Integer pageNum, Integer pageSize, String name, String position) {
        Page<Personnel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Personnel::getName, name);
        }
        if (position != null && !position.trim().isEmpty()) {
            wrapper.like(Personnel::getPosition, position);
        }
        wrapper.orderByDesc(Personnel::getCreateTime);
        Page<Personnel> result = this.page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Personnel> importPersonnel(List<Personnel> personnelList) {
        List<Personnel> result = new ArrayList<>();
        for (Personnel personnel : personnelList) {
            // 根据工号判断是否存在，存在则更新，不存在则新增
            LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Personnel::getEmpNo, personnel.getEmpNo());
            Personnel existing = this.getOne(wrapper, false);
            if (existing != null) {
                personnel.setId(existing.getId());
                this.updateById(personnel);
            } else {
                this.save(personnel);
            }
            result.add(personnel);
        }
        return result;
    }
}
