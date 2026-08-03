package com.pmtool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解
 * 标注在 Controller 方法或类上，指定允许访问的角色
 * 三级角色：ADMIN（管理员）、PROJECT_LEAD（项目负责人）、USER（普通运维人员）
 *
 * 用法示例：
 *   @RequireRole({"ADMIN", "PROJECT_LEAD"})  // 管理员和项目负责人可访问
 *   @RequireRole({"ADMIN"})                   // 仅管理员可访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 允许访问的角色列表
     */
    String[] value();
}
