package com.thx.module.payment.controller;

import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.payment.api.PaymentFacade;
import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.command.QueryPaymentCommand;
import com.thx.module.payment.api.result.CreatePaymentResult;
import com.thx.module.payment.api.result.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付 REST API。当前项目内部业务模块统一直接注入 {@link PaymentFacade} 走 Java 方法调用，
 * 不会自己调这套 HTTP 接口（同一 JVM 内不应该自调用 HTTP）；这里存在的意义是为未来 Payment
 * 拆分为独立服务预留对外契约。鉴权沿用项目现有 Shiro 会话机制，服务间身份认证是拆分前必须
 * 补齐的事项，见 docs/payment-architecture.md。
 * <p>
 * Controller 只做参数接收与 DTO 转换，不直接操作 Mapper/AlipayClient，不直接修改订单状态。
 */
@RestController
@RequestMapping("/api/payment/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/payments")
    public ResponseVo<CreatePaymentResult> createPayment(@RequestBody CreatePaymentCommand command) {
        return ResponseVo.success(paymentFacade.createPayment(command));
    }

    @GetMapping("/payments/{paymentNo}")
    public ResponseVo<PaymentResult> getPayment(@PathVariable String paymentNo) {
        QueryPaymentCommand command = QueryPaymentCommand.builder().paymentNo(paymentNo).build();
        return ResponseVo.success(paymentFacade.queryPayment(command));
    }

    @PostMapping("/payments/{paymentNo}/sync")
    public ResponseVo<PaymentResult> syncPayment(@PathVariable String paymentNo) {
        QueryPaymentCommand command = QueryPaymentCommand.builder().paymentNo(paymentNo).build();
        return ResponseVo.success(paymentFacade.syncPayment(command));
    }
}
