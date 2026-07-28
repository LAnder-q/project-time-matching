package com.pmtool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实体类型 */
    private String entityType;

    /** 实体ID */
    private Long entityId;

    /** 操作类型: CREATE/UPDATE/DELETE */
    private String action;

    /** 旧值JSON */
    private String oldValue;

    /** 新值JSON */
    private String newValue;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operateTime;
}
