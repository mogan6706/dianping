// 文件说明：优惠券视图对象，专门用于返回给前端或 WebSocket 推送。

package com.hmdp.vo;

import com.hmdp.entity.Voucher;
import lombok.Data;

import java.time.LocalDateTime;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
public class VoucherVO {
    private Long id;
    private Long shopId;
    private String title;
    private String subTitle;
    private String rules;
    private Long payValue;
    private Long actualValue;
    private Integer type;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;

    public static VoucherVO from(Voucher voucher) {
        if (voucher == null) {
            return null;
        }
        VoucherVO vo = new VoucherVO();
        vo.setId(voucher.getId());
        vo.setShopId(voucher.getShopId());
        vo.setTitle(voucher.getTitle());
        vo.setSubTitle(voucher.getSubTitle());
        vo.setRules(voucher.getRules());
        vo.setPayValue(voucher.getPayValue());
        vo.setActualValue(voucher.getActualValue());
        vo.setType(voucher.getType());
        vo.setStock(voucher.getStock());
        vo.setBeginTime(voucher.getBeginTime());
        vo.setEndTime(voucher.getEndTime());
        return vo;
    }
}
