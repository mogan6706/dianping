// 文件说明：全局异常处理类，把运行时异常统一转换成前端更容易处理的响应。

package com.hmdp.config;

import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestControllerAdvice
public class WebExceptionAdvice {
    // 统一处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        // 后端记录完整异常栈，前端只拿到统一错误文案，避免暴露内部实现细节。
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }
}
