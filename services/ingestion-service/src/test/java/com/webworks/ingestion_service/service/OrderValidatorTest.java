package com.webworks.ingestion_service.service.service;

import com.webworks.ingestion_service.service.OrderValidator;
import com.webworks.tradecommon.OrderEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderValidatorTest {

    private static final Instant TS = Instant.parse("2026-11-14T14:00:00Z");

    private static OrderEvent order(String orderId, String traderId, String symbol, OrderEvent.Side side,
                                    OrderEvent.Action action, Instant ts, long qty, BigDecimal price) {
        return new OrderEvent(orderId, traderId, symbol, side, OrderEvent.OrderType.LIMIT, price, qty, action, ts);
    }

    private static OrderEvent valid() {
        return order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.NEW, TS, 100, new BigDecimal("142.30"));
    }

    @Test
    void validOrderHasNoErrors() {
        assertThat(OrderValidator.validate(valid())).isEmpty();
        assertThat(OrderValidator.isValid(valid())).isTrue();
    }

    @Test
    void nullEventIsRejected() {
        assertThat(OrderValidator.validate(null)).containsExactly("event is missing");
        assertThat(OrderValidator.isValid(null)).isFalse();
    }

    @Test
    void missingFieldsAreAllReported() {
        var bad = order(" ", null, "", null, null, null, 100, new BigDecimal("1.00"));
        assertThat(OrderValidator.validate(bad)).contains(
                "orderId is required", "traderId is required", "symbol is required",
                "side is required", "action is required", "ts is required");
    }

    @Test
    void zeroOrNegativeQuantityIsRejected() {
        var zero = order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.NEW, TS, 0, new BigDecimal("1.00"));
        var negative = order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.NEW, TS, -5, new BigDecimal("1.00"));
        assertThat(OrderValidator.validate(zero)).contains("qty must be positive");
        assertThat(OrderValidator.validate(negative)).contains("qty must be positive");
    }

    @Test
    void newLimitOrderNeedsAPositivePrice() {
        var noPrice = order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.NEW, TS, 100, null);
        var zeroPrice = order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.NEW, TS, 100, BigDecimal.ZERO);
        assertThat(OrderValidator.validate(noPrice)).contains("price must be positive for a LIMIT order");
        assertThat(OrderValidator.validate(zeroPrice)).contains("price must be positive for a LIMIT order");
    }

    @Test
    void cancelDoesNotNeedAPrice() {
        var cancel = order("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.Action.CANCEL, TS, 100, null);
        assertThat(OrderValidator.validate(cancel)).isEmpty();
    }
}