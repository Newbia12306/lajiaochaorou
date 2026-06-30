#!/bin/bash
# ================================
# Quality Gate 检查脚本
# 用法: ./quality-gate-check.sh [sonarqube-token]
# 或者设置环境变量: export SONAR_TOKEN=your-token
# ================================

set -e

# ============ 配置 ============
SONAR_HOST="http://localhost:9000"
PROJECT_KEY="menu-app-backend"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SONAR_TOKEN="${SONAR_TOKEN:-${1}}"
# 使用 Java 21 (JaCoCo 0.8.12 兼容)
export JAVA_HOME="C:/Program Files/Java/jdk-21"
export PATH="${JAVA_HOME}/bin:${PATH}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "  SonarQube Quality Gate 检查"
echo "========================================"
echo ""
echo "项目: ${PROJECT_KEY}"
echo "目录: ${PROJECT_DIR}"
echo "SonarQube: ${SONAR_HOST}"
echo ""

# ============ 检查 Token ============
if [ -z "$SONAR_TOKEN" ]; then
    echo -e "${RED}[错误] 未提供 SonarQube Token！${NC}"
    echo ""
    echo "请通过以下方式之一提供 Token:"
    echo "  1. 命令行参数: ./quality-gate-check.sh <your-token>"
    echo "  2. 环境变量:   export SONAR_TOKEN=<your-token>"
    echo ""
    echo "获取 Token 的步骤:"
    echo "  1. 打开 ${SONAR_HOST}"
    echo "  2. 登录后点击右上角头像 → My Account → Security"
    echo "  3. 在 'Generate Tokens' 输入令牌名称（如: 'local-check'）"
    echo "  4. 点击 Generate 并复制令牌"
    echo ""
    exit 1
fi

# ============ 步骤1: 运行测试和覆盖率 ============
echo -e "${YELLOW}[Step 1/4] 运行 Maven 测试 + JaCoCo 覆盖率...${NC}"
echo ""

cd "${PROJECT_DIR}"

if mvn clean test -Dmaven.test.failure.ignore=false; then
    echo ""
    echo -e "${GREEN}✓ 测试全部通过${NC}"
else
    echo ""
    echo -e "${RED}✗ 测试失败！请修复失败的测试后再试${NC}"
    exit 1
fi

# 检查 JaCoCo 报告是否生成
JACOCO_REPORT="${PROJECT_DIR}/target/site/jacoco/jacoco.xml"
if [ ! -f "$JACOCO_REPORT" ]; then
    echo -e "${RED}[错误] JaCoCo 覆盖率报告未生成: ${JACOCO_REPORT}${NC}"
    exit 1
fi
echo "  JaCoCo 报告已生成: target/site/jacoco/jacoco.xml"

# ============ 步骤2: 运行 SonarQube 扫描 ============
echo ""
echo -e "${YELLOW}[Step 2/4] 运行 SonarQube 扫描...${NC}"
echo ""

# 检查 sonar-scanner 是否可用
SCANNER_HOME="E:/sonar-scanner-8.0.1.6346-windows-x64"
export PATH="${SCANNER_HOME}/bin:${PATH}"

if ! command -v sonar-scanner &> /dev/null; then
    echo -e "${RED}[错误] sonar-scanner 未找到: ${SCANNER_HOME}${NC}"
    exit 1
fi

sonar-scanner \
    -Dsonar.projectKey="${PROJECT_KEY}" \
    -Dsonar.sources=src/main/java \
    -Dsonar.tests=src/test/java \
    -Dsonar.java.binaries=target/classes \
    -Dsonar.java.test.binaries=target/test-classes \
    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
    -Dsonar.host.url="${SONAR_HOST}" \
    -Dsonar.token="${SONAR_TOKEN}"

echo ""
echo -e "${GREEN}✓ 扫描完成${NC}"

# ============ 步骤3: 等待 SonarQube 处理 ============
echo ""
echo -e "${YELLOW}[Step 3/4] 等待 SonarQube 后台处理完成...${NC}"

# 等待后台任务完成 (CE: Compute Engine)
MAX_WAIT=60
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    # 检查 CE 任务队列
    TASK_STATUS=$(curl -s -u "${SONAR_TOKEN}:" "http://localhost:9000/api/ce/component?component=${PROJECT_KEY}" 2>/dev/null | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$TASK_STATUS" = "SUCCESS" ] || [ -z "$TASK_STATUS" ]; then
        break
    fi

    sleep 3
    ELAPSED=$((ELAPSED + 3))
    echo "  等待中... (${ELAPSED}s)"
done

# ============ 步骤4: 检查 Quality Gate 状态 ============
echo ""
echo -e "${YELLOW}[Step 4/4] 检查 Quality Gate 状态...${NC}"

# 调用 Quality Gate API
QG_RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" "http://localhost:9000/api/qualitygates/project_status?projectKey=${PROJECT_KEY}")

echo "  API 响应: ${QG_RESPONSE}"

# 解析 Quality Gate 状态
QG_STATUS=$(echo "$QG_RESPONSE" | grep -o '"projectStatus":{"status":"[^"]*"' | cut -d'"' -f6)

if [ -z "$QG_STATUS" ]; then
    # 备用解析方式
    QG_STATUS=$(echo "$QG_RESPONSE" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
fi

echo ""
echo "========================================"
if [ "$QG_STATUS" = "OK" ]; then
    echo -e "${GREEN}  ✓ Quality Gate 通过！${NC}"
    echo -e "${GREEN}  代码覆盖率 >= 80%，可以合并到 main 分支${NC}"
    echo "========================================"
    exit 0
elif [ "$QG_STATUS" = "ERROR" ]; then
    echo -e "${RED}  ✗ Quality Gate 失败！${NC}"
    echo -e "${RED}  代码覆盖率低于 80%，禁止合并到 main 分支${NC}"
    echo ""
    echo "  请执行以下操作:"
    echo "  1. 补充单元测试提高覆盖率至 80% 以上"
    echo "  2. 重新运行此脚本检查"
    echo "  3. 查看详细报告: ${SONAR_HOST}/dashboard?id=${PROJECT_KEY}"
    echo "========================================"
    exit 1
else
    echo -e "${YELLOW}  Quality Gate 状态未知: ${QG_STATUS}${NC}"
    echo "  请手动检查: ${SONAR_HOST}/dashboard?id=${PROJECT_KEY}"
    echo "========================================"
    exit 1
fi
