package com.pmtool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员实体
 */
@Data
@TableName("personnel")
public class Personnel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 岗位 */
    private String position;

    /** 技能（逗号分隔） */
    private String skills;

    /** 可用开始日期 */
    private LocalDate availableStartDate;

    /** 可用结束日期 */
    private LocalDate availableEndDate;

    /** 逻辑删除标志 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
