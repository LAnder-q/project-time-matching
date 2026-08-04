-- ============================================================
-- 人员-项目时间匹配管理工具 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS pm_tool DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pm_tool;

-- ============================================================
-- 部门表 department（支持层级，parent_id 自引用）
-- ============================================================
DROP TABLE IF EXISTS assignment;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS personnel;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE department (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '部门名称',
  code VARCHAR(50) COMMENT '部门编码',
  parent_id BIGINT DEFAULT NULL COMMENT '父部门ID, NULL为顶级部门',
  sort INT DEFAULT 0 COMMENT '同级排序（升序）',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表（支持层级）';

-- ============================================================
-- 人员表 personnel
--   - position 改为 positions（逗号分隔多岗位，支持身兼数职）
--   - 新增 dept_id 关联部门
-- ============================================================
CREATE TABLE personnel (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_no VARCHAR(50) NOT NULL COMMENT '工号',
  name VARCHAR(100) NOT NULL COMMENT '姓名',
  positions VARCHAR(200) COMMENT '岗位（逗号分隔，支持身兼数职）',
  skills VARCHAR(500) COMMENT '技能（逗号分隔）',
  dept_id BIGINT COMMENT '所属部门ID',
  available_start_date DATE COMMENT '可用开始日期',
  available_end_date DATE COMMENT '可用结束日期',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_emp_no (emp_no),
  INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员表';

-- ============================================================
-- 项目表 project
-- ============================================================
CREATE TABLE project (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL COMMENT '项目名称',
  start_date DATE NOT NULL COMMENT '项目开始日期',
  end_date DATE NOT NULL COMMENT '项目结束日期',
  priority INT DEFAULT 3 COMMENT '优先级 1-5, 5最高',
  required_position VARCHAR(200) COMMENT '所需人员岗位（逗号分隔）',
  daily_hours DECIMAL(5,1) DEFAULT 8.0 COMMENT '每日所需工时',
  weekly_hours DECIMAL(5,1) DEFAULT 40.0 COMMENT '每周所需工时',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ============================================================
-- 人员项目分配表 assignment
-- ============================================================
CREATE TABLE assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  personnel_id BIGINT NOT NULL COMMENT '人员ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  start_date DATE NOT NULL COMMENT '分配开始日期',
  end_date DATE NOT NULL COMMENT '分配结束日期',
  daily_hours DECIMAL(5,1) DEFAULT 8.0 COMMENT '每日工时',
  version INT DEFAULT 1 COMMENT '版本号',
  operator VARCHAR(100) COMMENT '操作人',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_personnel (personnel_id),
  INDEX idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员项目分配表';

-- ============================================================
-- 操作日志表 operation_log
-- ============================================================
CREATE TABLE operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  entity_type VARCHAR(50) COMMENT '实体类型',
  entity_id BIGINT COMMENT '实体ID',
  action VARCHAR(50) COMMENT '操作类型: CREATE/UPDATE/DELETE',
  old_value TEXT COMMENT '旧值JSON',
  new_value TEXT COMMENT '新值JSON',
  operator VARCHAR(100) COMMENT '操作人',
  operate_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 系统用户表 sys_user（用于权限管理）
-- ============================================================
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(200) NOT NULL,
  role VARCHAR(20) DEFAULT 'USER' COMMENT 'ADMIN/PROJECT_LEAD/USER',
  real_name VARCHAR(100),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员账号 (密码: 123456 的 MD5)
INSERT INTO sys_user (username, password, role, real_name) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'ADMIN', '系统管理员'),
('project_lead', 'e10adc3949ba59abbe56e057f20f883e', 'PROJECT_LEAD', '项目负责人'),
('operator', 'e10adc3949ba59abbe56e057f20f883e', 'USER', '运维人员');

-- ============================================================
-- 测试数据
-- ============================================================

-- 部门数据（三级层级：技术中心 → 运维部/DBA组 → 基础运维组）
INSERT INTO department (id, name, code, parent_id, sort) VALUES
(1, '技术中心', 'TC', NULL, 1),
(2, '运维部',   'OPS', 1, 1),
(3, 'DBA组',    'DBA', 1, 2),
(4, '基础运维组', 'OPS-BASE', 2, 1);

-- 人员数据（5名运维人员，技能有重叠以便测试调优建议；支持身兼数职）
INSERT INTO personnel (emp_no, name, positions, skills, dept_id, available_start_date, available_end_date) VALUES
('EMP001', '张伟', '运维工程师', 'Linux,Docker,Kubernetes,Shell', 4, '2026-01-01', '2026-12-31'),
('EMP002', '李娜', '运维工程师,DBA', 'Linux,Docker,Python,Ansible', 2, '2026-01-01', '2026-12-31'),
('EMP003', '王强', '高级运维工程师', 'Linux,Kubernetes,Python,Jenkins,CICD', 2, '2026-01-01', '2026-12-31'),
('EMP004', '赵敏', 'DBA,运维工程师', 'MySQL,Redis,Linux,Python,Shell', 3, '2026-01-01', '2026-12-31'),
('EMP005', '刘洋', '运维工程师', 'Linux,Docker,Kubernetes,Shell,Python', 4, '2026-01-01', '2026-12-31');

-- 项目数据（5个项目，优先级不同，含所需岗位和每周工时）
INSERT INTO project (name, start_date, end_date, priority, required_position, daily_hours, weekly_hours) VALUES
('电商平台升级', '2026-02-01', '2026-06-30', 5, '运维工程师,DBA', 8.0, 40.0),
('数据中心迁移', '2026-03-01', '2026-07-31', 4, '运维工程师', 8.0, 40.0),
('安全审计加固', '2026-04-01', '2026-08-31', 3, '运维工程师,DBA', 6.0, 30.0),
('CI/CD流水线建设', '2026-05-01', '2026-09-30', 4, '运维工程师', 8.0, 40.0),
('数据库性能优化', '2026-03-15', '2026-07-15', 2, 'DBA', 6.0, 30.0);

-- 人员项目分配数据（包含刻意制造的冲突）
-- 张伟(EMP001): 同时在电商平台升级和数据中心迁移，时间重叠 → 冲突
-- create_time/update_time 显式指定，便于操作日志中的 JSON 快照与之保持一致
INSERT INTO assignment (personnel_id, project_id, start_date, end_date, daily_hours, version, operator, create_time, update_time) VALUES
(1, 1, '2026-02-01', '2026-06-30', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 张伟 → 电商平台升级
(1, 2, '2026-03-01', '2026-07-31', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 张伟 → 数据中心迁移（与上面重叠 3/1~6/30）

-- 李娜(EMP002): 同时在电商平台升级和安全审计加固，时间重叠 → 冲突
(2, 1, '2026-02-15', '2026-06-15', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 李娜 → 电商平台升级
(2, 3, '2026-04-01', '2026-08-31', 6.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 李娜 → 安全审计加固（与上面重叠 4/1~6/15）

-- 王强(EMP003): 同时在数据中心迁移和CI/CD建设，时间重叠 → 冲突
(3, 2, '2026-03-01', '2026-07-31', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 王强 → 数据中心迁移
(3, 4, '2026-05-01', '2026-09-30', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 王强 → CI/CD建设（与上面重叠 5/1~7/31）

-- 赵敏(EMP004): 同时在数据库性能优化和安全审计加固，时间重叠 → 冲突
(4, 5, '2026-03-15', '2026-07-15', 6.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 赵敏 → 数据库性能优化
(4, 3, '2026-04-15', '2026-08-31', 4.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),   -- 赵敏 → 安全审计加固（与上面重叠 4/15~7/15）

-- 刘洋(EMP005): 只分配一个项目，无冲突（可用于测试调优建议中的替代候选人）
(5, 4, '2026-06-01', '2026-09-30', 8.0, 1, 'admin', '2026-01-15 10:00:00', '2026-01-15 10:00:00');   -- 刘洋 → CI/CD建设（无冲突）

-- ============================================================
-- 操作日志：与 9 条分配一一对应的“新增”记录
-- 使初始化后的数据在网页上可直接追溯（分配管理-历史、操作日志页）
-- ============================================================
INSERT INTO operation_log (entity_type, entity_id, action, old_value, new_value, operator, operate_time) VALUES
('ASSIGNMENT', 1, 'CREATE', NULL, '{"id":1,"personnelId":1,"projectId":1,"startDate":"2026-02-01","endDate":"2026-06-30","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 2, 'CREATE', NULL, '{"id":2,"personnelId":1,"projectId":2,"startDate":"2026-03-01","endDate":"2026-07-31","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 3, 'CREATE', NULL, '{"id":3,"personnelId":2,"projectId":1,"startDate":"2026-02-15","endDate":"2026-06-15","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 4, 'CREATE', NULL, '{"id":4,"personnelId":2,"projectId":3,"startDate":"2026-04-01","endDate":"2026-08-31","dailyHours":6.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 5, 'CREATE', NULL, '{"id":5,"personnelId":3,"projectId":2,"startDate":"2026-03-01","endDate":"2026-07-31","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 6, 'CREATE', NULL, '{"id":6,"personnelId":3,"projectId":4,"startDate":"2026-05-01","endDate":"2026-09-30","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 7, 'CREATE', NULL, '{"id":7,"personnelId":4,"projectId":5,"startDate":"2026-03-15","endDate":"2026-07-15","dailyHours":6.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 8, 'CREATE', NULL, '{"id":8,"personnelId":4,"projectId":3,"startDate":"2026-04-15","endDate":"2026-08-31","dailyHours":4.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00'),
('ASSIGNMENT', 9, 'CREATE', NULL, '{"id":9,"personnelId":5,"projectId":4,"startDate":"2026-06-01","endDate":"2026-09-30","dailyHours":8.0,"version":1,"operator":"admin","deleted":0,"createTime":"2026-01-15T10:00:00","updateTime":"2026-01-15T10:00:00"}', 'admin', '2026-01-15 10:00:00');
