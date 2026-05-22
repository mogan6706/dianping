// 文件说明：LoginFormDTO 数据传输对象，用于接口请求或响应时在前后端之间传递数据。

package com.hmdp.dto;

import lombok.Builder;
import lombok.Data;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
@Builder
public class LoginFormDTO {
    // 手机号
    private String phone;
    // 短信验证码
    private String code;
    // 密码登录时使用的密码
    private String password;
}
