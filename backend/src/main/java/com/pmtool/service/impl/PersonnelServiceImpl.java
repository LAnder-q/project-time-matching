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
import java.util.Collection;
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
            // 加密工号后查询；同时兼容历史数据（早期版本双重加密、以及 init.sql 明文种子数据）
            String encryptedEmpNo = DataEncryptUtils.encrypt(personnel.getEmpNo());
            LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Personnel::getEmpNo, encryptedEmpNo)
                    .or()
                    .eq(Personnel::getEmpNo, personnel.getEmpNo())
                    .or()
                    .eq(Personnel::getEmpNo, DataEncryptUtils.encrypt(encryptedEmpNo));
            Personnel existing = this.getOne(wrapper, false);
            if (existing != null) {
                personnel.setId(existing.getId());
                // 由 updateById 统一处理加密与返回前解密
                this.updateById(personnel);
            } else {
                // 由 save 统一处理加密与返回前解密
                this.save(personnel);
            }
            result.add(personnel);
        }
        return result;
    }

    @Override
    public boolean save(Personnel personnel) {
        encryptFields(personnel);
        boolean ok = super.save(personnel);
        // 返回前解密，避免接口响应携带密文
        decryptFields(personnel);
        return ok;
    }

    @Override
    public boolean updateById(Personnel personnel) {
        encryptFields(personnel);
        boolean ok = super.updateById(personnel);
        // 返回前解密，避免接口响应携带密文
        decryptFields(personnel);
        return ok;
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

    @Override
    public List<Personnel> listByIds(Collection<? extends Serializable> idList) {
        List<Personnel> list = super.listByIds(idList);
        list.forEach(this::decryptFields);
        return list;
    }
}
