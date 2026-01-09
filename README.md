# DDD批量更新系统

这是一个基于Spring Boot 3.x和DDD（领域驱动设计）架构的批量更新系统，用于处理金融交易数据的批量更新操作。

## 项目结构

```
ddd-batch-update/
├── src/main/java/com/example/ddd/batchupdate/
│   ├── domain/                           # DDD领域层
│   │   ├── model/                        # 聚合根、实体、值对象
│   │   │   ├── entity/
│   │   │   │   ├── FinancialTransaction.java    # 金融交易聚合根
│   │   │   │   └── TransactionRecord.java        # 交易记录实体
│   │   │   └── valueobject/
│   │   │       ├── StreamId.java              # 流ID值对象
│   │   │       ├── TransactionStatus.java     # 交易状态枚举
│   │   │       ├── Timestamps.java            # 时间戳值对象
│   │   │       └── Version.java               # 版本号值对象
│   │   ├── service/                      # 领域服务
│   │   │   ├── TransactionDomainService.java      # 领域服务接口
│   │   │   └── TransactionDomainServiceImpl.java  # 领域服务实现
│   │   └── event/                        # 领域事件
│   │       ├── DomainEvent.java               # 领域事件基类
│   │       └── TransactionUpdatedEvent.java    # 交易更新事件
│   ├── application/                      # 应用服务层
│   │   └── TransactionApplicationService.java   # 交易应用服务
│   ├── infrastructure/                  # 基础设施层
│   │   ├── repository/                  # 数据访问层
│   │   │   ├── TransactionRepository.java          # 仓储接口
│   │   │   └── InMemoryTransactionRepository.java  # 内存仓储实现
│   │   └── messaging/                   # 消息基础设施
│   │       ├── DomainEventPublisher.java        # 事件发布器接口
│   │       ├── DomainEventHandler.java          # 事件处理器接口
│   │       └── InMemoryDomainEventInfrastructure.java # 内存事件基础设施
│   ├── interfaces/                      # 接口层
│   │   └── controller/
│   │       ├── TransactionController.java  # REST控制器
│   │       └── ApiResponse.java           # 统一API响应
│   ├── config/
│   │   └── DddBatchUpdateConfig.java     # 配置类
│   └── DddBatchUpdateApplication.java    # 主应用程序
├── src/main/resources/
│   └── application.yml                   # 应用程序配置
└── pom.xml                               # Maven配置
```

## 核心组件说明

### 1. 领域模型 (Domain Model)

#### FinancialTransaction聚合根
- **作用**: 交易聚合的边界，包含所有交易相关的业务规则
- **字段**: id, stream_id, status, update_time, update_by, create_time, create_by, version
- **功能**: 
  - 批量更新交易记录
  - 状态转换控制
  - 业务规则验证
  - 版本控制（乐观锁）

#### TransactionRecord实体
- **作用**: 表示单个交易记录
- **功能**:
  - 数据更新
  - 状态管理
  - 操作审计

#### 领域值对象
- **StreamId**: 流标识符
- **TransactionStatus**: 交易状态枚举，包含状态转换规则
- **Timestamps**: 创建和更新时间
- **Version**: 版本号用于乐观锁

### 2. 领域服务 (Domain Service)

#### TransactionDomainService
提供跨聚合的业务逻辑：
- 批量验证交易数据
- 批量更新交易状态
- 批量处理交易记录
- 交易合并处理
- 冲突解决策略

### 3. 领域事件 (Domain Events)

#### TransactionUpdatedEvent
- **类型**: 状态更新事件、数据更新事件、批量更新事件
- **用途**: 事件驱动架构，支持异步处理

### 4. 基础设施层 (Infrastructure)

#### 仓储模式
- **TransactionRepository**: 定义数据访问接口
- **InMemoryTransactionRepository**: 内存实现（开发/测试用）

#### 消息基础设施
- **DomainEventPublisher**: 事件发布
- **DomainEventHandler**: 事件处理
- **InMemoryDomainEventInfrastructure**: 内存事件总线

### 5. 应用服务层 (Application Service)

#### TransactionApplicationService
- 协调领域服务和基础设施
- 实现用例逻辑
- 事务管理
- 命令处理

### 6. 接口层 (Interfaces)

#### REST API
- 创建交易
- 批量更新状态
- 批量处理记录
- 查询交易

## 批量更新业务规则

### 1. 状态转换规则
- `PENDING` → `PROCESSING` | `CANCELLED`
- `PROCESSING` → `SUCCESS` | `FAILED` | `CANCELLED`
- `SUCCESS`, `FAILED`, `CANCELLED`, `ROLLBACK` 状态不可转换

### 2. 数据更新约束
- 只允许在 `PENDING` 和 `FAILED` 状态下更新数据
- 单次批量更新最多1000条记录
- 数据长度限制：10000字符

### 3. 乐观锁控制
- 使用版本号控制并发更新
- 版本号不匹配时更新失败
- 支持版本冲突检测和处理

### 4. 业务验证规则
- 流ID不能为空且必须唯一
- 操作人必须提供
- 状态转换必须合法
- 时间戳逻辑必须正确

## API接口

### 1. 创建交易
```
POST /api/v1/transactions
Content-Type: application/json

{
  "streamId": "stream-001",
  "recordData": [
    "{\"amount\": 100.0, \"currency\": \"USD\"}",
    "{\"amount\": 200.0, \"currency\": \"EUR\"}"
  ],
  "operator": "user001"
}
```

### 2. 批量更新状态
```
PUT /api/v1/transactions/batch-status
Content-Type: application/json

{
  "transactionIds": ["tx-001", "tx-002"],
  "newStatus": "PROCESSING",
  "operator": "user001"
}
```

### 3. 批量处理记录
```
PUT /api/v1/transactions/batch-records
Content-Type: application/json

{
  "streamId": "stream-001",
  "updates": {
    "record-001": "{\"amount\": 150.0, \"currency\": \"USD\"}",
    "record-002": "{\"amount\": 250.0, \"currency\": \"EUR\"}"
  },
  "operator": "user001"
}
```

### 4. 查询交易
```
GET /api/v1/transactions/{transactionId}
```

## 运行说明

### 1. 构建项目
```bash
mvn clean compile
```

### 2. 运行测试
```bash
mvn test
```

### 3. 启动应用
```bash
mvn spring-boot:run
```

### 4. 访问接口
- API文档: http://localhost:8080/swagger-ui.html
- H2控制台: http://localhost:8080/h2-console
- 健康检查: http://localhost:8080/actuator/health

## 依赖

- **Spring Boot 3.2.0**
- **Java 8+**
- **H2 Database** (内存数据库)
- **Spring Data JPA**
- **Spring Web**
- **Lombok**
- **SLF4J/Logback**

## 设计原则

1. **DDD架构**: 严格遵循领域驱动设计原则
2. **聚合边界**: FinancialTransaction作为聚合根，管理相关实体
3. **领域事件**: 支持事件驱动架构
4. **仓储模式**: 分离领域与数据访问
5. **应用服务**: 协调业务逻辑和基础设施
6. **依赖倒置**: 高层模块不依赖低层模块

## 扩展性

- 可替换内存实现为数据库实现
- 支持多种消息队列（RabbitMQ, Kafka等）
- 可扩展更多业务领域
- 支持微服务架构拆分

## 注意事项

1. 当前使用内存存储，生产环境需要替换为数据库
2. 乐观锁需要数据库层面的支持
3. 批量操作需要考虑内存和性能
4. 领域事件可以集成消息队列进行异步处理