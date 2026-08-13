# 更新日志（Changelog）

本项目所有重要变更都会记录在此文件，格式参照 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循语义化版本（SemVer）。

## [Unreleased]

### Added

- 核心逻辑单元测试：冲突检测、智能替换推荐、登录认证、数据加密、Excel 导入校验
- GitHub Actions CI：每次推送自动编译后端、运行单元测试、构建前端

## [1.1.0] - 2026-08-13

### Added

- 数据库结构版本化管理：启动时自动检测并执行未应用的迁移脚本（Flyway）
- 启动横幅与控制台显示应用版本和数据库结构版本
- 新增 `/api/version` 接口，返回应用版本与数据库结构版本
- 新增 `CHANGELOG.md` 更新日志

### Changed

- 数据库初始化脚本迁移为版本化脚本 `V1__init.sql`
- `start.bat` 瘦身为纯启动器，数据库建表/升级交由后端自动完成
- 引入 Maven Wrapper，启动不再依赖本机 Maven 安装路径

## [1.0.0] - 初始版本

### Added

- 人员、项目、分配管理，Excel 批量导入
- 时间冲突检测与智能替换推荐
- 日历可视化与报表导出（Excel / PDF）
- 三级权限体系（ADMIN / PROJECT_LEAD / USER）与 JWT 认证
- 敏感数据加密存储与操作日志归档
