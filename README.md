# 内容管理系统

### 内容管理系统，后端采用 Spring Boot + Apache Shiro + MyBatis-Plus，前台博客和后台管理均为 Vue 3 + Element Plus 单页应用(附带权限管理)，是搭建博客、网站的不二之选。

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

后端：Spring Boot、Apache Shiro、MyBatis-Plus、Alibaba Druid、Redis、MySQL、MinIO、Apache Tika<br/>
前端：Vue 3、Element Plus（前台博客 `static/blog-app/`、后台管理 `static/admin-app/` 均为纯静态文件，无需构建工具，改完即生效）<br/>

![JDK](https://img.shields.io/badge/JDK-1.8-green.svg)
![Maven](https://img.shields.io/badge/Maven-3.3.9-green.svg)
![MySQL](https://img.shields.io/badge/MySQL-5.7-green.svg)
![Redis](https://img.shields.io/badge/Redis-3.0.503-green.svg)
![license](https://img.shields.io/badge/license-MIT-yellow.svg)

## 安装

1. 将本项目源码导入本地开发工具(如 IntelliJ IDEA )，本地开发工具需要安装 [lombok](https://projectlombok.org/) 插件
2. 安装`MySQL`数据库：版本最低支持 5.7，新建 database `CREATE DATABASE cms;`
3. 初始化数据库：找到项目数据库文件 `docs/db/cms.sql`（文件系统相关表另见 `docs/db/file_system.sql`），执行导入
4. 安装`Redis`：最低版本支持 3.2
5. 安装`MinIO`（文件上传/存储依赖）：本地开发默认指向 `http://localhost:9000`，也可以通过环境变量 `MINIO_ENDPOINT`/`MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` 指向已有实例
6. 修改(`resources/application-dev.yml`)配置文件
    1. 修改数据库连接串、用户名和密码(可搜索`datasource`)
    2. redis 配置(可搜索`redis`)
7. 运行项目(三种方式)
    1. 项目根目录下执行`mvn -X clean package -Dmaven.test.skip=true`编译打包，然后执行`java -jar target/cms.jar`
    2. 项目根目录下执行`mvn spring-boot:run`
    3. 直接运行`SpringbootApplication.java`
8. 前台首页，浏览器访问`http://localhost:8080`
9. 后台首页，浏览器访问`http://localhost:8080/admin`使用账号密码admin,123456登录系统后台。


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

后台管理页面早已从传统的 Thymeleaf 多页面重构为 Vue 3 单页应用，下面是当前实际的目录结构（并非历史文档描述的老架构）：

```
├── main
│   ├── java
│   │   └── com
│   │       └── thx
│   │           ├── SpringbootApplication.java 项目启动类
│   │           ├── common     基础设施层：注解、拦截器、日志、Shiro 集成、通用工具类等，
│   │           │              被各业务 module 依赖，不反向依赖任何 module
│   │           ├── infra      项目级基础设施组件（邮件发送、WebSocket 推送、匿名路径扫描等）
│   │           ├── enums      全局枚举（响应状态码、站点配置 key 等）
│   │           ├── exception  全局异常处理
│   │           └── module
│   │               ├── admin  后台管理业务：controller 按业务域分了 article/auth/site/file/system
│   │               │          五个子包，entity/mapper/service 对应内容管理与 RBAC 权限模型
│   │               ├── blog   前台博客 API，复用 admin 模块的数据层，自己不维护 entity/mapper
│   │               ├── file   独立的文件系统子模块：基于 MinIO 对象存储 + Apache Tika 文件类型
│   │               │          嗅探，有自己的响应体/异常处理，是有意保持松耦合的模块边界
│   │               ├── tools  独立小工具（PDF 转 Word、OCR 识别等），部分接口对接外部 Python 服务
│   │               └── agent  供外部 AI Agent/自动化客户端调用的 API 网关层（/agent/api/**），
│   │                          走独立的 X-API-Key 鉴权，不走 Shiro 会话
│   └── resources
│       ├── application-dev.yml 开发环境配置文件
│       ├── application-prd.yml 生产环境配置文件
│       ├── application.yml     通用配置文件
│       ├── logback-spring.xml  日志配置文件
│       ├── mapper              MyBatis XML 文件
│       ├── static
│       │   ├── admin-app       后台管理 Vue 3 + Element Plus 单页应用（无构建步骤，纯静态文件）
│       │   ├── blog-app        前台博客 Vue 3 单页应用
│       │   ├── css / img / js / libs  前后台共用的静态资源、第三方类库
│       │   └── theme           前台主题相关资源
│       └── templates           仍由 Thymeleaf 服务端渲染的少数页面
│           ├── error           403/404/500 等错误页
│           ├── home/fragments  前台公共导航片段
│           └── system          登录/注册/踢出页面
└── test
    └── java
        └── com
            └── thx
                └── ...          单元测试，与 main 下的包结构一一对应
```
