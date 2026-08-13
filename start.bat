@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM  人员-项目时间匹配管理系统 - 启动脚本
REM  职责：检查环境 -> 确保数据库存在 -> 启动前后端 -> 开浏览器
REM  数据库建表/升级：由后端启动时自动执行（Flyway）
REM  用法：双击运行；命令行执行 start.bat --check 仅检查环境
REM ============================================================

chcp 65001 >nul

set "ROOT=%~dp0"
set "DB_NAME=pm_tool"

REM ---- 数据库账号：优先读环境变量，未设置时用默认值 ----
set "DB_USER=%PM_DB_USER%"
if "%DB_USER%"=="" set "DB_USER=root"
set "DB_PASS=%PM_DB_PASS%"
if "%DB_PASS%"=="" set "DB_PASS=123456"

REM ---- 应用版本：从 pom.xml 读取（单一来源，勿在此处手写）----
set "APP_VERSION=unknown"
for /f "usebackq delims=" %%v in (`powershell -NoProfile -Command "[xml]$x = Get-Content -Raw -Encoding UTF8 '%ROOT%backend\pom.xml'; Write-Output $x.DocumentElement.version"`) do set "APP_VERSION=%%v"
if "%APP_VERSION%"=="" set "APP_VERSION=unknown"

REM ========== 1. 环境检查 ==========
echo.
echo  ============================================
echo   人员-项目时间匹配管理系统 v%APP_VERSION%
echo  ============================================
echo.

echo  [1/3] 检查 Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo   [错误] 未找到 JDK 17（若已安装，请将 Java 加入 PATH）
    set "FAILED=1"
) else (
    echo   [OK] Java 可用
)

echo  [2/3] 检查 Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo   [错误] 未找到 Node.js，请先安装
    set "FAILED=1"
) else (
    echo   [OK] Node.js 可用
)

if not "%FAILED%"=="" (
    echo.
    echo  [错误] 环境依赖缺失，请安装后再试。
    pause
    exit /b 1
)

REM ========== 2. MySQL 检查 ==========
set "MYSQL_BIN="
for /d %%d in ("C:\Program Files\MySQL\MySQL Server 8*") do (
    if exist "%%d\bin\mysql.exe" set "MYSQL_BIN=%%d\bin"
)

echo  [3/3] 检查 MySQL 服务...
if "%MYSQL_BIN%"=="" (
    echo   [错误] 未找到 MySQL 客户端，请确认 MySQL 已安装
    pause
    exit /b 1
)
"%MYSQL_BIN%\mysql.exe" -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo   [错误] 无法连接 MySQL（请检查服务是否启动、账号密码是否正确）
    pause
    exit /b 1
)
echo   [OK] MySQL 已运行

if "%1"=="--check" (
    echo.
    echo  ============================================
    echo   环境检查完毕，全部就绪（应用版本 %APP_VERSION%）
    echo  ============================================
    pause
    exit /b 0
)

REM ========== 3. 确保数据库存在（建表/升级由后端 Flyway 完成）==========
echo  检查数据库 %DB_NAME% ...
"%MYSQL_BIN%\mysql.exe" -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" >nul 2>&1
if errorlevel 1 (
    echo   [错误] 自动建库失败，请检查 MySQL 账号权限
    pause
    exit /b 1
)
echo   [OK] 数据库就绪，建表与升级由后端自动完成

REM ========== 4. 启动前后端 ==========
echo  启动后端服务 (http://localhost:8080/api) ...
pushd "%ROOT%backend"
if exist "mvnw.cmd" (
    start "PMTool-Backend" cmd /k "mvnw.cmd spring-boot:run"
) else (
    start "PMTool-Backend" cmd /k "mvn spring-boot:run"
)
popd

echo  启动前端服务 (http://localhost:5173) ...
pushd "%ROOT%frontend"
start "PMTool-Frontend" cmd /k "npm run dev"
popd

REM ========== 5. 打开浏览器 + 摘要 ==========
echo.
echo  正在启动浏览器，请稍候（后端首次启动需编译，约 30-60 秒）...
timeout /t 8 /nobreak >nul
start http://localhost:5173

echo.
echo  ============================================
echo   启动完成！
echo   前端地址: http://localhost:5173
echo   后端地址: http://localhost:8080/api
echo   应用版本: %APP_VERSION%
echo   测试账号: admin / 123456
echo   (关闭两个服务窗口即可停止系统)
echo  ============================================
echo.

REM ---- 展示最近一次更新日志（完整内容见 CHANGELOG.md）----
if exist "%ROOT%CHANGELOG.md" (
    echo  最近更新：
    set "PRINTING="
    set "DONE="
    set "COUNT=0"
    for /f "usebackq delims=" %%L in ("%ROOT%CHANGELOG.md") do (
        set "LINE=%%L"
        if "!LINE:~0,3!"=="## " (
            if defined PRINTING set "DONE=1"
            set "PRINTING=1"
        )
        if defined PRINTING if not defined DONE if not "!LINE!"=="" (
            echo    !LINE!
            set /a COUNT+=1
            if !COUNT! geq 10 set "DONE=1"
        )
    )
    echo.
)

pause
