// 文件说明：FollowController 控制器，负责处理 Follow 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IBlogImageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/follow
@RequestMapping("/follow")
// 控制器类：负责接收请求、调用业务层并返回结果
public class FollowController {
    // 注入 followService（IFollowService）
    @Resource
    private IBlogImageService followService;
    // 关注或取关用户
    @PutMapping("/{id}/{shouldFollow}")
    public Result follow(@PathVariable("id") Long followUserId,@PathVariable("shouldFollow") Boolean shouldFollow){
        return followService.follow(followUserId,shouldFollow);
    }

    // 判断是否已关注用户
    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }

    // 查询共同关注
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable Long id){
        return followService.followCommons(id);
    }
}
