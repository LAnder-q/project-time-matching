package com.pmtool.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目 Excel 导入 DTO
 * 用于 EasyExcel 解析上传的 Excel 文件
 *
 * 列顺序：项目名称 | 开始日期 | 结束日期 | 优先级 | 所需岗位 | 每日工时 | 每周工时
 */
@Data
public class ProjectImportDTO {

    @ExcelProperty(value = "项目名称", index = 0)
    private String name;

    @ExcelProperty(value = "开始日期", index = 1)
    private String startDate;

    @ExcelProperty(value = "结束日期", index = 2)
    private String endDate;

    @ExcelProperty(value = "优先级", index = 3)
    private Integer priority;

    @ExcelProperty(value = "所需岗位", index = 4)
    private String requiredPosition;

    @ExcelProperty(value = "每日工时", index = 5)
    private BigDecimal dailyHours;

    @ExcelProperty(value = "每周工时", index = 6)
    private BigDecimal weeklyHours;
}
