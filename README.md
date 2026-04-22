# 体育馆场地预约系统 - 测试项目

## 项目简介

基于 Spring Boot 2.2.2 + JPA + MySQL + Thymeleaf 的体育馆场地预约管理系统，包含完整的集成测试和单元测试。

## 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 21+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |

## 环境配置

### 1. 数据库配置

修改 `src/main/resources/application.yml` 中的数据库密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_db?useSSL=false&characterEncoding=utf8&zeroDateTimeBehavior=CONVERT_To_NULL&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为本地 MySQL root 密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. 数据库初始化

创建数据库（首次运行）：
```sql
CREATE DATABASE demo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

如需导入初始测试数据，执行项目根目录的 SQL 文件：
```bash
mysql -u root -p demo_db < demo_db.sql
```

## 运行测试

### 运行指定测试类

```powershell
# PowerShell（注意引号包裹）
mvn test "-Dtest=AdminOrderControllerTest,AdminUserControllerTest"
```

```cmd
# CMD
mvn test -Dtest=AdminOrderControllerTest,AdminUserControllerTest
```

### 运行所有测试

```bash
mvn test
```

### 运行单个测试类

```bash
mvn test -Dtest=AdminOrderControllerTest
mvn test -Dtest=AdminUserControllerTest
mvn test -Dtest=UserControllerTest
```

### 清理并重新测试

```bash
mvn clean test
```

## 项目结构

```
src/
├── main/java/com/demo/
│   ├── controller/       # Controller 层（集成测试目标）
│   │   ├── user/          # 用户端接口
│   │   └── admin/         # 管理端接口
│   └── service/impl/      # Service 层（单元测试目标）
└── test/
    ├── java/com/demo/controller/  # 集成测试类
    │   ├── user/
    │   │   └── UserControllerTest.java
    │   └── admin/
    │       ├── AdminOrderControllerTest.java
    │       ├── AdminUserControllerTest.java
    │       └── ...
    └── resources/sql/             # 测试数据 SQL 文件
        └── admin_order_test_data.sql
```

## 测试覆盖范围

### 已完成的集成测试

| 测试类 | 方法数 | 覆盖技术 |
|-------|-------|---------|
| UserControllerTest | 16 | 等价类、边界值、判定覆盖 |
| AdminOrderControllerTest | 12 | 判定覆盖 + 边界值 |
| AdminUserControllerTest | 11 | 判定覆盖 + 时序组合 |

### 测试技术说明

- **黑盒测试**：等价类划分（有效/无效/边界）、边界值分析
- **白盒测试**：判定覆盖（true/false分支）
- **集成测试**：多接口时序组合（增→查→改→删生命周期）

## 常见问题

### 1. Lombok 与 JDK 21 兼容性

**问题**：`java.lang.IllegalAccessError: class lombok.javac.apt.LombokProcessor`

**解决**：已在 `pom.xml` 中指定 Lombok 版本为 1.18.30，兼容 JDK 21：
```xml
<properties>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

### 2. PowerShell 执行测试命令

**问题**：`参数列表中缺少参量`

**解决**：PowerShell 中逗号需要引号包裹：
```powershell
mvn test "-Dtest=AdminOrderControllerTest,AdminUserControllerTest"
```

### 3. 测试数据依赖

测试使用 `@Sql` 注解自动导入测试数据，位于 `src/test/resources/sql/` 目录：
- `admin_order_test_data.sql` - 订单测试数据
- 内联 SQL - 用户测试数据

数据在 `@Transactional` 事务中自动回滚，无需手动清理。

## 技术栈

- **框架**：Spring Boot 2.2.2
- **模板引擎**：Thymeleaf
- **数据库**：MySQL 8.0 + JPA
- **测试框架**：JUnit 5 + SpringBootTest + MockMvc
- **构建工具**：Maven

## 待完成工作

- [ ] AdminVenueController 集成测试（8个方法，含文件上传）
- [ ] AdminMessageController 集成测试（4个方法）
- [ ] AdminNewsController 集成测试（7个方法）
- [ ] Service 层单元测试（5个 ServiceImpl 类）
