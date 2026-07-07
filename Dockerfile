# syntax=docker/dockerfile:1
# ============================
# 第一阶段：使用 Maven 构建项目
# ============================
FROM maven:3.9.6-eclipse-temurin-8 AS builder

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

# 复制 pom.xml 文件
COPY pom.xml .

# 预下载依赖，利用 BuildKit 缓存
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -B -s /root/.m2/settings.xml

# 复制源代码
COPY src ./src

# 构建项目
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests -B -s /root/.m2/settings.xml

# ============================
# 第二阶段：运行阶段
# ============================
FROM eclipse-temurin:8-jre

# 设置工作目录
WORKDIR /app/cms

# 从 builder 阶段复制构建好的 jar 包
COPY --from=builder /build/target/cms.jar app.jar

# 开放端口
EXPOSE 8080

# 设置环境变量为生产环境
ENV SPRING_PROFILES_ACTIVE=prd

# 启动应用
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/cms/app.jar"]
