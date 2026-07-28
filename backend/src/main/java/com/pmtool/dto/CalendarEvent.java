package com.pmtool.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日历视图事件
 */
@Data
public class CalendarEvent {

    /** 事件ID */
    private Long id;

    /** 事件标题 */
    private String title;

    /** 开始日期 */
    private LocalDate start;

    /** 结束日期 */
    private LocalDate end;

    /** 显示颜色（十六进制色值，按项目分配） */
    private String color;

    /** 人员ID */
    private Long personnelId;

    /** 人员姓名 */
    private String personnelName;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 每日工时 */
    private BigDecimal dailyHours;
}
