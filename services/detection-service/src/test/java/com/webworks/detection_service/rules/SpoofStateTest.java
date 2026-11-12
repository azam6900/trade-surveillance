package com.webworks.detection_service.rules;

import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.OrderEvent.Action;
import com.webworks.tradecommon.OrderEvent.Side;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpoofStateTest {

    private static final Instant T0 = Instant.parse("2026-10-27T14:00:00.200Z");

    private OrderEvent ev(String id, Action action, Side side, long qty, long offsetMs) {
        return new OrderEvent(id, "T-77", "RY.TO", side, OrderEvent.OrderType.LIMIT,
                new BigDecimal("140.00"), qty, action, T0.plusMillis(offsetMs));
    }

    /** Five large BUYs, a small SELL that fills, then the five BUYs cancelled. */
    private List<OrderEvent> spoofingSequence(long layerQty, boolean withFill) {
        List<OrderEvent> events = new ArrayList<>();
        for (int i = 1; i <= 5; i++) events.add(ev("L-" + i, Action.NEW, Side.BUY, layerQty, 200L * i));
        events.add(ev("S-1", Action.NEW, Side.SELL, 500, 1500));
        if (withFill) events.add(ev("S-1", Action.FILL, Side.SELL, 500, 1700));
        for (int i = 1; i <= 5; i++) events.add(ev("L-" + i, Action.CANCEL, Side.BUY, layerQty, 3000 + 200L * i));
        return events;
    }

    private int countEmits(SpoofState state, List<OrderEvent> events) {
        int fired = 0;
        for (OrderEvent e : events) if (state.apply(e).emit) fired++;
        return fired;
    }

    @Test
    void fullSpoofingSequenceFiresOnce() {
        var state = new SpoofState();
        assertThat(countEmits(state, spoofingSequence(10_000, true))).isEqualTo(1);
        assertThat(state.layerSide).isEqualTo(Side.BUY);
        assertThat(state.evidence).hasSize(12);
    }

    @Test
    void cancelsWithoutAnOppositeFillDoNotFire() {
        assertThat(countEmits(new SpoofState(), spoofingSequence(10_000, false))).isZero();
    }

    @Test
    void smallOrdersAreIgnored() {
        assertThat(countEmits(new SpoofState(), spoofingSequence(500, true))).isZero();
    }

    @Test
    void firesOnlyOnceEvenIfMoreEventsFollow() {
        var state = new SpoofState();
        List<OrderEvent> events = spoofingSequence(10_000, true);
        events.add(ev("L-6", Action.NEW, Side.BUY, 10_000, 5000));
        events.add(ev("L-6", Action.CANCEL, Side.BUY, 10_000, 5200));
        assertThat(countEmits(state, events)).isEqualTo(1);
    }
}