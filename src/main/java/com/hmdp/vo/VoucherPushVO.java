// 文件说明：优惠券 WebSocket 推送消息对象，统一描述事件类型、店铺和消息数据。

package com.hmdp.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherPushVO {
    // 事件类型，例如 voucher.created、voucher.list
    private String event;
    // 关联店铺 id
    private Long shopId;
    // 实际推送数据，可以是单个 VoucherVO，也可以是列表
    private Object data;
    // 推送时间
    private LocalDateTime pushTime;

    public static VoucherPushVO of(String event, Long shopId, Object data) {
        return new VoucherPushVO(event, shopId, data, LocalDateTime.now());
    }
}
