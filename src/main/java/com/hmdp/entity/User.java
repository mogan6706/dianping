// 文件说明：User 实体类，用来映射数据库中的一类业务数据记录。

package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
// 表映射：指定实体类对应的数据库表
@TableName("tb_user") 
public class User implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 手机号码
    private String phone;

    // 密码，加密存储
    private String password;

    // 昵称，默认是随机字符
    private String nickName;

    /**
     * 用户头像
     */
    private String icon = "";

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;


}
