@echo off
REM ================================
REM   SonarQube Code Scan
REM ================================
echo ================================
echo   SonarQube Code Scan
echo ================================
echo.

set SCANNER_HOME=E:\sonar-scanner-8.0.1.6346-windows-x64
set PATH=%SCANNER_HOME%\bin;%PATH%
REM 使用 Java 21 (JaCoCo 0.8.12 兼容)
set JAVA_HOME=C:\Program Files\Java\jdk-21

REM 如果设置了 SONAR_TOKEN 环境变量则使用，否则需要手动输入
if "%SONAR_TOKEN%"=="" (
    echo [提示] 未设置 SONAR_TOKEN 环境变量
    echo   可以运行: set SONAR_TOKEN=^<your-token^>
    echo.
)

REM 切换到 backend 目录
cd /d "%~dp0backend"

echo Project: %CD%
echo Server:  http://localhost:9000
echo.

REM 先运行测试生成覆盖率报告
echo Running tests with coverage...
call mvn clean test -q
if %errorlevel% neq 0 (
    echo [警告] 测试执行失败，继续扫描...
)

echo.
echo Running SonarQube scan...

if not "%SONAR_TOKEN%"=="" (
    REM 使用 Token 认证
    sonar-scanner ^
      -Dsonar.projectKey=menu-app-backend ^
      -Dsonar.projectName="Menu App - Backend" ^
      -Dsonar.sources=src/main/java ^
      -Dsonar.tests=src/test/java ^
      -Dsonar.java.binaries=target/classes ^
      -Dsonar.java.test.binaries=target/test-classes ^
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml ^
      -Dsonar.host.url=http://localhost:9000 ^
      -Dsonar.token=%SONAR_TOKEN%
) else (
    REM 无 Token，需要在 SonarQube 中手动查看结果
    sonar-scanner ^
      -Dsonar.projectKey=menu-app-backend ^
      -Dsonar.projectName="Menu App - Backend" ^
      -Dsonar.sources=src/main/java ^
      -Dsonar.tests=src/test/java ^
      -Dsonar.java.binaries=target/classes ^
      -Dsonar.java.test.binaries=target/test-classes ^
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml ^
      -Dsonar.host.url=http://localhost:9000
)

echo.
echo ================================
echo Scan complete!
echo View results: http://localhost:9000/dashboard?id=menu-app-backend
echo ================================
pause
