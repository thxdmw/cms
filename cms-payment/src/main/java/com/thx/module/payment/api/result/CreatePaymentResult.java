package com.thx.module.payment.api.result;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建支付结果。payData 是客户端拉起支付所需的数据，如 App 支付的 {@code {"orderStr": "..."}}；
 * 当命中幂等分支返回既有的 SUCCESS 订单时，payData 为 null（不需要、也不应该再次拉起支付）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResult {
    private String paymentNo;
    private PaymentStatus status;
    private PaymentChannel channel;
    private PaymentScene scene;
    private Map<String, Object> payData;
}
