# 人员-项目时间匹配管理工具

运维人员多项目时间冲突检测与可视化管理系统。

## 技术栈

| 层面 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + Java 21 + MyBatis-Plus 3.5 |
| 前端 | Vue 3.4 + TypeScript + Vite + Element Plus |
| 数据库 | MySQL 8.x |
| 日历组件 | FullCalendar 6 |
| 报表导出 | EasyExcel + Hutool |

## 项目结构

```
project-time-matching/
├── backend/          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pmtool/
│       │   ├── PmToolApplication.java
│       │   ├── config/         # 跨域、分页配置
│       │   ├── common/         # 统一响应体
│       │   ├── entity/         # 实体类
│       │   ├── mapper/         # MyBatis-Plus Mapper
│       │   ├── dto/            # 数据传输对象
│       │   ├── service/        # 业务逻辑（含冲突检测算法）
│       │   └── controller/     # REST API
│       └── resources/
│           ├── application.yml
│           └── sql/init.sql    # 建表脚本
└── frontend/         # Vue 3 前端
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                # Axios 封装 + 接口模块
        ├── router/             # 路由配置
        ├── stores/             # Pinia 状态管理
        ├── types/              # TypeScript 类型定义
        ├── views/              # 页面组件
        └── components/         # 公共组件
```

## 快速开始

### 1. 数据库准备

打开 Navicat，连接 MySQL，执行建表脚本：

```sql
CREATE DATABASE IF NOT EXISTS pm_tool DEFAULT CHARACTER SET utf8mb4;
```

然后导入 `backend/src/main/resources/sql/init.sql`。

### 2. 启动后端

1. 用 IDEA 打开 `backend` 目录
2. 等待 Maven 自动导入依赖
3. 修改 `application.yml` 中的数据库密码（默认 root/123456）
4. 运行 `PmToolApplication.java`
5. 后端启动在 http://localhost:8080/api

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 http://localhost:5173

### 4. 登录系统

- 账号：admin
- 密码：123456

## 核心功能

| 功能 | 说明 |
|------|------|
| 人员管理 | 人员信息 CRUD、批量导入 |
| 项目管理 | 项目信息 CRUD、优先级设置 |
| 分配管理 | 人员-项目时间分配、历史版本追溯 |
| 冲突检测 | 自动识别同一人员多项目时间重叠 |
| 日历可视化 | 日/周/月视图展示人员时间分配，项目分色标识 |
| 报表导出 | Excel/PDF 导出（待实现） |

## API 接口

| 模块 | 路径 | 方法 |
|------|------|------|
| 人员 | /api/personnel | GET/POST/PUT/DELETE |
| 项目 | /api/project | GET/POST/PUT/DELETE |
| 分配 | /api/assignment | GET/POST/PUT/DELETE |
| 冲突检测 | /api/conflict/detect | GET |
| 人员冲突 | /api/conflict/personnel/{id} | GET |
| 日历数据 | /api/conflict/calendar | GET |
