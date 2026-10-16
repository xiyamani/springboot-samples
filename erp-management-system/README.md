# ERP 管理系统

基于 Java 21、Spring Boot、Spring MVC、Spring Security、MyBatis 和 Vue 的桌面浏览器业务系统，包含完整 Java 源码、前端资源、自动化测试及接口文档。

## 业务模块

- 采购、销售：建单、提交、审批、驳回、作废。
- 仓储执行：审批后入库、出库，联动库存、移动平均成本和流水。
- 经营报表：销售收入、成本、毛利、商品分类和客户统计，支持 CSV 导出。
- 基础资料：商品、供应商、客户。
- 系统协同：金蝶业务字段适配、WMS 回执、MOM 工单与报工、任务重试和幂等处理。
- 权限与记录：管理员、业务员、审批主管、仓库员，以及业务操作审计。

## 快速启动

开发环境需要 **JDK 21** 和 **Maven 3.9 或更新版本**。首次构建需要联网下载 Maven 依赖。

在此目录执行：

```powershell
mvn test
mvn package
java -Dfile.encoding=UTF-8 -jar target/erp-training-1.0.0.jar
```

浏览器访问 `http://127.0.0.1:8087/`。如果端口已占用，可指定其他端口：

```powershell
java -Dfile.encoding=UTF-8 -jar target/erp-training-1.0.0.jar --server.port=8088
```

IDEA 中打开本目录的 `pom.xml`，作为 Maven 项目导入，将 SDK 设为 JDK 21，再运行 `cn.training.erp.ErpApplication`。工作目录设为本目录，以便数据库保存在当前项目的 `data` 文件夹。

前端 Vue、图标和样式随项目保存，由同一 Java 服务提供，不需要单独安装 Node.js 或构建前端。

## 登录账号

| 角色 | 账号 | 密码 | 操作范围 |
| --- | --- | --- | --- |
| 管理员 | admin | admin123 | 全部业务与接口维护 |
| 业务员 | buyer | buyer123 | 资料维护、建单、提交审批、作废 |
| 审批主管 | reviewer | review123 | 审批通过、驳回、查看记录 |
| 仓库员 | keeper | keeper123 | 审批后的入库与出库 |

以上是课程项目的固定账号。演示岗位分工时按“业务员提交 → 审批主管审核 → 仓库员执行”操作。管理员也可完成完整流程。

## 数据和接口

默认使用内嵌 H2 文件数据库，首次启动自动创建表和课程业务数据，不需要安装 MySQL。关闭并重启服务后数据保留在 `data/erp.mv.db`。备份时先停止服务，再复制整个 `data` 文件夹。

MySQL 优化脚本 `数据库优化脚本-mysql.sql` 为独立技术资料，提供样本表、联合索引和执行计划对比，不在业务页面展示。可选 MySQL 配置见 `部署与开发.md`。

**金蝶、WMS、MOM 默认采用本地模拟接口。** 项目实现本机 HTTP/JSON、SOAP/XML、字段解析、业务处理、回执和重试流程；不代表已经连接金蝶官方企业账套。金蝶客户端保留真实 HTTP 请求的适配位置，正式接入仍需授权、接口权限和环境验证。

本仓库不包含个人数据库、备份、日志、Java 运行环境或访问密钥。接入其他服务时通过环境变量提供凭证。

## 源码结构

```text
pom.xml
src/
  main/java/cn/training/erp/     20 个 Java 后端源码文件
  main/resources/
    static/                    Vue、JavaScript、HTML、CSS 和本地图标
    application*.properties    配置与数据库连接
    *schema*.sql               数据库表结构
  test/java/cn/training/erp/    2 个 Java 测试类
distribution/windows/         Windows 免安装分发启动模板
```

核心代码包括 `ErpService`、`WorkflowService`、`ErpMapper`、`TrainingMapper`、`IntegrationService`、`KingdeeApiClient` 和 `SecurityConfig`。

## 说明文件

- `使用说明.md`：账号、功能和完整操作流程。
- `课堂操作步骤.md`：项目展示步骤。
- `接口说明.md`、`ERP接口集合.postman_collection.json`：接口和 Postman 调试。
- `需求与设计.md`：需求、模型和业务设计。
- `部署与开发.md`：构建、运行、可选 MySQL 与接口配置。
- `测试与验收.md`：测试范围和已知边界。

本项目面向课程实训和桌面浏览器，不包含手机端。
