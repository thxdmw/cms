package com.thx.module.payment.controller;

import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.payment.api.PaymentFacade;
import com.thx.module.payment.api.command.CreateRefundCommand;
import com.thx.module.payment.api.command.QueryRefundCommand;
import com.thx.module.payment.api.result.RefundResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款 REST API，定位与 {@link PaymentController} 相同，见其类注释。
 */
@RestController
@RequestMapping("/api/payment/v1")
@RequiredArgsConstructor
public class RefundController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/refunds")
    public ResponseVo<RefundResult> createRefund(@RequestBody CreateRefundCommand command) {
        return ResponseVo.success(paymentFacade.refund(command));
    }

    @GetMapping("/refunds/{refundNo}")
    public ResponseVo<RefundResult> getRefund(@PathVariable String refundNo) {
        QueryRefundCommand command = QueryRefundCommand.builder().refundNo(refundNo).build();
        return ResponseVo.success(paymentFacade.queryRefund(command));
    }
}
