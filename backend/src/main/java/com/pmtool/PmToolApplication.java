package com.pmtool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 人员-项目时间匹配管理工具 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.pmtool.mapper")
@EnableScheduling
public class PmToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmToolApplication.class, args);
    }
}
