@echo off
REM ================================
REM Quality Gate 检查脚本 (Windows)
REM 用法: quality-gate-check.bat <sonarqube-token>
REM 或者设置环境变量: set SONAR_TOKEN=your-token
REM ================================

setlocal enabledelayedexpansion

REM ============ 配置 ============
set SONAR_HOST=http://localhost:9000
set PROJECT_KEY=menu-app-backend
set PROJECT_DIR=%~dp0backend
set SCANNER_HOME=E:\sonar-scanner-8.0.1.6346-windows-x64
REM 使用 Java 21 (JaCoCo 0.8.12 兼容)
set JAVA_HOME=C:\Program Files\Java\jdk-21

REM 优先使用环境变量，其次使用命令行参数
if not "%SONAR_TOKEN%"=="" goto :token_ok
if not "%~1"=="" set SONAR_TOKEN=%~1
if "%SONAR_TOKEN%"=="" goto :no_token
:token_ok

echo ========================================
echo   SonarQube Quality Gate 检查
echo ========================================
echo.
echo 项目: %PROJECT_KEY%
echo 目录: %PROJECT_DIR%
echo SonarQube: %SONAR_HOST%
echo.

REM ============ Step 1: 运行测试和覆盖率 ============
echo [Step 1/4] 运行 Maven 测试 + JaCoCo 覆盖率...
echo.

cd /d "%PROJECT_DIR%"

call mvn clean test -Dmaven.test.failure.ignore=false
if %errorlevel% neq 0 (
    echo.
    echo [错误] 测试失败！请修复失败的测试后再试
    exit /b 1
)
echo.
echo [OK] 测试全部通过

REM 检查 JaCoCo 报告
if not exist "%PROJECT_DIR%\target\site\jacoco\jacoco.xml" (
    echo [错误] JaCoCo 覆盖率报告未生成
    exit /b 1
)
echo   JaCoCo 报告已生成

REM ============ Step 2: SonarQube 扫描 ============
echo.
echo [Step 2/4] 运行 SonarQube 扫描...
echo.

set PATH=%SCANNER_HOME%\bin;%PATH%

sonar-scanner ^
    -Dsonar.projectKey=%PROJECT_KEY% ^
    -Dsonar.sources=src/main/java ^
    -Dsonar.tests=src/test/java ^
    -Dsonar.java.binaries=target/classes ^
    -Dsonar.java.test.binaries=target/test-classes ^
    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml ^
    -Dsonar.host.url=%SONAR_HOST% ^
    -Dsonar.token=%SONAR_TOKEN%

if %errorlevel% neq 0 (
    echo.
    echo [错误] SonarQube 扫描失败
    exit /b 1
)
echo.
echo [OK] 扫描完成

REM ============ Step 3: 等待处理 ============
echo.
echo [Step 3/4] 等待 SonarQube 后台处理完成...
timeout /t 10 /nobreak >nul

REM ============ Step 4: 检查 Quality Gate ============
echo.
echo [Step 4/4] 检查 Quality Gate 状态...

REM 使用 PowerShell 解析 JSON
for /f "delims=" %%i in ('powershell -NoProfile -Command ^
    "$r=Invoke-RestMethod -Uri 'http://localhost:9000/api/qualitygates/project_status?projectKey=%PROJECT_KEY%' -Headers @{Authorization='Bearer %SONAR_TOKEN%'}; echo $r.projectStatus.status" 2^>nul') do set QG_STATUS=%%i

echo.
echo ========================================
if /i "%QG_STATUS%"=="OK" (
    echo   [PASS] Quality Gate 通过！
    echo   代码覆盖率 >= 80%%，可以合并到 main 分支
    echo ========================================
    exit /b 0
) else if /i "%QG_STATUS%"=="ERROR" (
    echo   [FAIL] Quality Gate 失败！
    echo   代码覆盖率低于 80%%，禁止合并到 main 分支
    echo.
    echo   请执行以下操作:
    echo   1. 补充单元测试提高覆盖率至 80%% 以上
    echo   2. 重新运行此脚本检查
    echo   3. 查看详细报告: %SONAR_HOST%/dashboard?id=%PROJECT_KEY%
    echo ========================================
    exit /b 1
) else (
    echo   Quality Gate 状态: %QG_STATUS%
    echo   请手动检查: %SONAR_HOST%/dashboard?id=%PROJECT_KEY%
    echo ========================================
    exit /b 1
)

:no_token
echo ========================================
echo   [错误] 未提供 SonarQube Token！
echo ========================================
echo.
echo 请通过以下方式之一提供 Token:
echo   1. 命令行参数: quality-gate-check.bat ^<your-token^>
echo   2. 环境变量:   set SONAR_TOKEN=^<your-token^>
echo.
echo 获取 Token 的步骤:
echo   1. 打开 %SONAR_HOST%
echo   2. 登录后点击右上角头像 -^> My Account -^> Security
echo   3. 在 'Generate Tokens' 输入令牌名称（如: local-check）
echo   4. 点击 Generate 并复制令牌
echo.
exit /b 1
