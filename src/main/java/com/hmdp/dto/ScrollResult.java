// 文件说明：ScrollResult 数据传输对象，用于接口请求或响应时在前后端之间传递数据。

package com.hmdp.dto;

import lombok.Data;

import java.util.List;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
public class ScrollResult {
    // 本次查询到的数据列表
    private List<?> list;
    // 本页数据里最小的时间戳
    private Long minTime;
    // 下一次滚动查询要跳过的条数
    private Integer offset;
}
