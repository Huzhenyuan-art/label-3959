# syntax=docker/dockerfile:1.6

# ============================
# 阶段 1: Maven 依赖解析缓存
# ============================
FROM maven:3.9.8-eclipse-temurin-17 AS maven-cache
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -q

# ============================
# 阶段 2: 应用构建
# ============================
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY --from=maven-cache /root/.m2/repository /root/.m2/repository
COPY --from=maven-cache /app/pom.xml .
COPY src ./src

ARG PROFILE=prod
ARG SKIP_TESTS=true

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests=${SKIP_TESTS} -P${PROFILE} -B -q \
    && mv target/*.jar target/app.jar

# ============================
# 阶段 3: 运行时镜像
# ============================
FROM eclipse-temurin:17.0.12_7-jre-alpine AS runtime

WORKDIR /app

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=100" \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs

RUN apk add --no-cache \
    curl \
    tzdata \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p ${LOG_PATH} \
    && chown -R spring:spring /app

USER spring:spring

COPY --from=builder --chown=spring:spring /app/target/app.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

EXPOSE 8080 9090

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

