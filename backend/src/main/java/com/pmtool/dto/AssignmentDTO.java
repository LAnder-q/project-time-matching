package com.pmtool.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人员项目分配创建/更新 DTO
 */
@Data
public class AssignmentDTO {

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

    /** 操作人 */
    private String operator;
}
