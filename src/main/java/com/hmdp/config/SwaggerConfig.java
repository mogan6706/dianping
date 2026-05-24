// 文件说明：Knife4j/Swagger 配置类，生成 Controller 接口文档和调试页面。

package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

// 配置类：Spring 启动时会加载 Swagger 配置
@Configuration
public class SwaggerConfig {

    // Knife4j 文档入口：http://localhost:8081/doc.html
    @Bean
    public Docket hmdpApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.hmdp.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("HM DianPing API")
                .description("黑马点评项目接口文档")
                .version("1.0")
                .build();
    }
}
