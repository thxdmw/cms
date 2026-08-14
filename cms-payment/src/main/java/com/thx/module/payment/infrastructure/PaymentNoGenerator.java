package com.thx.module.payment.infrastructure;

import cn.hutool.core.util.RandomUtil;
import com.thx.common.util.DateUtil;
import com.thx.common.util.UUIDUtil;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 支付单号 / 尝试号 / 退款单号 / 事件 ID 生成。复用项目已有的 {@link DateUtil} 时间戳格式
 * 和 {@link UUIDUtil}，不引入新的雪花算法组件。格式：前缀 + 17 位毫秒级时间戳(yyyyMMddHHmmssSSS) + 4 位随机数字，
 * 碰撞概率极低，落库处仍以数据库唯一约束兜底。
 */
@Component
public class PaymentNoGenerator {

    private static final int RANDOM_SUFFIX_LENGTH = 4;

    public String nextPaymentNo() {
        return "P" + timestamp() + randomSuffix();
    }

    public String nextAttemptNo() {
        return "A" + timestamp() + randomSuffix();
    }

    public String nextRefundNo() {
        return "R" + timestamp() + randomSuffix();
    }

    public String nextEventId() {
        return UUIDUtil.uuid();
    }

    private String timestamp() {
        return DateUtil.getConcurrentFormatDateString(new Date());
    }

    private String randomSuffix() {
        return RandomUtil.randomNumbers(RANDOM_SUFFIX_LENGTH);
    }
}
