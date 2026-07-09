package com.thx.module.payment.exception;

import com.thx.module.admin.vo.base.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 支付模块统一异常处理，只作用于 {@code com.thx.module.payment.controller} 包，
 * 复用项目全局的 {@link ResponseVo}，但用具体 HTTP 状态码承载精确语义（参照 file 模块 FileExceptionHandler 的先例）。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.thx.module.payment.controller")
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ResponseVo<Void>> handlePaymentException(PaymentException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        if (httpStatus >= 500) {
            log.error("支付模块异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);
        } else {
            log.warn("支付请求被拒绝: errorCode={}, message={}", e.getErrorCode(), e.getMessage());
        }
        return ResponseEntity.status(httpStatus).body(ResponseVo.error(httpStatus, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseVo<Void>> handleUnknown(Exception e) {
        log.error("支付模块未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(ResponseVo.error(500, "服务器内部错误"));
    }
}
