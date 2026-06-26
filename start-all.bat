@echo off
echo ========================================
echo   Starting all services...
echo ========================================

echo.
echo [1/2] Building backend JAR...
call .\mvnw.cmd package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Backend build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Starting Docker containers...
docker-compose up -d --build

echo.
echo ========================================
echo   All services started!
echo   Frontend: http://localhost
echo   Backend:  http://localhost:8081
echo   PostgreSQL: localhost:5432
echo   Redis:      localhost:6379
echo   Elasticsearch: http://localhost:9200
echo ========================================
echo.
echo   To stop: docker-compose down
echo   To view logs: docker-compose logs -f
echo.
pause
