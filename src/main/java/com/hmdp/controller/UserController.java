// 文件说明：UserController 控制器，负责处理 User 相关的 HTTP 接口请求。

package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import io.reactivex.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/user
@RequestMapping("/user")
// 控制器类：负责接收请求、调用业务层并返回结果
public class UserController {

    // 注入 userService（IUserService）
    @Resource
    private IUserService userService;

    // 注入 userInfoService（IUserInfoService）
    @Resource
    private IUserInfoService userInfoService;

    // 注入 redisTemplate（StringRedisTemplate）
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 发送登录验证码
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    // 用户登录并返回 token
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        return userService.login(loginForm);
    }

    // 退出登录
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "authorization", required = false) String token){
        return userService.logout(token);
    }

    // 返回当前登录用户
    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    // 查询用户详情
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    // 根据 id 查询用户
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        //查询详情
        User user = userService.getById(userId);
        if(user==null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    // 记录当天签到
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    // 统计连续签到天数
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
