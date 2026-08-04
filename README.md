# 人员-项目时间匹配管理系统

> 运维人员多项目时间冲突检测与可视化管理系统。高效管理人员与项目的时间分配，智能检测冲突，辅助管理者合理排期，提升团队资源利用率。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)
![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 目录

- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [核心功能详解](#核心功能详解)
- [权限系统](#权限系统)
- [数据库设计](#数据库设计)
- [API 接口文档](#api-接口文档)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [常见问题](#常见问题)
- [开发指南](#开发指南)

---

## 功能特性

- **人员管理** — 增删改查、按姓名/工号/岗位搜索、Excel 批量导入（工号查重）
- **项目管理** — 增删改查、按项目名搜索、优先级设置（1-5 星）、所需岗位/每周工时配置、Excel 批量导入（项目名查重）
- **分配管理** — 人员与项目时间分配、双重筛选、版本号追溯、操作日志审计
- **冲突检测** — 自动检测时间区间重叠、三级严重程度分级（HIGH / MEDIUM / LOW）
- **智能替换推荐** — 岗位/技能匹配（Jaccard 相似度）+ 项目周期可用率计算、三档推荐分级
- **日历可视化** — FullCalendar 日/周/月视图、人员/项目/时间区间三维度筛选、冲突红色高亮
- **报表导出** — 三种报表 × 两种格式（Excel + PDF）、中文 PDF 支持
- **权限控制** — JWT 认证、三级角色（ADMIN / PROJECT_LEAD / USER）、菜单按角色过滤
- **数据安全** — 人员工号、项目名称 AES 对称加密存储，读取自动解密；操作日志自动归档留存 24 个月
- **设计体系** — Indigo Workspace 设计系统、CSS 变量驱动、响应式布局

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                    浏览器 (Browser)                    │
│  Vue 3 + TypeScript + Element Plus + FullCalendar    │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / RESTful API
                       │ (Vite 代理 /api → :8080)
┌──────────────────────▼──────────────────────────────┐
│              Spring Boot 后端 (:8080/api)              │
│  ┌──────────┐  ┌─────────────┐  ┌────────────────┐  │
│  │Controller│→ │   Service   │→ │     Mapper     │  │
│  └──────────┘  └─────────────┘  └───────┬────────┘  │
│  ┌──────────────────┐  ┌──────────────┐ │           │
│  │  JwtInterceptor  │  │ Utils(JWT/PDF)│ │           │
│  └──────────────────┘  └──────────────┘ │           │
└─────────────────────────────────────────┼───────────┘
                                          │
                    ┌─────────────────────▼───────────┐
                    │          MySQL 8.x 数据库         │
                    │  sys_user / personnel / project  │
                    │  assignment / operation_log      │
                    └─────────────────────────────────┘
```

采用前后端分离架构，前端通过 Vite 开发代理或 Nginx 反向代理与后端通信，后端提供 RESTful API，使用 JWT 进行身份认证。

## 技术栈

| 层面 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 开发语言 | Java | 17 |
| ORM 框架 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.x |
| 认证方案 | JWT (JJWT) | 0.12.5 |
| Excel 导出 | EasyExcel | 3.3.3 |
| PDF 导出 | iText 7 (含亚洲字体) | 7.2.5 |
| 工具库 | Hutool | 5.8.25 |
| 前端框架 | Vue 3 + TypeScript | 3.4 |
| 构建工具 | Vite | 5.0 |
| UI 组件库 | Element Plus | 2.5 |
| 日历组件 | FullCalendar | 6.1 |
| 状态管理 | Pinia | 2.1 |
| HTTP 库 | Axios | 1.6 |

## 项目结构

```
project-time-matching/
├── start.bat                         # 一键启动脚本（双击运行）
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/main/
│       ├── java/com/pmtool/
│       │   ├── PmToolApplication.java       # 启动类
│       │   ├── config/                      # 配置类
│       │   │   ├── WebMvcConfig.java        #   JWT 拦截器注册
│       │   │   ├── CorsConfig.java          #   跨域配置
│       │   │   └── MyBatisPlusConfig.java   #   分页插件配置
│       │   ├── common/                      # 通用组件
│       │   │   ├── Result.java              #   统一响应体
│       │   │   └── PageResult.java          #   分页结果封装
│       │   ├── entity/                      # 实体类 (5个)
│       │   │   ├── SysUser.java             #   系统用户
│       │   │   ├── Personnel.java           #   人员
│       │   │   ├── Project.java             #   项目
│       │   │   ├── Assignment.java          #   分配
│       │   │   └── OperationLog.java        #   操作日志
│       │   ├── dto/                         # 数据传输对象 (10个)
│       │   │   ├── LoginDTO / LoginVO       #   登录请求/响应
│       │   │   ├── AssignmentDTO / VO       #   分配请求/响应
│       │   │   ├── ConflictResult / Detail  #   冲突检测结果
│       │   │   ├── ConflictSuggestion       #   调优建议(含候选人)
│       │   │   ├── CalendarEvent            #   日历事件
│       │   │   ├── PersonnelImportDTO       #   人员Excel导入
│       │   │   └── ProjectImportDTO         #   项目Excel导入
│       │   ├── mapper/                      # MyBatis-Plus Mapper (5个)
│       │   ├── interceptor/
│       │   │   └── JwtInterceptor.java      #   JWT 认证拦截器
│       │   ├── utils/                       # 工具类
│       │   │   ├── JwtUtils.java            #   JWT 生成与解析
│       │   │   └── PdfExportUtils.java      #   PDF 导出(iText 7)
│       │   ├── service/                     # 业务逻辑层
│       │   │   ├── AuthService              #   认证服务
│       │   │   ├── PersonnelService         #   人员服务
│       │   │   ├── ProjectService           #   项目服务
│       │   │   ├── AssignmentService        #   分配服务
│       │   │   └── ConflictDetectionService #   冲突检测服务(核心算法)
│       │   └── controller/                  # REST API (6个)
│       │       ├── AuthController           #   认证接口
│       │       ├── PersonnelController      #   人员管理
│       │       ├── ProjectController        #   项目管理
│       │       ├── AssignmentController     #   分配管理
│       │       ├── ConflictController       #   冲突检测与日历
│       │       └── ReportController         #   报表导出
│       └── resources/
│           ├── application.yml              # 应用配置
│           └── sql/init.sql                 # 数据库初始化脚本
├── frontend/                         # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts                # Vite 配置(含 API 代理)
│   └── src/
│       ├── main.ts                   # 应用入口
│       ├── App.vue                   # 根组件
│       ├── api/                      # API 请求封装 (7个模块)
│       │   ├── request.ts            #   Axios 实例与拦截器
│       │   ├── auth.ts               #   认证 API
│       │   ├── personnel.ts          #   人员 API
│       │   ├── project.ts            #   项目 API
│       │   ├── assignment.ts         #   分配 API
│       │   ├── conflict.ts           #   冲突检测 API
│       │   └── report.ts             #   报表导出 API
│       ├── router/index.ts           # 路由配置(含角色守卫)
│       ├── stores/user.ts            # Pinia 用户状态管理
│       ├── types/index.ts            # TypeScript 类型定义
│       ├── components/StatCard.vue   # 统计卡片组件
│       ├── views/                    # 页面组件 (9个)
│       │   ├── Login.vue             #   登录页(分屏设计)
│       │   ├── Layout.vue            #   主布局(侧边栏+顶栏)
│       │   ├── Dashboard.vue         #   仪表盘
│       │   ├── PersonnelManage.vue   #   人员管理
│       │   ├── ProjectManage.vue     #   项目管理
│       │   ├── AssignmentManage.vue  #   分配管理
│       │   ├── ConflictList.vue      #   冲突清单
│       │   ├── CalendarView.vue      #   日历视图
│       │   └── NotFound.vue          #   404 页面
│       └── assets/styles/main.scss   # 全局样式(Indigo 设计系统)
├── tech-doc/                         # 技术文档目录
│   └── tech-doc.html                 #   完整技术文档(浏览器打开可查看)
└── README.md
```

## 快速开始

### 一键启动（推荐）

项目根目录提供 `start.bat` 一键启动脚本，**双击即可**完成全部启动流程，无需手动安装依赖或配置环境：

```
① 自动检测 JDK / Maven / Node.js
② 自动检测 MySQL 服务是否运行
③ 自动检查 pm_tool 数据库，不存在则自动建库并导入 init.sql
④ 启动后端服务 (http://localhost:8080/api)
⑤ 启动前端服务 (http://localhost:5173)
⑥ 自动打开浏览器
```

> 提示：
> - 脚本会自动探测本机 Java 17、Maven、MySQL 客户端路径，无需提前配置环境变量
> - 如只想检查环境是否就绪而不启动服务，可在命令行运行 `start.bat --check`
> - 默认数据库账号密码为 `root / 123456`，如需修改请同时更新 `backend/src/main/resources/application.yml` 和 `start.bat` 开头的 `MYSQL_USER / MYSQL_PASS`
> - 关闭「PMTool-Backend」「PMTool-Frontend」两个窗口即可停止系统

### 环境要求

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端依赖管理 |
| Node.js | 18+ | 前端构建环境 |
| MySQL | 8.0+ | 数据库 |
| IDEA | — | 后端开发工具（推荐） |
| Navicat | — | 数据库管理工具（推荐） |

### 1. 数据库准备

打开 Navicat，连接 MySQL，执行建库脚本：

```sql
CREATE DATABASE IF NOT EXISTS pm_tool DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后导入 `backend/src/main/resources/sql/init.sql`，该脚本会创建全部 5 张表并插入测试数据：

| 数据 | 数量 | 说明 |
|------|------|------|
| 系统用户 | 3 | admin / project_lead / operator |
| 人员 | 5 | 运维工程师、DBA 等 |
| 项目 | 5 | 不同优先级和周期 |
| 分配记录 | 9 | 含刻意制造的冲突场景 |
| 操作日志 | 9 | 与 9 条分配一一对应的“新增”记录，初始化后即可追溯 |

### 2. 启动后端

1. 用 IDEA 打开 `backend` 目录
2. 等待 Maven 自动导入依赖
3. 修改 `application.yml` 中的数据库连接信息（默认 `root/123456`）
4. 运行 `PmToolApplication.java`
5. 后端启动在 http://localhost:8080/api

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 http://localhost:5173，Vite 会自动将 `/api` 请求代理到后端 8080 端口。

### 4. 登录系统

| 角色 | 账号 | 密码 | 可访问菜单 |
|------|------|------|------------|
| 系统管理员 | admin | 123456 | 全部菜单 |
| 项目负责人 | project_lead | 123456 | 仪表盘、人员、项目、分配、冲突、日历 |
| 运维人员 | operator | 123456 | 仪表盘、冲突清单、日历（只读） |

## 核心功能详解

### 人员管理

人员信息的增删改查、按姓名/工号/岗位搜索。支持 Excel 批量导入：前端解析 Excel 文件后以 JSON 数组提交至 `/personnel/batch` 接口，后端按工号查重，已存在则更新，不存在则新增。

### 项目管理

项目信息的增删改查、按项目名搜索、优先级设置（1-5 星）、所需岗位与每日/每周工时配置。支持 Excel 批量导入：前端解析 Excel 文件后以 JSON 数组提交至 `/project/batch` 接口，后端按项目名查重，已存在则更新，不存在则新增。

### 分配管理

人员与项目的时间分配记录管理，支持按人员/项目双重筛选。每次更新自动版本号 +1，并记录操作日志到 `operation_log` 表实现历史版本追溯。

### 冲突检测

自动检测同一人员在多个项目中的时间区间重叠冲突。核心算法：两个区间 `[start1, end1]` 和 `[start2, end2]` 重叠的条件是 `start1 ≤ end2 && start2 ≤ end1`。按重叠天数分三级严重程度：

| 级别 | 条件 | 说明 |
|------|------|------|
| HIGH | 重叠 ≥ 30 天 | 严重冲突，需立即处理 |
| MEDIUM | 重叠 ≥ 7 天 | 中等冲突，建议调整 |
| LOW | 重叠 < 7 天 | 轻微冲突，可观察 |

### 智能替换推荐

当检测到冲突后，对低优先级项目自动推荐替代人员，基于两个维度评分：

- **岗位/技能匹配度**：优先使用项目的 `requiredPosition`（所需岗位）与候选人员的 `positions` 计算 Jaccard 相似度；若项目未设置所需岗位则回退到冲突人员技能匹配
- **可用率**：基于低优先级项目完整周期计算空闲天数占比

三档推荐分级：

| 档位 | 条件 | 标签颜色 |
|------|------|----------|
| 推荐 | 可用率 ≥ 80% 且 匹配度 ≥ 50% | 绿色 |
| 可考虑 | 可用率 ≥ 50% 或 匹配度 ≥ 30% | 橙色 |
| 不推荐 | 可用率 < 50% 且 匹配度 < 30% | 灰色 |

### 日历可视化

使用 FullCalendar 展示人员-项目分配，支持日/周/月视图切换。支持人员、项目名称、时间区间三个维度筛选。每个项目分配独立颜色（后端统一控制，调色板不含红色），冲突分配以红色高亮标识。

### 报表导出

支持三种报表的 Excel 和 PDF 双格式导出，通过 `format` 参数切换（`xlsx` / `pdf`）：

| 报表 | 接口路径 | 说明 |
|------|----------|------|
| 人员时间分配表 | `/report/export/assignment` | 人员与项目的时间分配明细 |
| 项目人员冲突报表 | `/report/export/conflict` | 人员时间冲突详情汇总 |
| 人员利用率统计 | `/report/export/utilization` | 人员工时利用率分析 |

## 权限系统

系统采用 JWT 认证 + 三级角色权限控制：

| 角色 | 代码 | 权限范围 |
|------|------|----------|
| 系统管理员 | `ADMIN` | 全部功能 |
| 项目负责人 | `PROJECT_LEAD` | 仪表盘、人员、项目、分配、冲突、日历 |
| 运维人员 | `USER` | 仪表盘、冲突清单、日历（只读） |

**认证流程：**

1. 用户登录 → 后端验证账号密码 → 生成 JWT Token（有效期 24 小时）
2. 前端存储 Token 到 localStorage，通过 Axios 请求拦截器自动添加 `Authorization: Bearer <token>` 头部
3. 后端 `JwtInterceptor` 拦截全部请求（除 `/auth/login`），解析并验证 Token
4. 文件下载场景支持通过 `?token=xxx` query 参数传递 Token
5. 前端路由守卫进行角色校验（统一转大写比较），菜单按角色过滤显示

## 数据库设计

### 表结构概览

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `sys_user` | 系统用户表 | id, username, password, role, real_name |
| `personnel` | 人员表 | id, emp_no, name, position, skills, available_start/end_date |
| `project` | 项目表 | id, name, start/end_date, priority, required_position, daily_hours, weekly_hours |
| `assignment` | 分配表 | id, personnel_id, project_id, start/end_date, daily_hours, version |
| `operation_log` | 操作日志表 | id, entity_type, entity_id, action, old_value, new_value, operator |

### ER 关系

```
sys_user (1) ──独立── 登录认证
    │
personnel (1) ──< assignment >── (1) project
    │                                  │
    └────── operation_log ─────────────┘
         (记录 assignment 变更历史)
```

- `personnel` 与 `project` 为多对多关系，通过 `assignment` 表关联
- `assignment` 表包含 `version` 字段，每次更新自增，配合 `operation_log` 实现版本追溯
- 所有业务表使用 `deleted` 字段（0=未删除, 1=已删除）实现逻辑删除
- 密码使用 MD5 加密存储
- 敏感业务字段（人员工号、项目名称）使用 AES 对称加密存储（`DataEncryptUtils`），
  加密密钥在 `application.yml` 的 `data.encrypt.key` 配置；解密失败时兼容返回原文，支持历史明文数据平滑迁移

## API 接口文档

所有接口前缀为 `/api`，除登录接口外均需携带 JWT Token。

### 认证模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录，返回 JWT Token | 否 |
| GET | `/auth/info` | 获取当前登录用户信息 | 是 |

### 人员管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/personnel` | 分页查询（参数：pageNum, pageSize, name, position） |
| GET | `/personnel/list` | 查询全部（供下拉选择） |
| GET | `/personnel/{id}` | 根据 ID 查询 |
| POST | `/personnel` | 新增人员 |
| PUT | `/personnel/{id}` | 更新人员 |
| DELETE | `/personnel/{id}` | 删除人员（逻辑删除） |
| POST | `/personnel/batch` | 批量导入（JSON 数组，工号查重） |

### 项目管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/project` | 分页查询（参数：pageNum, pageSize, name） |
| GET | `/project/list` | 查询全部（供下拉选择） |
| GET | `/project/{id}` | 根据 ID 查询 |
| POST | `/project` | 新增项目 |
| PUT | `/project/{id}` | 更新项目 |
| DELETE | `/project/{id}` | 删除项目（逻辑删除） |
| POST | `/project/batch` | 批量导入（JSON 数组，项目名查重） |

### 分配管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/assignment` | 条件查询（参数：personnelId, projectId） |
| POST | `/assignment` | 创建分配（同时记录操作日志） |
| PUT | `/assignment/{id}` | 更新分配（版本号 +1，记录操作日志） |
| DELETE | `/assignment/{id}` | 删除分配（逻辑删除） |

### 冲突检测

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/conflict/detect` | 检测所有人员的分配冲突 |
| GET | `/conflict/personnel/{id}` | 检测指定人员的分配冲突 |
| GET | `/conflict/calendar` | 获取日历数据（参数：personnelId, projectId, startDate, endDate） |
| GET | `/conflict/suggestions` | 生成冲突调优建议（含替换候选人） |

### 报表导出

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/report/export/assignment` | 人员时间分配表（参数：format=xlsx/pdf） |
| GET | `/report/export/conflict` | 项目人员冲突报表（参数：format=xlsx/pdf） |
| GET | `/report/export/utilization` | 人员利用率统计（参数：format=xlsx/pdf） |

> 完整的接口请求/响应示例和字段说明请参阅 [技术文档](tech-doc/tech-doc.html)。

## 配置说明

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api          # API 统一前缀

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pm_tool?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root              # 修改为你的数据库用户名
    password: 123456            # 修改为你的数据库密码

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰
  global-config:
    db-config:
      logic-delete-field: deleted        # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: pm-tool-secret-key-2026-for-hs256-algorithm-must-be-long-enough   # 生产环境请修改
  expiration: 86400000          # Token 有效期：24 小时（毫秒）
```

### 前端配置 (`vite.config.ts`)

```typescript
server: {
  port: 5173,                   // 开发服务器端口
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true
    }
  }
}
```

## 部署指南

### 后端打包

```bash
cd backend
mvn clean package -DskipTests
java -jar target/pm-tool-backend-1.0.0.jar
```

生产环境可通过参数覆盖配置：

```bash
java -jar pm-tool-backend-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://生产数据库:3306/pm_tool \
  --spring.datasource.password=生产密码 \
  --jwt.secret=生产环境密钥
```

### 前端打包

```bash
cd frontend
npm run build
```

构建产物在 `frontend/dist/`。

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

> `try_files $uri $uri/ /index.html` 确保前端路由在刷新时不会 404。

## 常见问题

### Q: 启动后端报数据库连接失败？

确认 MySQL 已启动，检查 `application.yml` 中的 `url`、`username`、`password` 是否正确。确保 `pm_tool` 数据库已创建并导入了 `init.sql`。

### Q: 前端页面空白或报 404？

开发环境下确认后端已启动（8080 端口），Vite 代理配置正确。生产环境确认 Nginx 配置了 `try_files` 指向 `index.html`。

### Q: 登录后页面空白或无限重定向？

检查浏览器 localStorage 中是否存在旧的 mock token（以 `mock-token-` 开头），清除后重新登录。系统已内置自动清理逻辑，但手动清除可解决极端情况。

### Q: 日历视图中所有分配都显示红色？

冲突标记为人员 + 项目级别，仅时间重叠的分配标红。如果全部标红，检查后端冲突检测逻辑是否将冲突标记精确到人员+项目维度，而非人员级别。

### Q: PDF 导出中文乱码？

后端使用 iText 7 的 `font-asian` 依赖处理中文。确认 `pom.xml` 中已引入 `com.itextpdf:font-asian` 依赖。

### Q: 批量导入失败？

批量导入支持 Excel 文件上传和文本批量导入两种方式。Excel 导入通过后端 EasyExcel 解析；文本导入由前端解析为 JSON 数组后提交至 `/batch` 接口。确认 Excel 表头与模板一致（项目导入含：项目名称、开始日期、结束日期、优先级、所需岗位、每日工时、每周工时），且数据格式正确。

## 开发指南

### 项目约定

- 后端遵循 Controller → Service → Mapper 三层架构
- 统一使用 `Result<T>` 包装响应体，包含 `code`、`message`、`data` 字段
- 实体类使用 Lombok 简化代码，MyBatis-Plus 注解映射数据库
- 前端 API 请求统一封装在 `src/api/` 目录，通过 `request.ts` 的 Axios 实例发送
- 前端路由守卫在 `router/index.ts` 中实现角色权限校验
- 项目颜色由后端统一分配，前端直接使用 `color` 字段，调色板不含红色（红色专属冲突标记）
- 所有角色比较统一转为大写

### 开发命令

```bash
# 前端开发
cd frontend
npm run dev              # 启动开发服务器
npm run build            # 生产构建
npm run type-check       # TypeScript 类型检查

# 后端开发
cd backend
mvn spring-boot:run      # 启动后端（命令行）
# 或在 IDEA 中直接运行 PmToolApplication.java
mvn clean package        # 打包
```

### 添加新页面

1. 在 `frontend/src/views/` 下创建 `.vue` 组件
2. 在 `router/index.ts` 中添加路由，配置 `meta.roles` 指定可访问角色
3. 在 `Layout.vue` 的菜单配置中添加菜单项
4. 如需后端支持，在 `backend` 中按三层架构添加对应代码

### 添加新 API

1. 后端在对应 Controller 中添加接口方法
2. 前端在 `src/api/` 对应模块中封装请求方法
3. 在页面组件中调用封装好的 API 方法

## 相关文档

- [技术文档](tech-doc/tech-doc.html) — 包含完整的系统架构、文件说明、数据库设计、核心算法和 API 接口详情（浏览器打开即可浏览）

## License

本项目采用 MIT 许可证。
