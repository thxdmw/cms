package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelNotifyCommand {
    private Long channelAccountId;
    /** 原始表单参数（未 URL 编码），如支付宝异步通知的全部字段 */
    private Map<String, String> params;
}
