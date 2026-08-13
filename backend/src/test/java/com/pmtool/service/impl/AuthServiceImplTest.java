package com.pmtool.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.pmtool.dto.LoginDTO;
import com.pmtool.dto.LoginVO;
import com.pmtool.entity.SysUser;
import com.pmtool.mapper.SysUserMapper;
import com.pmtool.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录认证单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("用户名密码正确时登录成功并签发 Token")
    void loginSuccess() {
        SysUser user = user(1L, "admin", SecureUtil.md5("123456"), "ADMIN", "系统管理员");
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(jwtUtils.generateToken(1L, "admin", "ADMIN", "系统管理员")).thenReturn("token-abc");

        LoginVO vo = authService.login(login("admin", "123456"));

        assertEquals("token-abc", vo.getToken());
        assertEquals("ADMIN", vo.getRole());
        assertEquals("admin", vo.getUsername());
        verify(jwtUtils).generateToken(1L, "admin", "ADMIN", "系统管理员");
    }

    @Test
    @DisplayName("密码错误时登录失败")
    void loginWrongPassword() {
        SysUser user = user(1L, "admin", SecureUtil.md5("123456"), "ADMIN", "系统管理员");
        when(sysUserMapper.selectOne(any())).thenReturn(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(login("admin", "654321")));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("用户不存在时登录失败")
    void loginUserNotFound() {
        when(sysUserMapper.selectOne(any())).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(login("ghost", "123456")));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("获取当前用户信息成功")
    void getUserInfoSuccess() {
        SysUser user = user(1L, "admin", SecureUtil.md5("123456"), "ADMIN", "系统管理员");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        LoginVO vo = authService.getUserInfo(1L);

        assertEquals("admin", vo.getUsername());
        assertEquals("系统管理员", vo.getRealName());
    }

    @Test
    @DisplayName("用户不存在时获取信息失败")
    void getUserInfoNotFound() {
        when(sysUserMapper.selectById(99L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.getUserInfo(99L));
        assertEquals("用户不存在", ex.getMessage());
    }

    private SysUser user(Long id, String username, String password, String role, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setRealName(realName);
        return user;
    }

    private LoginDTO login(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }
}
