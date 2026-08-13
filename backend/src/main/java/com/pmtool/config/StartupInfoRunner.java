package com.pmtool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动信息打印：应用版本 + 数据库结构版本
 */
@Component
public class StartupInfoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Override
    public void run(ApplicationArguments args) {
        log.info("============================================================");
        log.info("人员-项目时间匹配管理系统");
        log.info("应用版本: {}", appVersion);
        log.info("数据库结构版本: {}", querySchemaVersion());
        log.info("更新日志: 见项目根目录 CHANGELOG.md");
        log.info("============================================================");
    }

    private String querySchemaVersion() {
        try {
            String v = jdbcTemplate.queryForObject(
                    "SELECT MAX(version) FROM flyway_schema_history WHERE success = 1", String.class);
            return v == null ? "未初始化" : "V" + v;
        } catch (Exception e) {
            log.warn("读取数据库结构版本失败：{}", e.getMessage());
            return "未知";
        }
    }
}
