package com.thx.module.payment.domain;

import com.thx.module.payment.exception.IllegalPaymentStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link PaymentOrder} 状态转换的唯一权威来源，业务代码禁止绕过它直接
 * {@code order.setStatus(...)}。核心语义约束：
 * <ul>
 *     <li>{@code from == to} 恒定合法，视为对同一事实的重复确认（幂等 no-op）；</li>
 *     <li>{@link PaymentStatus#FAILED} -&gt; {@link PaymentStatus#SUCCESS} 和
 *         {@link PaymentStatus#CLOSED} -&gt; {@link PaymentStatus#SUCCESS} 是刻意保留的修正路径——
 *         支付渠道的成功事实优先于本地任何历史判断（本地误判失败/关闭时的竞态，不能拒绝渠道真实成功）；</li>
 *     <li>{@link PaymentStatus#SUCCESS} 及之后的状态不允许再退回 {@code FAILED}/{@code CLOSED}。</li>
 * </ul>
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private static Map<PaymentStatus, Set<PaymentStatus>> buildTransitions() {
        Map<PaymentStatus, Set<PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);
        map.put(PaymentStatus.CREATED, EnumSet.of(
                PaymentStatus.PROCESSING, PaymentStatus.FAILED, PaymentStatus.UNKNOWN, PaymentStatus.CLOSED));
        map.put(PaymentStatus.PROCESSING, EnumSet.of(
                PaymentStatus.UNKNOWN, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.CLOSED));
        map.put(PaymentStatus.UNKNOWN, EnumSet.of(
                PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.CLOSED));
        map.put(PaymentStatus.FAILED, EnumSet.of(PaymentStatus.SUCCESS));
        map.put(PaymentStatus.CLOSED, EnumSet.of(PaymentStatus.SUCCESS));
        map.put(PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED));
        map.put(PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(PaymentStatus.REFUNDED));
        map.put(PaymentStatus.REFUNDED, Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 判断 from -&gt; to 是否是合法转换（不修改任何状态，纯规则查询）。
     */
    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        if (from == to) {
            return true;
        }
        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 校验 from -&gt; to 合法，非法则抛 {@link IllegalPaymentStateTransitionException}。
     * 返回值仅用于函数式调用链，恒等于 {@code to}。
     */
    public PaymentStatus transition(PaymentStatus from, PaymentStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalPaymentStateTransitionException(String.valueOf(from), String.valueOf(to));
        }
        return to;
    }

    /**
     * 是否是不可逆的"高阶"终局状态（SUCCESS 及之后）。
     * {@code PaymentChannelResultProcessor} 用它判断收到的迟到/不适用渠道回执是否应静默忽略而不是报错。
     */
    public boolean isTerminalHighOrder(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.REFUNDED;
    }
}
