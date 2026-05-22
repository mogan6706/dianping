// 文件说明：BlogCommentsController 控制器，负责处理 Blog Comments 相关的 HTTP 接口请求。

package com.hmdp.controller;


import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/blog-comments
@RequestMapping("/blog-comments")
// 控制器类：负责接收请求、调用业务层并返回结果
public class BlogCommentsController {

}
