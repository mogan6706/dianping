// 文件说明：ThreadLocal 用户上下文工具类，用来在一次请求处理过程中随时获取当前登录用户。

package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;

public class UserHolder {
    // ThreadLocal：在一次请求处理中临时保存当前登录用户
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    // 保存当前登录用户
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    // 获取当前登录用户
    public static UserDTO getUser(){
        return tl.get();
    }

    // 移除当前登录用户
    public static void removeUser(){
        tl.remove();
    }
}
