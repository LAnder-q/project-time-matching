package com.pmtool.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pmtool.dto.LoginDTO;
import com.pmtool.dto.LoginVO;
import com.pmtool.entity.SysUser;
import com.pmtool.mapper.SysUserMapper;
import com.pmtool.service.AuthService;
import com.pmtool.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 根据用户名查询
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // MD5 密码校验
        String md5Password = SecureUtil.md5(loginDTO.getPassword());
        if (!md5Password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getRealName());

        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    @Override
    public LoginVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return new LoginVO(null, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }
}
