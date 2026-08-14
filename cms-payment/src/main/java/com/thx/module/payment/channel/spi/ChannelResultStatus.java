package com.thx.module.payment.channel.spi;

/**
 * 归一化的渠道结果状态，供 {@code PaymentChannelResultProcessor} 统一消费，
 * 与具体渠道（如支付宝 trade_status）的字面值解耦。
 */
public enum ChannelResultStatus {

    /** 处理中，尚无结果 */
    PROCESSING,

    /** 渠道调用异常/超时，结果未知 */
    UNKNOWN,

    /** 明确成功 */
    SUCCESS,

    /** 明确业务失败 */
    FAILED,

    /** 已关闭 */
    CLOSED
}
