package com.pmtool.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分配记录展示VO（包含人员姓名、工号、项目名称等联表字段）
 */
@Data
public class AssignmentVO {

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

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ===== 联表字段 =====

    /** 人员姓名 */
    private String personnelName;

    /** 工号 */
    private String empNo;

    /** 项目名称 */
    private String projectName;
}
