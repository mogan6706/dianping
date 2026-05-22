// 文件说明：Voucher 实体类，用来映射数据库中的一类业务数据记录。

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
@TableName("tb_voucher")
public class Voucher implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 商铺id
    private Long shopId;

    // 代金券标题
    private String title;

    // 副标题
    private String subTitle;

    // 使用规则
    private String rules;

    // 支付金额
    private Long payValue;

    // 抵扣金额
    private Long actualValue;

    // 优惠券类型
    private Integer type;

    // 优惠券类型
    private Integer status;
    // 库存
    @TableField(exist = false)
    private Integer stock;

    // 生效时间
    @TableField(exist = false)
    private LocalDateTime beginTime;

    // 失效时间
    @TableField(exist = false)
    private LocalDateTime endTime;

    // 创建时间
    private LocalDateTime createTime;


    // 更新时间
    private LocalDateTime updateTime;


}
