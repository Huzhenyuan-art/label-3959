@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

title MyBatis Plus Demo - 项目管理脚本

REM ==========================================
REM MyBatis Plus Demo - Windows 启动脚本
REM ==========================================

REM 颜色定义（使用 ANSI 转义码）
for /F %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "RED=%ESC%[91m"
set "GREEN=%ESC%[92m"
set "YELLOW=%ESC%[93m"
set "BLUE=%ESC%[94m"
set "NC=%ESC%[0m"

REM 配置文件路径
set "ENV_FILE=.env"
set "ENV_EXAMPLE=.env.example"
set "COMPOSE_FILE=docker-compose.yml"
set "COMPOSE=docker compose"

REM 主菜单
:menu
cls
echo %BLUE%==========================================%NC%
echo %BLUE%   MyBatis Plus Demo - 项目管理脚本%NC%
echo %BLUE%==========================================%NC%
echo.
echo %YELLOW%请选择操作:%NC%
echo.
echo   %GREEN%1.%NC%  初始化项目环境
echo   %GREEN%2.%NC%  启动所有服务 (Docker)
echo   %GREEN%3.%NC%  停止所有服务
echo   %GREEN%4.%NC%  重启所有服务
echo   %GREEN%5.%NC%  重新构建并启动
echo   %GREEN%6.%NC%  查看服务状态
echo   %GREEN%7.%NC%  查看服务日志
echo   %GREEN%8.%NC%  运行后端单元测试
echo   %GREEN%9.%NC%  服务健康检查
echo  %GREEN%10.%NC%  显示访问地址
echo  %GREEN%11.%NC%  清理构建产物
echo  %GREEN%12.%NC%  初始化并启动 (首次使用)
echo.
echo  %GREEN%0.%NC%  退出
echo.
set /p "choice=请输入选项 [0-12]: "

if "%choice%"=="1" goto init
if "%choice%"=="2" goto up
if "%choice%"=="3" goto down
if "%choice%"=="4" goto restart
if "%choice%"=="5" goto rebuild
if "%choice%"=="6" goto status
if "%choice%"=="7" goto logs_menu
if "%choice%"=="8" goto backend_test
if "%choice%"=="9" goto health_check
if "%choice%"=="10" goto urls
if "%choice%"=="11" goto clean
if "%choice%"=="12" goto start
if "%choice%"=="0" goto end
goto menu

REM ==========================================
REM 1. 初始化项目环境
REM ==========================================
:init
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           初始化项目环境%NC%
echo %BLUE%==========================================%NC%
echo.

echo %YELLOW%[1/3] 检查 Docker 环境...%NC%
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 Docker 或 Docker 未启动%NC%
    echo 请先安装并启动 Docker Desktop
    pause
    goto menu
)
for /f "tokens=3" %%i in ('docker --version') do echo %GREEN%  ✓ Docker: %%i%NC%

docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 Docker Compose%NC%
    pause
    goto menu
)
for /f "tokens=4" %%i in ('docker compose version') do echo %GREEN%  ✓ Docker Compose: %%i%NC%

echo.
echo %YELLOW%[2/3] 检查 .env 文件...%NC%
if not exist "%ENV_FILE%" (
    echo %YELLOW%  .env 文件不存在，从 .env.example 复制...%NC%
    copy "%ENV_EXAMPLE%" "%ENV_FILE%" >nul
    echo %GREEN%  ✓ 已创建 .env 文件，请根据需要修改敏感配置%NC%
) else (
    echo %GREEN%  ✓ .env 文件已存在%NC%
)

echo.
echo %YELLOW%[3/3] 创建 Docker 缓存目录...%NC%
if not exist ".docker-cache" mkdir ".docker-cache"
echo %GREEN%  ✓ .docker-cache 目录已创建%NC%

echo.
echo %GREEN%==========================================%NC%
echo %GREEN%           初始化完成%NC%
echo %GREEN%==========================================%NC%
echo.
echo %YELLOW%下一步操作:%NC%
echo   1. 编辑 .env 文件，修改敏感配置
echo   2. 返回菜单选择 "启动所有服务"
echo.
pause
goto menu

REM ==========================================
REM 检查 .env 文件
REM ==========================================
:check_env
if not exist "%ENV_FILE%" (
    echo %RED%错误: .env 文件不存在%NC%
    echo %YELLOW%请先选择 "初始化项目环境"%NC%
    echo.
    pause
    goto menu
)
goto :eof

REM ==========================================
REM 2. 启动所有服务
REM ==========================================
:up
call :check_env
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           启动所有服务%NC%
echo %BLUE%==========================================%NC%
echo.

%COMPOSE% -f "%COMPOSE_FILE%" up -d --build
if %errorlevel% neq 0 (
    echo %RED%服务启动失败%NC%
    pause
    goto menu
)

echo.
echo %GREEN%==========================================%NC%
echo %GREEN%           服务启动中%NC%
echo %GREEN%==========================================%NC%
echo.
echo %YELLOW%请等待健康检查通过，可选择 "查看服务状态" 查看进度%NC%
echo.
pause
goto menu

REM ==========================================
REM 3. 停止所有服务
REM ==========================================
:down
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           停止所有服务%NC%
echo %BLUE%==========================================%NC%
echo.

%COMPOSE% -f "%COMPOSE_FILE%" down
echo.
echo %GREEN%服务已停止%NC%
echo.
pause
goto menu

REM ==========================================
REM 4. 重启所有服务
REM ==========================================
:restart
call :check_env
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           重启所有服务%NC%
echo %BLUE%==========================================%NC%
echo.

%COMPOSE% -f "%COMPOSE_FILE%" restart
echo.
echo %GREEN%服务已重启%NC%
echo.
pause
goto menu

REM ==========================================
REM 5. 重新构建并启动
REM ==========================================
:rebuild
call :check_env
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           重新构建并启动%NC%
echo %BLUE%==========================================%NC%
echo.

%COMPOSE% -f "%COMPOSE_FILE%" up -d --build --force-recreate
echo.
echo %GREEN%重新构建完成并启动%NC%
echo.
pause
goto menu

REM ==========================================
REM 6. 查看服务状态
REM ==========================================
:status
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           服务状态%NC%
echo %BLUE%==========================================%NC%
echo.

%COMPOSE% -f "%COMPOSE_FILE%" ps -a
echo.
echo %YELLOW%提示: 等待所有服务的 HEALTH 状态变为 healthy%NC%
echo.
pause
goto menu

REM ==========================================
REM 7. 日志菜单
REM ==========================================
:logs_menu
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           查看日志%NC%
echo %BLUE%==========================================%NC%
echo.
echo   %GREEN%1.%NC%  所有服务日志
echo   %GREEN%2.%NC%  后端日志
echo   %GREEN%3.%NC%  前端日志
echo   %GREEN%4.%NC%  数据库日志
echo   %GREEN%0.%NC%  返回主菜单
echo.
set /p "log_choice=请选择 [0-4]: "

if "%log_choice%"=="1" %COMPOSE% -f "%COMPOSE_FILE%" logs -f
if "%log_choice%"=="2" %COMPOSE% -f "%COMPOSE_FILE%" logs -f backend
if "%log_choice%"=="3" %COMPOSE% -f "%COMPOSE_FILE%" logs -f frontend
if "%log_choice%"=="4" %COMPOSE% -f "%COMPOSE_FILE%" logs -f db
if "%log_choice%"=="0" goto menu

echo.
echo 按 Ctrl+C 退出日志查看
pause
goto logs_menu

REM ==========================================
REM 8. 运行后端单元测试
REM ==========================================
:backend_test
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           运行后端单元测试%NC%
echo %BLUE%==========================================%NC%
echo.

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 Maven 或未配置环境变量%NC%
    echo 请先安装 Maven 并配置环境变量
    pause
    goto menu
)

cd backend
call mvn test -Ptest -Dspring.profiles.active=test
cd ..

echo.
if %errorlevel% equ 0 (
    echo %GREEN%测试通过%NC%
) else (
    echo %RED%测试失败%NC%
)
echo.
pause
goto menu

REM ==========================================
REM 9. 健康检查
REM ==========================================
:health_check
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           服务健康检查%NC%
echo %BLUE%==========================================%NC%
echo.

where curl >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%错误: 未安装 curl 工具%NC%
    echo Windows 10/11 已内置 curl，如缺失请安装 Git Bash 或使用 WSL
    pause
    goto menu
)

echo %YELLOW%检查后端 API...%NC%
curl -fsS http://localhost:8959/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%  ✓ 后端 API 正常%NC%
) else (
    echo %RED%  ✗ 后端 API 不可访问%NC%
)

echo %YELLOW%检查前端页面...%NC%
curl -fsS http://localhost:3959/ >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%  ✓ 前端页面正常%NC%
) else (
    echo %RED%  ✗ 前端页面不可访问%NC%
)

echo %YELLOW%检查 Actuator 端点...%NC%
for %%e in (health info) do (
    curl -fsS http://localhost:8959/actuator/%%e >nul 2>&1
    if !errorlevel! equ 0 (
        echo %GREEN%  ✓ /actuator/%%e%NC%
    ) else (
        echo %RED%  ✗ /actuator/%%e%NC%
    )
)

echo.
echo %GREEN%健康检查完成%NC%
echo.
pause
goto menu

REM ==========================================
REM 10. 显示访问地址
REM ==========================================
:urls
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           服务访问地址%NC%
echo %BLUE%==========================================%NC%
echo.
echo %GREEN%前端:%NC%        http://localhost:3959
echo %GREEN%后端 API:%NC%    http://localhost:8959/api
echo %GREEN%Actuator:%NC%    http://localhost:8959/actuator
echo %GREEN%数据库:%NC%      localhost:33959
echo.
echo %YELLOW%监控端点:%NC%
echo   - 健康检查: http://localhost:8959/actuator/health
echo   - 应用信息: http://localhost:8959/actuator/info
echo   - 指标:     http://localhost:8959/actuator/metrics
echo   - Prometheus: http://localhost:8959/actuator/prometheus
echo.
pause
goto menu

REM ==========================================
REM 11. 清理构建产物
REM ==========================================
:clean
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           清理构建产物%NC%
echo %BLUE%==========================================%NC%
echo.

echo %YELLOW%清理后端构建产物...%NC%
if exist "backend\target" (
    rmdir /s /q "backend\target"
    echo %GREEN%  ✓ 已清理 backend/target%NC%
)

echo %YELLOW%清理前端构建产物...%NC%
if exist "frontend\dist" (
    rmdir /s /q "frontend\dist"
    echo %GREEN%  ✓ 已清理 frontend/dist%NC%
)

echo.
echo %GREEN%清理完成%NC%
echo.
pause
goto menu

REM ==========================================
REM 12. 初始化并启动 (首次使用)
REM ==========================================
:start
cls
echo %BLUE%==========================================%NC%
echo %BLUE%           初始化并启动服务%NC%
echo %BLUE%==========================================%NC%
echo.

call :init

echo %BLUE%正在启动服务...%NC%
echo.
call :up

goto menu

REM ==========================================
REM 退出
REM ==========================================
:end
cls
echo %GREEN%感谢使用，再见！%NC%
echo.
timeout /t 2 >nul
exit /b 0
