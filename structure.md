<!-- 文件说明：项目结构速览文档，用来帮助初学者快速理解仓库目录和主要模块分工。 -->

# 项目结构概览

这是一个基于 Spring Boot 的后端项目，主要实现类似点评类的业务功能，重点使用 Redis、MyBatis-Plus、Lombok 和分布式缓存/秒杀等技术。

## 根目录

- `pom.xml`：Maven 构建文件，声明 Spring Boot、MyBatis-Plus、Redis、Redisson、Lettuce、RabbitMQ 等依赖。
- `README.md`：项目简介，介绍该项目的功能和技术亮点。
- `structure.md`：本文件，用于快速理解项目结构。

## 主要目录

### `src/main/java/com/hmdp`

- `HmDianPingApplication.java`：Spring Boot 启动类。

#### `config`
- `MvcConfig.java`：拦截器等 Web 配置。
- `MybatisConfig.java`：MyBatis-Plus 配置。
- `QueueConfig.java`：消息队列/RabbitMQ 配置。
- `RedissonConfig.java`：Redisson 和 Redis 相关配置。
- `WebExceptionAdvice.java`：全局异常处理。

#### `controller`
- 各类控制器，负责接收 HTTP 请求并调用服务层：
  - `BlogController`, `ShopController`, `UserController`, `VoucherController` 等。
  - `UploadController`、`VoucherOrderController`、`FollowController`、`ShopTypeController`、`BlogCommentsController`。

#### `dto`
- 数据传输对象，用于前后端交互：
  - `LoginFormDTO`, `UserDTO`, `Result`, `ScrollResult`。

#### `entity`
- 对应数据库表的实体类：
  - `User`, `Shop`, `Voucher`, `VoucherOrder`, `Blog`, `BlogComments`, `ShopType`, `UserInfo`, `Follow`, `SeckillVoucher`。

#### `interceptor`
- `LoginInterceptor.java`：登录校验拦截器。
- `RefreshTokenInterceptor.java`：刷新 token 或用户状态的拦截器。

#### `listener`
- `SeckillVoucherListener.java`：秒杀相关异步消息监听器。

#### `mapper`
- MyBatis-Plus 映射接口，负责数据访问：
  - `UserMapper`, `ShopMapper`, `VoucherMapper`, `VoucherOrderMapper`, `BlogMapper`, `BlogCommentsMapper`, `ShopTypeMapper`, `UserInfoMapper`, `FollowMapper`, `SeckillVoucherMapper`。

#### `service`
- 业务层接口和实现，处理业务逻辑。
- 这里有 `service` 目录，实际实现可能在 `service/impl`，但根目录里也包含 `IBlogService`, `IFollowService` 等接口。

#### `utils`
- 工具类和公共方法，通常包括 Redis、RedisKey、日期、文件、异常等辅助逻辑。

### `src/main/resources`

- `application.yaml`：Spring Boot 配置文件，包含 Redis、数据库、消息队列等配置。
- `seckill.lua`, `unLock.lua`：Redis Lua 脚本，用于秒杀与分布式锁逻辑。
- `db/hmdp.sql`：数据库建表和初始数据脚本。
- `mapper/VoucherMapper.xml`：MyBatis XML 映射文件，定义 SQL 语句。

### `src/test/java/com/hmdp`

- 测试代码：
  - `HmDianPingApplicationTests.java`：Spring Boot 启动测试。
  - `ShopCacheTest.java`, `VoucherOrderControllerTest.java` 等。
- `tokens.txt`：测试用的 token 数据。

## 核心思想

- 后端采用 Spring Boot + MyBatis-Plus 构建。
- Redis 负责缓存、分布式锁、消息队列、秒杀、用户会话等。
- 通过拦截器实现登录校验与 Token 刷新。
- 采用 `Cache Aside` 模式优化缓存与数据库一致性。
- 使用 Redis Stream 或消息队列实现异步下单与秒杀消费。

## 典型学习路径

1. 先看项目怎么启动  
   `HmDianPingApplication.java`  
   `resources/application.yaml`  
   `../../compose.yaml`  
   `../../pom.xml`

2. 再看请求进入系统前经过什么  
   `config/MvcConfig.java`  
   `interceptor/RefreshTokenInterceptor.java`  
   `interceptor/LoginInterceptor.java`

3. 先选一条简单链路看  
   推荐先看 `/shop/{id}`  
   再看 `/user/login`

4. 顺着链路看请求怎么执行  
   `controller/ShopController.java` 或 `controller/UserController.java`  
   ↓  
   `service/IShopService.java`、`service/impl/ShopServiceImpl.java`  
   或 `service/IUserService.java`、`service/impl/UserServiceImpl.java`  
   ↓  
   `mapper/ShopMapper.java` 或 `mapper/UserMapper.java`  
   ↓  
   Redis / MySQL

5. 看链路时顺便认识常见对象  
   `entity/Shop.java`、`entity/User.java`  
   `dto/LoginFormDTO.java`、`dto/UserDTO.java`、`dto/Result.java`

6. 看懂一条链路后，再总结分层职责  
   `controller`：接收请求  
   `service`：业务逻辑  
   `mapper`：数据库访问  
   `entity`：数据库表对象  
   `dto`：前后端传输对象  
   `utils`：工具与公共逻辑

7. 再重点看核心机制  
   `service/impl/ShopServiceImpl.java`：缓存查询、缓存穿透、缓存击穿  
   `utils/CacheClient.java`：缓存工具封装  
   `service/impl/UserServiceImpl.java`：登录、token、Redis 会话

8. 最后看高并发部分  
   `service/impl/VoucherOrderServiceImpl.java`  
   `config/QueueConfig.java`  
   `listener/SeckillVoucherListener.java`  
   `resources/seckill.lua`  
   `resources/unLock.lua`  
   `config/RedissonConfig.java`
