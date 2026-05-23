// 文件说明：IFollowService 业务接口，定义 Follow Service 模块对外提供的能力。

package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// 业务接口：先定义当前模块要提供哪些能力
public interface IBlogImageService extends IService<Follow> {

    Result follow(Long followUserId, Boolean shouldFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
