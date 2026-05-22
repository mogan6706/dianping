// 文件说明：VoucherOrder 实体类，用来映射数据库中的一类业务数据记录。

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
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    // 下单的用户id
    private Long userId;

    // 购买的代金券id
    private Long voucherId;

    // 支付方式 1：余额支付；2：支付宝；3：微信
    private Integer payType;

    // 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
    private Integer status;

    // 下单时间
    private LocalDateTime createTime;

    // 支付时间
    private LocalDateTime payTime;

    // 核销时间
    private LocalDateTime useTime;

    // 退款时间
    private LocalDateTime refundTime;

    // 更新时间
    private LocalDateTime updateTime;


}
