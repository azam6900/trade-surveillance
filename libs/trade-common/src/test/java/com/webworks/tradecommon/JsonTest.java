package com.webworks.tradecommon;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class JsonTest {

    @Test
    void orderEventRoundTrips() {
        var e = new OrderEvent("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.OrderType.LIMIT,
                new BigDecimal("142.30"), 100, OrderEvent.Action.NEW, Instant.parse("2026-10-27T14:03:11Z"));
        assertThat(Json.read(Json.write(e), OrderEvent.class)).isEqualTo(e);
    }

    @Test
    void alertKeyIsStableAndSixtyFourChars() {
        var t = Instant.parse("2026-10-27T14:00:00Z");
        assertThat(AlertKeys.of("SPOOFING", "T-1", "RY.TO", t))
                .isEqualTo(AlertKeys.of("SPOOFING", "T-1", "RY.TO", t))
                .hasSize(64);
    }
}