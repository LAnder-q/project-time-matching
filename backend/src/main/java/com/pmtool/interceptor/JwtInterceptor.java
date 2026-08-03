package com.pmtool.interceptor;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmtool.annotation.RequireRole;
import com.pmtool.common.Result;
import com.pmtool.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 认证与授权拦截器
 * 1. 校验请求头中的 Authorization: Bearer <token>（认证）
 * 2. 根据 @RequireRole 注解校验角色权限（授权）
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;

        // 优先从 Authorization Header 获取 token
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 回退：从 query 参数获取 token（用于 window.open 等文件下载场景）
            token = request.getParameter("token");
        }

        if (StrUtil.isBlank(token)) {
            writeUnauthorized(response, "未提供有效的认证令牌");
            return false;
        }

        if (!jwtUtils.validateToken(token)) {
            writeUnauthorized(response, "认证令牌已过期或无效，请重新登录");
            return false;
        }

        try {
            Claims claims = jwtUtils.parseToken(token);
            String role = claims.get("role", String.class);

            // 将用户信息存入 request 属性，供 Controller 使用
            request.setAttribute("userId", Long.parseLong(claims.getSubject()));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("role", role);

            // ========== 角色权限校验（授权） ==========
            // 若 Handler 方法或所在类标注了 @RequireRole，则校验当前用户角色是否在允许列表中
            if (handler instanceof HandlerMethod handlerMethod) {
                RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
                if (requireRole == null) {
                    requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
                }
                if (requireRole != null) {
                    String[] allowedRoles = requireRole.value();
                    boolean hasRole = false;
                    if (role != null) {
                        for (String allowed : allowedRoles) {
                            if (allowed.equalsIgnoreCase(role)) {
                                hasRole = true;
                                break;
                            }
                        }
                    }
                    if (!hasRole) {
                        writeForbidden(response,
                                "权限不足，当前角色[" + role + "]无权访问，需要[" + Arrays.toString(allowedRoles) + "]角色");
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "认证令牌解析失败");
            return false;
        }
    }

    /**
     * 返回 401 未认证响应
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 返回 403 无权限响应
     */
    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
