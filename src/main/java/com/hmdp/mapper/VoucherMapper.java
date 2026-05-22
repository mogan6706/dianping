// 文件说明：VoucherMapper Mapper 接口，负责把 Java 方法映射到数据库查询或更新操作。

package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// Mapper 接口：定义数据库访问方法，具体 SQL 由 MyBatis 或 MyBatis-Plus 执行
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
