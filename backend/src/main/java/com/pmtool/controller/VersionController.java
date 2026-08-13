package com.pmtool.controller;

import com.pmtool.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 版本信息接口：返回应用版本与数据库结构版本
 */
@RestController
@RequestMapping("/version")
public class VersionController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.version:unknown}")
    private String appVersion;

    @GetMapping
    public Result<Map<String, String>> version() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("appVersion", appVersion);
        data.put("schemaVersion", querySchemaVersion());
        return Result.ok(data);
    }

    private String querySchemaVersion() {
        try {
            String v = jdbcTemplate.queryForObject(
                    "SELECT MAX(version) FROM flyway_schema_history WHERE success = 1", String.class);
            return v == null ? "none" : "V" + v;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
