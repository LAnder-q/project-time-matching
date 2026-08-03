package com.pmtool.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 人员 Excel 导入 DTO
 * 用于 EasyExcel 解析上传的 Excel 文件
 *
 * 列顺序：工号 | 姓名 | 部门名称 | 岗位 | 技能 | 可用开始日期 | 可用结束日期
 */
@Data
public class PersonnelImportDTO {

    @ExcelProperty(value = "工号", index = 0)
    private String empNo;

    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ExcelProperty(value = "部门名称", index = 2)
    private String deptName;

    @ExcelProperty(value = "岗位", index = 3)
    private String positions;

    @ExcelProperty(value = "技能", index = 4)
    private String skills;

    @ExcelProperty(value = "可用开始日期", index = 5)
    private String availableStartDate;

    @ExcelProperty(value = "可用结束日期", index = 6)
    private String availableEndDate;
}
