#!/bin/bash

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== 指标配置服务镜像构建 ===${NC}"

# 参数解析
REGISTRY=${1:-"registry.example.com"}
VERSION=${2:-"1.0.0"}

echo -e "Registry: ${YELLOW}${REGISTRY}${NC}"
echo -e "Version: ${YELLOW}${VERSION}${NC}"

# 清理并构建
echo -e "\n${GREEN}[1/3] 清理之前的构建...${NC}"
cd ..
mvn clean package -DskipTests -q

# 构建镜像
echo -e "\n${GREEN}[2/3] 构建 Docker 镜像...${NC}"
cd deploy
mvn dockerfile:build \
    -Ddockerfile.repository=${REGISTRY}/metrics-config-service \
    -Ddockerfile.tag=${VERSION} \
    -Ddockerfile.maven.fallback=true

# 推送镜像
echo -e "\n${GREEN}[3/3] 推送镜像...${NC}"
docker push ${REGISTRY}/metrics-config-service:${VERSION}
docker push ${REGISTRY}/metrics-config-service:latest

echo -e "\n${GREEN}=== 构建完成 ===${NC}"
echo -e "镜像地址: ${YELLOW}${REGISTRY}/metrics-config-service:${VERSION}${NC}"