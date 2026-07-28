package com.pmtool.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 冲突详情
 */
@Data
public class ConflictDetail {

    /** 项目1 ID */
    private Long projectId1;

    /** 项目1 名称 */
    private String projectName1;

    /** 项目2 ID */
    private Long projectId2;

    /** 项目2 名称 */
    private String projectName2;

    /** 重叠开始日期 */
    private LocalDate overlapStart;

    /** 重叠结束日期 */
    private LocalDate overlapEnd;

    /** 严重程度: HIGH / MEDIUM / LOW */
    private String severity;
}
