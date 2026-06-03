// 文件说明：商铺本地缓存刷新消息，携带版本号防止 MQ 乱序覆盖新缓存。

package com.hmdp.dto;

import com.hmdp.entity.Shop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopCacheRefreshMessage {

    private Shop shop;

    private Long version;
}
