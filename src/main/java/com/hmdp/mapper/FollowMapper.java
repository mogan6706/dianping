// 文件说明：FollowMapper Mapper 接口，负责把 Java 方法映射到数据库查询或更新操作。

package com.hmdp.mapper;

import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// Mapper 接口：定义数据库访问方法，具体 SQL 由 MyBatis 或 MyBatis-Plus 执行
public interface FollowMapper extends BaseMapper<Follow> {

}
