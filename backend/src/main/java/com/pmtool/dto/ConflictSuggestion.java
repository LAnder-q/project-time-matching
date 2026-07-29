package com.pmtool.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConflictSuggestion {
    private Long personnelId;
    private String personnelName;
    private String empNo;
    private Long conflictAssignmentId;  // 冲突的分配ID
    private Long projectId;  // 冲突涉及的项目ID
    private String projectName;
    private String suggestionType;  // REPLACE_PERSONNEL 或 ADJUST_HOURS
    private String description;  // 建议描述
    private List<ReplacementCandidate> candidates;  // 替代候选人列表（仅 REPLACE_PERSONNEL 时有值）
    private String adjustAdvice;  // 工时调整建议（仅 ADJUST_HOURS 时有值）
    
    @Data
    public static class ReplacementCandidate {
        private Long personnelId;
        private String name;
        private String empNo;
        private String position;
        private String skills;
        private Double matchScore;  // 技能匹配度 0-100
        private Double availabilityRate;  // 可用率 0-100（项目周期内空闲天数占比）
        private String recommendationLevel;  // 推荐档位：RECOMMENDED / CONSIDERABLE / NOT_RECOMMENDED
        private String reason;  // 推荐理由
    }
}
