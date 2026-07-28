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
 * 人员项目分配实体
 */
@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 人员ID */
    private Long personnelId;

    /** 项目ID */
    private Long projectId;

    /** 分配开始日期 */
    private LocalDate startDate;

    /** 分配结束日期 */
    private LocalDate endDate;

    /** 每日工时 */
    private BigDecimal dailyHours;

    /** 版本号 */
    private Integer version;

    /** 操作人 */
    private String operator;

    /** 逻辑删除标志 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
