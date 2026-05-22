// 文件说明：IShopTypeService 业务接口，定义 Shop Type Service 模块对外提供的能力。

package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// 业务接口：先定义当前模块要提供哪些能力
public interface IShopTypeService extends IService<ShopType> {

    Result querySort();
}
