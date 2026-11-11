package com.webworks.detection_service.rules;

import com.webworks.tradecommon.OrderEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OtrStateTest {

    private OrderEvent order(OrderEvent.Action action) {
        return new OrderEvent("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.OrderType.LIMIT,
                new BigDecimal("100.00"), 100, action, Instant.parse("2026-10-27T14:00:05Z"));
    }

    @Test
    void staysQuietBelowTheMinimumNumberOfCancels() {
        var state = new OtrState();
        for (int i = 0; i < 19; i++) state.apply(order(OrderEvent.Action.CANCEL), 20, 10.0);
        assertThat(state.emit).isFalse();
    }

    @Test
    void firesExactlyOnceWhenThresholdIsCrossed() {
        var state = new OtrState();
        int fired = 0;
        for (int i = 0; i < 30; i++) {
            if (state.apply(order(OrderEvent.Action.CANCEL), 20, 10.0).emit) fired++;
        }
        assertThat(fired).isEqualTo(1);
    }

    @Test
    void staysQuietWhenFillsKeepTheRatioLow() {
        var state = new OtrState();
        for (int i = 0; i < 3; i++) state.apply(order(OrderEvent.Action.FILL), 20, 10.0);
        for (int i = 0; i < 25; i++) state.apply(order(OrderEvent.Action.CANCEL), 20, 10.0);   // 25 / 3 = 8.3
        assertThat(state.alerted).isFalse();
    }
}