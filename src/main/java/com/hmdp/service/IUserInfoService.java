// 文件说明：IUserInfoService 业务接口，定义 User Info Service 模块对外提供的能力。

package com.hmdp.service;

import com.hmdp.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
// 业务接口：先定义当前模块要提供哪些能力
public interface IUserInfoService extends IService<UserInfo> {

}
