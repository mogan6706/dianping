// 文件说明：SeckillVoucher 实体类，用来映射数据库中的一类业务数据记录。

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
 * 秒杀优惠券表，与优惠券是一对一关系
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
// 表映射：指定实体类对应的数据库表
@TableName("tb_seckill_voucher")
public class SeckillVoucher implements Serializable {

        // 序列化版本号

    private static final long serialVersionUID = 1L;

    /**
     * 关联的优惠券的id
     */
    // 主键映射：指定主键字段和生成策略
    @TableId(value = "voucher_id", type = IdType.INPUT)
    private Long voucherId;

    // 库存
    private Integer stock;

    // 创建时间
    private LocalDateTime createTime;

    // 生效时间
    private LocalDateTime beginTime;

    // 失效时间
    private LocalDateTime endTime;

    // 更新时间
    private LocalDateTime updateTime;


}
