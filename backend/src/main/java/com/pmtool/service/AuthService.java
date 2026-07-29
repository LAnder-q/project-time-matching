package com.pmtool.service;

import com.pmtool.dto.LoginDTO;
import com.pmtool.dto.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果（含 token）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据用户名获取用户信息（用于获取当前登录用户）
     *
     * @param userId 用户ID
     * @return 登录结果（不含 token）
     */
    LoginVO getUserInfo(Long userId);
}
