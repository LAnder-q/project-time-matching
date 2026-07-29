package com.pmtool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（MD5） */
    private String password;

    /** 角色：ADMIN / PROJECT_LEAD / USER */
    private String role;

    /** 真实姓名 */
    private String realName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
