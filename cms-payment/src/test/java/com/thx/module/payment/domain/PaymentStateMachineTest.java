package com.thx.module.payment.domain;

import com.thx.module.payment.exception.IllegalPaymentStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @ParameterizedTest
    @CsvSource({
            "CREATED,PROCESSING",
            "CREATED,FAILED",
            "CREATED,UNKNOWN",
            "CREATED,CLOSED",
            "PROCESSING,UNKNOWN",
            "PROCESSING,SUCCESS",
            "PROCESSING,FAILED",
            "PROCESSING,CLOSED",
            "UNKNOWN,SUCCESS",
            "UNKNOWN,FAILED",
            "UNKNOWN,CLOSED",
            "SUCCESS,PARTIALLY_REFUNDED",
            "SUCCESS,REFUNDED",
            "PARTIALLY_REFUNDED,REFUNDED",
    })
    void allowsDocumentedLegalTransitions(PaymentStatus from, PaymentStatus to) {
        assertTrue(stateMachine.canTransition(from, to));
        assertEquals(to, stateMachine.transition(from, to));
    }

    @Test
    void allowsFailedToSuccessAsCorrectionPath() {
        // 核心场景：本地误判/调用失败记为 FAILED，但支付宝真实成功属于高优先级渠道事实，必须允许修正
        assertTrue(stateMachine.canTransition(PaymentStatus.FAILED, PaymentStatus.SUCCESS));
    }

    @Test
    void allowsClosedToSuccessAsRaceSelfHealPath() {
        // 关闭指令与用户支付成功竞态时的自愈路径
        assertTrue(stateMachine.canTransition(PaymentStatus.CLOSED, PaymentStatus.SUCCESS));
    }

    @Test
    void sameStatusIsAlwaysIdempotentNoOp() {
        for (PaymentStatus status : PaymentStatus.values()) {
            assertTrue(stateMachine.canTransition(status, status), status + " -> " + status + " 应恒定允许");
        }
    }

    @Test
    void rejectsSuccessBackToFailed() {
        // 核心场景：不能把已经确认的 SUCCESS 改回 FAILED
        assertFalse(stateMachine.canTransition(PaymentStatus.SUCCESS, PaymentStatus.FAILED));
        assertThrows(IllegalPaymentStateTransitionException.class,
                () -> stateMachine.transition(PaymentStatus.SUCCESS, PaymentStatus.FAILED));
    }

    @Test
    void rejectsRefundedGoingBackToProcessing() {
        assertFalse(stateMachine.canTransition(PaymentStatus.REFUNDED, PaymentStatus.PROCESSING));
        assertThrows(IllegalPaymentStateTransitionException.class,
                () -> stateMachine.transition(PaymentStatus.REFUNDED, PaymentStatus.PROCESSING));
    }

    @Test
    void rejectsClosedGoingToProcessing() {
        assertFalse(stateMachine.canTransition(PaymentStatus.CLOSED, PaymentStatus.PROCESSING));
    }

    @Test
    void illegalTransitionExceptionCarriesFromAndTo() {
        IllegalPaymentStateTransitionException e = assertThrows(IllegalPaymentStateTransitionException.class,
                () -> stateMachine.transition(PaymentStatus.SUCCESS, PaymentStatus.CLOSED));
        assertEquals("SUCCESS", e.getFrom());
        assertEquals("CLOSED", e.getTo());
    }

    @Test
    void isTerminalHighOrderCoversSuccessAndBeyond() {
        assertTrue(stateMachine.isTerminalHighOrder(PaymentStatus.SUCCESS));
        assertTrue(stateMachine.isTerminalHighOrder(PaymentStatus.PARTIALLY_REFUNDED));
        assertTrue(stateMachine.isTerminalHighOrder(PaymentStatus.REFUNDED));
        assertFalse(stateMachine.isTerminalHighOrder(PaymentStatus.CREATED));
        assertFalse(stateMachine.isTerminalHighOrder(PaymentStatus.PROCESSING));
        assertFalse(stateMachine.isTerminalHighOrder(PaymentStatus.UNKNOWN));
        assertFalse(stateMachine.isTerminalHighOrder(PaymentStatus.FAILED));
        assertFalse(stateMachine.isTerminalHighOrder(PaymentStatus.CLOSED));
    }
}
