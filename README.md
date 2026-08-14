# 内容管理系统

### 内容管理系统，后端采用 Spring Boot + Sa-Token + MyBatis-Plus，前台博客和后台管理均为 Vue 3 + Element Plus 单页应用(附带权限管理)，是搭建博客、网站的不二之选。

## 文档目录

- [项目介绍](#项目介绍)
- [安装](#安装)
- [使用](#使用)
- [代码结构](#代码结构)

## 项目介绍

### CMS管理系统，适合搭建博客、企业网站等，完美自适应。

## 项目预览

项目前台预览：[https://cms.thxdxw.cn](https://cms.thxdxw.cn?_blank) <br/>
项目后台预览：[https://cms.thxdxw.cn/admin](https://cms.thxdxw.cn/admin?_blank) <br/>

后台测试账号（只读权限）账号：guest 密码：123456

## 技术栈

后端：Spring Boot、Sa-Token、MyBatis-Plus、Alibaba Druid、Redis、MySQL、MinIO、Apache Tika<br/>
前端：Vue 3、Element Plus（前台博客 `static/blog-app/`、后台管理 `static/admin-app/` 均为纯静态文件，无需构建工具，改完即生效）<br/>

![JDK](https://img.shields.io/badge/JDK-21-green.svg)
![Maven](https://img.shields.io/badge/Maven-3.3.9-green.svg)
![MySQL](https://img.shields.io/badge/MySQL-5.7-green.svg)
![Redis](https://img.shields.io/badge/Redis-3.0.503-green.svg)
![license](https://img.shields.io/badge/license-MIT-yellow.svg)

## 安装

1. 将本项目源码导入本地开发工具(如 IntelliJ IDEA )，本地开发工具需要安装 [lombok](https://projectlombok.org/) 插件
2. 安装`MySQL`数据库：版本最低支持 5.7，新建 database `CREATE DATABASE cms;`
3. 初始化数据库：按顺序执行四个模块的初始化脚本（顺序不能颠倒，gamesave 种子数据依赖 file 模块的表），
   详见 [`docs/modules/README.md`](docs/modules/README.md)
   ```bash
   mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/platform/cms.sql
   mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/file/schema.sql
   mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/payment/schema.sql
   mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/gamesave/schema.sql
   ```
4. 安装`Redis`：最低版本支持 3.2
5. 安装`MinIO`（文件上传/存储依赖）：本地开发默认指向 `http://localhost:9000`，也可以通过环境变量 `MINIO_ENDPOINT`/`MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` 指向已有实例
6. 修改(`cms-app/src/main/resources/application-dev.yml`)配置文件
    1. 修改数据库连接串、用户名和密码(可搜索`datasource`)
    2. redis 配置(可搜索`redis`)
7. 运行项目(三种方式)
    1. 项目根目录下执行`mvn clean package -DskipTests`编译打包，然后执行`java -jar cms-app/target/cms.jar`
    2. 项目根目录下执行`mvn -pl cms-app spring-boot:run`
    3. 直接运行`cms-app/src/main/java/com/thx/SpringbootApplication.java`
8. 前台首页，浏览器访问`http://localhost:8080`
9. 后台首页，浏览器访问`http://localhost:8080/admin`使用账号密码admin,123456登录系统后台。

> 数据库结构变更请勿再直接改初始化脚本：已有环境走 Flyway 迁移，规范见
> [`cms-app/src/main/resources/db/migration/README.md`](cms-app/src/main/resources/db/migration/README.md)。

## 部署

推送到 `master` 分支后由 Drone CI（`.drone.yml`）自动完成：先拉起 MySQL/Redis 跑完整构建与测试，
通过后再 SSH 到服务器执行 `deploy.sh`。

`deploy.sh` 先构建候选镜像（此时旧版本继续对外服务），切换后轮询 `/actuator/health` 确认新版本真正
就绪；**健康检查失败会自动回滚到上一个镜像**。镜像按提交号打标签并保留最近 4 个版本。

部署前需在服务器上准备好环境变量文件：复制 [`.env.example`](.env.example) 为部署目录上一级的
`config/.env` 并填入真实配置。

## 使用

### 文件上传

文件存储基于 MinIO 对象存储，支持按接入方(App)+场景(namespace)配置不同的访问策略（公开/私有、大小限制、允许的扩展名等），具体策略在 `file_app`/`file_policy` 等表中维护，不再通过配置文件下发。

### 移动端适配

后台管理页面（`static/admin-app/`）已适配移动端：小屏下侧边栏菜单收起为可通过顶部汉堡按钮开合的抽屉导航，列表页操作按钮收进下拉菜单，表单/弹窗自适应窄屏宽度。

### 静态化

网站启用静态化步骤：

1. 在yml配置文件中，配置好静态页面文件生成的文件夹路径
2. 启动项目，进入后台->网站管理->基础信息，切换到开启“静态化”，点击保存

## 代码结构

项目是 **Maven 模块化单体（modular monolith）**：拆成 6 个 Maven 模块，但只部署一个 `cms.jar`、一个容器。
模块依赖方向固定为 `cms-app → cms-platform → cms-file → cms-kernel`、`cms-gamesave → cms-file`、`cms-payment → cms-kernel`，
Maven 在编译期阻止跨模块的错误引用。

```
├── pom.xml                   cms-parent 聚合 POM（只管理版本，不声明依赖）
├── cms-kernel                内核：纯公共类型/异常/工具/基础契约
│   └── src/main/java/com/thx
│       ├── common/annotation  @AnonymousAccess 等
│       ├── common/holder     SpringContextHolder
│       ├── common/util       JsonUtil/UUIDUtil/DateUtil/Pagination/ResultUtil 等
│       ├── common/vo         ResponseVo/PageResultVo/BaseVo/BaseConditionVo（通用响应体）
│       ├── enums             响应状态码、站点配置 key
│       └── exception         ApiException
├── cms-file                  文件系统：MinIO 对象存储 + Tika 类型嗅探 + App/Scope 独立认证 + REST API
│   └── src/main/java/com/thx/module/file
├── cms-payment               支付：支付宝渠道/支付/退款/通知/对账，稳定 api 包（PaymentFacade）
│   └── src/main/java/com/thx/module/payment
├── cms-gamesave              游戏存档：账号/设备/存档对象/快照，直接调用 cms-file 的 Java Service
│   └── src/main/java/com/thx/module/gamesave
├── cms-platform              平台层：admin（内容+权限后台）+ blog（前台 API）+ agent（运维 API 网关）
│   │                         + tools（小工具）+ 平台安全实现（common.security 等）
│   └── src/main/java/com/thx
│       ├── module/admin | blog | agent | tools | platform/observability
│       ├── common/security|interceptor|log|config/properties  平台安全/日志/拦截器
│       ├── infra            邮件发送、WebSocket 推送、匿名路径扫描
│       └── exception        ExceptionHandleController 全局异常处理
└── cms-app                   启动模块：启动类、统一装配（WebMvc/MyBatis-Plus/Redis/WebSocket/ErrorPage）、
    │                         全部配置与静态资源，产出可执行 cms.jar
    ├── src/main/java/com/thx/SpringbootApplication.java
    └── src/main/resources
        ├── application-dev.yml / application-prd.yml / application.yml
        ├── logback-spring.xml
        ├── mapper             MyBatis XML 文件
        ├── db/migration       Flyway 迁移脚本
        ├── static             admin-app / blog-app 两个 Vue 单页应用及公共静态资源
        └── templates          Thymeleaf 服务端渲染页面（error / home / system）
```

常用命令：

```bash
mvn clean package -DskipTests        # 全量打包，产物 cms-app/target/cms.jar
mvn -pl cms-app -am package          # 只构建 app 及其依赖
mvn -pl cms-payment -am test         # 只验证 payment 模块（及其依赖模块）的单元测试
mvn test -pl cms-kernel,cms-file,cms-payment,cms-gamesave,cms-platform   # 全部库模块单元测试（无需外部基础设施）
```

> 需要完整应用上下文的集成测试（`@SpringBootTest`）统一放在 `cms-app/src/test`，需要本地/CI 提供 MySQL、Redis、MinIO。
> 库模块的测试是纯单元测试（Mockito），可以脱离外部依赖单独运行。
