// 文件说明：Spring Boot 启动类，项目从这里开始加载配置、注册 Bean 并启动 Web 服务。

package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

// 功能开关：开启 AOP 代理功能
@EnableAspectJAutoProxy(exposeProxy = true)
// 扫描配置：扫描 mapper 包，让数据库接口自动生效
@MapperScan("com.hmdp.mapper")
// 功能开关：开启 RabbitMQ 监听功能
@EnableRabbit
// 启动类注解：告诉 Spring Boot 从这里开始启动整个项目
@SpringBootApplication
public class HmDianPingApplication {
    // main：项目启动入口。
    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

}
