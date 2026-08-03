@echo off
chcp 936 >nul
setlocal

REM ============================================================
REM  人员-项目时间匹配管理系统 - 一键启动脚本
REM  用法：
REM    1. 双击运行本脚本（推荐）
REM    2. 命令行: start.bat [--check]  （--check 仅检查环境不启动服务）
REM  功能：自动检查 MySQL、初始化数据库（如需）、启动前后端、打开浏览器
REM ============================================================

set "ROOT=%~dp0"
set "DB_NAME=pm_tool"
set "MYSQL_USER=root"
set "MYSQL_PASS=123456"

REM ---- 环境路径（按本机实际安装位置自动探测） ----
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-17*" "C:\Program Files\Java\jdk-17*") do (
        if exist "%%d\bin\java.exe" set "JAVA_HOME=%%d"
    )
)
set "MAVEN_HOME=%USERPROFILE%\.trae-cn\work\6a6ff461ed7c0e26541f8b83\maven\apache-maven-3.9.16"
set "MYSQL_BIN="
for /d %%d in ("C:\Program Files\MySQL\MySQL Server 8*") do (
    if exist "%%d\bin\mysql.exe" set "MYSQL_BIN=%%d\bin"
)
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%MYSQL_BIN%;%PATH%"

echo.
echo  ============================================
echo   人员-项目时间匹配管理系统 - 一键启动
echo  ============================================
echo.

REM ========== 1. 环境检查 ==========
echo  [1/5] 检查环境依赖...

java -version >nul 2>&1
if errorlevel 1 (
    echo   [错误] 未找到 JDK 17，请先安装
    set "FAILED=1"
) else (
    echo   [OK] Java: %JAVA_HOME%
)

call "%MAVEN_HOME%\bin\mvn.cmd" -v >nul 2>&1
if errorlevel 1 (
    echo   [错误] 未找到 Maven
    set "FAILED=1"
) else (
    echo   [OK] Maven 3.9.16
)

node -v >nul 2>&1
if errorlevel 1 (
    echo   [错误] 未找到 Node.js
    set "FAILED=1"
) else (
    echo   [OK] Node.js 已安装
)

if not "%FAILED%"=="" (
    echo.
    echo  [错误] 环境依赖缺失，请安装后再试。
    pause
    exit /b 1
)

REM ========== 2. 检查 MySQL ==========
echo  [2/5] 检查 MySQL 服务...
if "%MYSQL_BIN%"=="" (
    echo   [错误] 未找到 mysql 客户端，请确认 MySQL 已安装。
    pause
    exit /b 1
)
"%MYSQL_BIN%\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo   [错误] 无法连接 MySQL（请检查服务是否启动、application.yml 中账号密码是否正确）。
    pause
    exit /b 1
)
echo   [OK] MySQL 已运行

REM ========== 3. 检查数据库并自动初始化 ==========
echo  [3/5] 检查数据库 %DB_NAME% ...
"%MYSQL_BIN%\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='%DB_NAME%';" >nul 2>&1
if errorlevel 1 (
    echo   [提示] 数据库 %DB_NAME% 不存在，自动初始化...
    "%MYSQL_BIN%\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    if errorlevel 1 (
        echo   [警告] 自动建库失败，请手动执行 backend\src\main\resources\sql\init.sql 后重试。
        pause
        exit /b 1
    )
    "%MYSQL_BIN%\mysql.exe" -u%MYSQL_USER% -p%MYSQL_PASS% --default-character-set=utf8mb4 %DB_NAME% < "%ROOT%backend\src\main\resources\sql\init.sql"
    if errorlevel 1 (
        echo   [警告] 导入 init.sql 失败，请手动检查。
        pause
        exit /b 1
    )
    echo   [OK] 数据库初始化完成
) else (
    echo   [OK] 数据库已存在
)

if "%1"=="--check" (
    echo.
    echo  ============================================
    echo   环境检查完毕，全部就绪，可以启动服务。
    echo  ============================================
    echo.
    pause
    exit /b 0
)

REM ========== 4. 启动后端 ==========
echo  [4/5] 启动后端服务 (http://localhost:8080/api) ...
pushd "%ROOT%backend"
start "PMTool-Backend" cmd /k "mvn spring-boot:run"
popd

REM ========== 5. 启动前端 ==========
echo  [5/5] 启动前端服务 (http://localhost:5173) ...
pushd "%ROOT%frontend"
start "PMTool-Frontend" cmd /k "npm run dev"
popd

REM ========== 打开浏览器 ==========
echo.
echo  正在启动浏览器，请稍候（后端首次启动需编译，约 30-60 秒）...
timeout /t 8 /nobreak >nul
start http://localhost:5173

echo.
echo  ============================================
echo   启动完成！
echo   前端地址: http://localhost:5173
echo   后端地址: http://localhost:8080/api
echo   测试账号: admin / 123456
echo   (关闭两个服务窗口即可停止系统)
echo  ============================================
echo.
pause
