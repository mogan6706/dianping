// 文件说明：BlogCommentsController 控制器，负责处理 Blog Comments 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.service.IBlogCommentsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/blog-comments
@RequestMapping("/blog-comments")
// 控制器类：负责接收请求、调用业务层并返回结果
public class BlogCommentsController {

    // 注入 blogCommentsService（IBlogCommentsService）
    @Resource
    private IBlogCommentsService blogCommentsService;

    // 分页查询某篇博客下的一级评论
    @GetMapping("/of/blog/{blogId}")
    public Result queryCommentsByBlogId(
            @PathVariable("blogId") Long blogId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogCommentsService.queryCommentsByBlogId(blogId, current);
    }

    // 分页查询某条评论下的回复
    @GetMapping("/replies/{parentId}")
    public Result queryRepliesByParentId(
            @PathVariable("parentId") Long parentId,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogCommentsService.queryRepliesByParentId(parentId, current);
    }

    // 新增评论或回复
    @PostMapping
    public Result saveComment(@RequestBody BlogComments comment) {
        return blogCommentsService.saveComment(comment);
    }

    // 删除当前用户自己的评论
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id) {
        return blogCommentsService.deleteComment(id);
    }
}
