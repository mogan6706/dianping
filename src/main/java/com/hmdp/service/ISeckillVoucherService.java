// 文件说明：ISeckillVoucherService 业务接口，定义 Seckill Voucher Service 模块对外提供的能力。

package com.hmdp.service;

import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
// 业务接口：先定义当前模块要提供哪些能力
public interface ISeckillVoucherService extends IService<SeckillVoucher> {

}
