package com.pmtool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目实体
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目开始日期 */
    private LocalDate startDate;

    /** 项目结束日期 */
    private LocalDate endDate;

    /** 优先级 1-5, 5最高 */
    private Integer priority;

    /** 所需人员岗位（逗号分隔，如：运维工程师,DBA） */
    private String requiredPosition;

    /** 每日所需工时 */
    private BigDecimal dailyHours;

    /** 每周所需工时 */
    private BigDecimal weeklyHours;

    /** 逻辑删除标志 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
