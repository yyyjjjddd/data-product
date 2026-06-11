FROM openjdk:21-jdk-alpine

LABEL maintainer="dev@example.com"
LABEL description="指标配置后台服务"

# 时区通过环境变量控制，默认上海时区
# 运行时可通过 -e TZ=Asia/Shanghai 或挂载宿主机的时区文件覆盖
ENV TZ=${TZ:-Asia/Shanghai}

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone

# 创建应用目录
RUN mkdir -p /app

# 复制 jar 包
ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

# 复制配置文件
COPY config.yaml /app/config.yaml

# 设置工作目录
WORKDIR /app

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动命令 - 使用外部配置文件
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=file:/app/config.yaml"]