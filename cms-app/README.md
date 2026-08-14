# cms-app 启动模块

唯一产出**可执行 Jar** 的模块（`cms-app/target/cms.jar`），负责把各库模块装配成可运行的 Spring Boot 应用。
Dockerfile / deploy.sh / Drone 都按 `cms.jar` 这个名字工作，改名会破坏部署。

## 包含的内容

- `com.thx.SpringbootApplication` 启动类（`@SpringBootApplication` + `@EnableScheduling`）
- `com.thx.common.config`：**统一装配配置**——`WebMvcConfig`（静态资源/拦截器/跨域，组合各模块拦截器）、
  `MybatisPlusConfig`（聚合扫描全部 mapper 包）、`RedisConfig`、`WebSocketConfig`、`ErrorPageConfig`
- `src/main/resources`：application*.yml、logback-spring.xml、mapper XML、Flyway `db/migration`、
  static（admin-app/blog-app 两个 Vue SPA）、templates（Thymeleaf 页面）

## 依赖

全部 5 个库模块 + spring-boot-starter-web/websocket/thymeleaf/data-redis/actuator、
mysql-connector-j、flyway-core/mysql、druid-spring-boot-3-starter、configuration-processor、test starter。

## 约束

- **本模块只做装配，不放业务代码**：新的 Controller/Service/Mapper 必须放进对应库模块。
- 只有本模块的 POM 允许 `spring-boot-maven-plugin` repackage（库模块已配置 `<skip>true</skip>`）。
- `@MapperScan` 已在 `MybatisPlusConfig` 聚合声明，新 mapper 包要登记进去。

## 测试

`src/test/java/com/thx`：
- `SpringbootApplicationTests`（contextLoads）
- `schema.SchemaInitScriptTest`（守护 docs/modules 下的 4 份初始化 SQL 不漂移）
- 需要完整应用上下文的集成测试：登录/密码升级（`common.security`）、file 字段映射（`module.file.service`）、
  GameSave 全链路（`module.gamesave.integration`，`-Dgamesave.integration=true` 开启）

这些测试需要本地/CI 提供 MySQL（+ Redis/MinIO，gamesave 场景）。库模块的单元测试可脱离外部依赖单独跑：

```bash
mvn test -pl cms-kernel,cms-file,cms-payment,cms-gamesave,cms-platform
```
