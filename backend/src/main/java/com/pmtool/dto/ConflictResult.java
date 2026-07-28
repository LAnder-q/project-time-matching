package com.pmtool.dto;

import lombok.Data;

import java.util.List;

/**
 * 冲突检测结果（按人员维度）
 */
@Data
public class ConflictResult {

    /** 人员ID */
    private Long personnelId;

    /** 人员姓名 */
    private String personnelName;

    /** 工号 */
    private String empNo;

    /** 冲突详情列表 */
    private List<ConflictDetail> conflicts;
}
