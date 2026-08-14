# syntax=docker/dockerfile:1
# ============================
# 第一阶段：使用 Maven 构建项目
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# 配置阿里云 Maven 镜像加速
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml << 'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <mirrorOf>*</mirrorOf>
      <name>Aliyun Public Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

WORKDIR /build

# 复制所有 POM 文件（多模块聚合：先只复制 POM 以便预下载依赖命中 BuildKit 缓存）
COPY pom.xml .
COPY cms-kernel/pom.xml cms-kernel/pom.xml
COPY cms-file/pom.xml cms-file/pom.xml
COPY cms-payment/pom.xml cms-payment/pom.xml
COPY cms-gamesave/pom.xml cms-gamesave/pom.xml
COPY cms-platform/pom.xml cms-platform/pom.xml
COPY cms-app/pom.xml cms-app/pom.xml

# 预下载依赖，利用 BuildKit 缓存
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -s /root/.m2/settings.xml

# 复制各模块源代码
COPY cms-kernel/src ./cms-kernel/src
COPY cms-file/src ./cms-file/src
COPY cms-payment/src ./cms-payment/src
COPY cms-gamesave/src ./cms-gamesave/src
COPY cms-platform/src ./cms-platform/src
COPY cms-app/src ./cms-app/src

# 构建项目
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests -B -s /root/.m2/settings.xml

# ============================
# 第二阶段：运行阶段
# ============================
FROM eclipse-temurin:21-jre

# 设置工作目录
WORKDIR /app/cms

# 从 builder 阶段复制构建好的 jar 包（可执行包只由 cms-app 模块产出）
COPY --from=builder /build/cms-app/target/cms.jar app.jar

# 开放端口
EXPOSE 8080

# 设置环境变量为生产环境
ENV SPRING_PROFILES_ACTIVE=prd

# 启动应用
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/cms/app.jar"]
