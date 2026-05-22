// 文件说明：Shop 实体类，用来映射数据库中的一类业务数据记录。

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
@TableName("tb_shop")
public class Shop implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 商铺名称
    private String name;

    // 商铺类型的id
    private Long typeId;

    // 商铺图片，多个图片以','隔开
    private String images;

    // 商圈，例如陆家嘴
    private String area;

    // 地址
    private String address;

    // 经度
    private Double x;

    // 维度
    private Double y;

    // 均价，取整数
    private Long avgPrice;

    // 销量
    private Integer sold;

    // 评论数量
    private Integer comments;

    // 评分，1~5分，乘10保存，避免小数
    private Integer score;

    // 营业时间，例如 10:00-22:00
    private String openHours;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;


    @TableField(exist = false)
    // 非数据库字段：当前商铺距离用户的位置距离
    private Double distance;
}
