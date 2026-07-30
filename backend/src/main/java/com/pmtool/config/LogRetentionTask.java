package com.pmtool.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pmtool.entity.OperationLog;
import com.pmtool.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志留存定时任务
 * 需求：操作日志留存 ≥ 24 个月
 * 策略：每月 1 日凌晨 2 点执行，归档超过 24 个月的日志
 * 归档方式：将超期日志的 action 字段标记为 "ARCHIVED" 前缀，保留数据但不影响正常查询
 */
@Component
public class LogRetentionTask {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionTask.class);

    /** 日志留存期限：24 个月 */
    private static final int RETENTION_MONTHS = 24;

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 每月 1 日凌晨 2 点执行日志归档
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(RETENTION_MONTHS);
        log.info("开始归档 {} 之前的操作日志", cutoff);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(OperationLog::getOperateTime, cutoff);
        wrapper.notLike(OperationLog::getAction, "ARCHIVED%");

        var expiredLogs = operationLogMapper.selectList(wrapper);
        if (expiredLogs.isEmpty()) {
            log.info("无需归档的过期日志");
            return;
        }

        int count = 0;
        for (OperationLog logEntry : expiredLogs) {
            // 标记为已归档，保留原始数据
            String originalAction = logEntry.getAction();
            if (originalAction != null && !originalAction.startsWith("ARCHIVED")) {
                logEntry.setAction("ARCHIVED_" + originalAction);
                operationLogMapper.updateById(logEntry);
                count++;
            }
        }

        log.info("操作日志归档完成，共归档 {} 条记录", count);
    }
}
