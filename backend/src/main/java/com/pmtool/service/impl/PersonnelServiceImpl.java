package com.pmtool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pmtool.common.PageResult;
import com.pmtool.entity.Personnel;
import com.pmtool.mapper.PersonnelMapper;
import com.pmtool.service.PersonnelService;
import com.pmtool.utils.DataEncryptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 人员服务实现
 * 敏感字段（工号）使用 AES 加密存储，读取时自动解密
 */
@Service
public class PersonnelServiceImpl extends ServiceImpl<PersonnelMapper, Personnel> implements PersonnelService {

    /**
     * 加密人员敏感字段（保存前调用）
     */
    private void encryptFields(Personnel personnel) {
        if (personnel == null) return;
        if (personnel.getEmpNo() != null && !personnel.getEmpNo().isEmpty()) {
            personnel.setEmpNo(DataEncryptUtils.encrypt(personnel.getEmpNo()));
        }
    }

    /**
     * 解密人员敏感字段（读取后调用）
     */
    private void decryptFields(Personnel personnel) {
        if (personnel == null) return;
        if (personnel.getEmpNo() != null && !personnel.getEmpNo().isEmpty()) {
            personnel.setEmpNo(DataEncryptUtils.decrypt(personnel.getEmpNo()));
        }
    }

    @Override
    public PageResult<Personnel> pageQuery(Integer pageNum, Integer pageSize, String name, String positions, Long deptId) {
        Page<Personnel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Personnel::getName, name);
        }
        if (positions != null && !positions.trim().isEmpty()) {
            wrapper.like(Personnel::getPositions, positions);
        }
        if (deptId != null) {
            wrapper.eq(Personnel::getDeptId, deptId);
        }
        wrapper.orderByDesc(Personnel::getCreateTime);
        Page<Personnel> result = this.page(page, wrapper);
        // 解密敏感字段
        result.getRecords().forEach(this::decryptFields);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Personnel> importPersonnel(List<Personnel> personnelList) {
        List<Personnel> result = new ArrayList<>();
        for (Personnel personnel : personnelList) {
            // 加密工号后查询
            String encryptedEmpNo = DataEncryptUtils.encrypt(personnel.getEmpNo());
            LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Personnel::getEmpNo, encryptedEmpNo);
            Personnel existing = this.getOne(wrapper, false);
            if (existing != null) {
                personnel.setId(existing.getId());
                encryptFields(personnel);
                this.updateById(personnel);
            } else {
                encryptFields(personnel);
                this.save(personnel);
            }
            // 返回前解密
            decryptFields(personnel);
            result.add(personnel);
        }
        return result;
    }

    @Override
    public boolean save(Personnel personnel) {
        encryptFields(personnel);
        return super.save(personnel);
    }

    @Override
    public boolean updateById(Personnel personnel) {
        encryptFields(personnel);
        return super.updateById(personnel);
    }

    @Override
    public Personnel getById(Serializable id) {
        Personnel personnel = super.getById(id);
        decryptFields(personnel);
        return personnel;
    }

    @Override
    public List<Personnel> list() {
        List<Personnel> list = super.list();
        list.forEach(this::decryptFields);
        return list;
    }
}
