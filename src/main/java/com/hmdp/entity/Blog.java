// 文件说明：Blog 实体类，用来映射数据库中的一类业务数据记录。

package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("tb_blog")
public class Blog implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // 商户id
    private Long shopId;
    // 用户id
    private Long userId;
    // 用户图标
    @TableField(exist = false)
    private String icon;
    // 用户姓名
    @TableField(exist = false)
    private String name;
    // 是否点赞过了
    @TableField(exist = false)
    private Boolean isLike;

    // 标题
    private String title;

    // 探店的照片，最多9张，多张以","隔开
    private String images;

    // 探店的文字描述
    private String content;

    // 点赞数量
    private Integer liked;

    // 评论数量
    private Integer comments;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;


}
