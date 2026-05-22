// 文件说明：UserDTO 数据传输对象，用于接口请求或响应时在前后端之间传递数据。

package com.hmdp.dto;

import lombok.Data;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
public class UserDTO {
    // 用户 id
    private Long id;
    // 用户昵称
    private String nickName;
    // 用户头像
    private String icon;
}
